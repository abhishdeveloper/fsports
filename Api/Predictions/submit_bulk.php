<?php

declare(strict_types=1);

namespace App\Api\Predictions;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use PDOException;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';
// Require Middleware
require_once __DIR__ . '/../Middleware/auth_middleware.php';

use function App\Api\Middleware\authenticateJWT;

// Only allow POST requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// 1. Authenticate Request
$userPayload = authenticateJWT();
$userId = $userPayload['sub'] ?? null;

if (!$userId || !is_numeric($userId)) {
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}
$userId = (int) $userId;

// Fetch JSON payload (expecting an array of objects)
$inputJSON = file_get_contents('php://input');
$predictions = json_decode($inputJSON, true);

if (!is_array($predictions) || empty($predictions)) {
    Response::json(400, ['error' => 'Invalid JSON payload. Expected a non-empty array of predictions.']);
}

// Secure logging setup
$requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/Api/Predictions/submit_bulk.php', PHP_URL_PATH);
$ipAddress = filter_var($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0', FILTER_VALIDATE_IP) ?: '0.0.0.0';

try {
    $pdo = Database::getConnection();

    // Begin Transaction for true atomic integrity
    $pdo->beginTransaction();

    $statusStmt = $pdo->prepare("
        SELECT m.match_status
        FROM questions q
        JOIN matches m ON q.match_id = m.id
        WHERE q.id = :question_id
        LIMIT 1
    ");

    $insertStmt = $pdo->prepare("
        INSERT INTO predictions (user_id, question_id, selected_option, status)
        VALUES (:user_id, :question_id, :selected_option, 'pending')
    ");

    foreach ($predictions as $prediction) {
        $questionIdStr = $prediction['question_id'] ?? null;
        $selectedOption = strtoupper(trim($prediction['selected_option'] ?? ''));

        // Input Validation
        if ($questionIdStr === null || !filter_var($questionIdStr, FILTER_VALIDATE_INT)) {
            $pdo->rollBack();
            Response::json(400, ['error' => 'A valid question_id integer is required for all items.']);
        }
        $questionId = (int) $questionIdStr;

        if (!in_array($selectedOption, ['A', 'B', 'C', 'D'], true)) {
            $pdo->rollBack();
            Response::json(400, ['error' => 'Invalid option selected. Must be A, B, C, or D.']);
        }

        // Validation 1: Check match status dynamically for each question
        $statusStmt->execute([':question_id' => $questionId]);
        $match = $statusStmt->fetch(PDO::FETCH_ASSOC);

        if (!$match) {
            $pdo->rollBack();
            Response::json(404, ['error' => "Question ID $questionId not found."]);
        }

        if ($match['match_status'] !== 'upcoming') {
            $pdo->rollBack();
            logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
            Response::json(403, ['error' => 'Betting closed for one or more matches in this submission.']);
        }

        // Validation 2: Insert the prediction
        try {
            $insertStmt->execute([
                ':user_id' => $userId,
                ':question_id' => $questionId,
                ':selected_option' => $selectedOption
            ]);
        } catch (PDOException $e) {
            // Check for MySQL duplicate entry error code (1062)
            if ($e->getCode() === '23000' && strpos($e->getMessage(), '1062') !== false) {
                $pdo->rollBack();
                logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
                Response::json(409, ['error' => 'You have already predicted on one or more of these questions.']);
            }
            throw $e; // Re-throw other PDOExceptions to be caught by the outer block
        }
    }

    // Commit Transaction if all predictions were valid and successfully inserted
    $pdo->commit();

    logSecurityEvent($pdo, $ipAddress, $requestUri, 'success');
    Response::json(201, ['message' => 'Predictions locked successfully.']);

} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Bulk Prediction DB Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while processing predictions.']);
} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Bulk Prediction General Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An unexpected error occurred.']);
}

/**
 * Log the security event asynchronously
 */
function logSecurityEvent(PDO $pdo, string $ipAddress, string $endpoint, string $status): void
{
    try {
        // Run on a separate prepared statement outside the main transaction scope if possible,
        // but here it shares the connection. If the main transaction rolls back, we still want this logged.
        // For strictness, we instantiate a new connection for logging or execute it after rollback/commit.
        // In this implementation, it's called after rollback/commit, so it's safe.
        $stmt = $pdo->prepare("INSERT INTO security_logs (ip_address, endpoint_accessed, status) VALUES (:ip, :endpoint, :status)");
        $stmt->execute([
            ':ip' => $ipAddress,
            ':endpoint' => $endpoint,
            ':status' => $status
        ]);
    } catch (Exception $e) {
        error_log("Failed to insert security log: " . $e->getMessage());
    }
}
