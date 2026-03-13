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
// Extracts the user payload from the decoded token
$userPayload = authenticateJWT();

// Strictly retrieve user ID from JWT to prevent tampering
$userId = $userPayload['sub'] ?? null;

if (!$userId || !is_numeric($userId)) {
    // Should theoretically never happen if the JWT structure is valid
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}

$userId = (int) $userId;

// Fetch JSON payload
$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, true);

if (!$input) {
    Response::json(400, ['error' => 'Invalid JSON payload']);
}

$questionIdStr = $input['question_id'] ?? null;
$selectedOption = strtoupper(trim($input['selected_option'] ?? ''));

// Validate inputs
if ($questionIdStr === null || !filter_var($questionIdStr, FILTER_VALIDATE_INT)) {
    Response::json(400, ['error' => 'A valid question_id integer is required.']);
}

$questionId = (int) $questionIdStr;

if (!in_array($selectedOption, ['A', 'B', 'C', 'D'], true)) {
    Response::json(400, ['error' => 'Invalid option selected. Must be A, B, C, or D.']);
}

// Securely handle endpoint paths by stripping query parameters to maintain clean logs
$requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/Api/Predictions/submit.php', PHP_URL_PATH);
// Sanitize IP
$ipAddress = filter_var($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0', FILTER_VALIDATE_IP) ?: '0.0.0.0';

try {
    $pdo = Database::getConnection();

    // 2. Validation 1: Check match status
    // Join questions to matches to verify the match is still upcoming
    $statusQuery = "
        SELECT m.match_status
        FROM questions q
        JOIN matches m ON q.match_id = m.id
        WHERE q.id = :question_id
        LIMIT 1
    ";

    $statusStmt = $pdo->prepare($statusQuery);
    $statusStmt->execute([':question_id' => $questionId]);

    $match = $statusStmt->fetch(PDO::FETCH_ASSOC);

    if (!$match) {
        logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
        Response::json(404, ['error' => 'Question not found.']);
    }

    if ($match['match_status'] !== 'upcoming') {
        logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
        Response::json(403, ['error' => 'Betting closed for this match.']);
    }

    // 3. Validation 2: Insert Prediction
    $insertQuery = "
        INSERT INTO predictions (user_id, question_id, selected_option, status)
        VALUES (:user_id, :question_id, :selected_option, 'pending')
    ";

    $insertStmt = $pdo->prepare($insertQuery);
    $insertStmt->execute([
        ':user_id' => $userId,
        ':question_id' => $questionId,
        ':selected_option' => $selectedOption
    ]);

    // Log the successful prediction
    logSecurityEvent($pdo, $ipAddress, $requestUri, 'success');

    Response::json(201, ['message' => 'Prediction submitted successfully.']);

} catch (PDOException $e) {
    // Check for MySQL duplicate entry error code (1062) for the unique idx_user_question constraint
    if ($e->getCode() === '23000' && strpos($e->getMessage(), '1062') !== false) {
        logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
        Response::json(409, ['error' => 'You have already predicted on this question.']);
    }

    // Log unexpected database errors securely
    error_log("Prediction DB Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred.']);
} catch (Exception $e) {
    // Log unexpected general errors securely
    error_log("Prediction General Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An unexpected error occurred.']);
}

/**
 * Log the security event asynchronously
 */
function logSecurityEvent(PDO $pdo, string $ipAddress, string $endpoint, string $status): void
{
    try {
        $stmt = $pdo->prepare("INSERT INTO security_logs (ip_address, endpoint_accessed, status) VALUES (:ip, :endpoint, :status)");
        $stmt->execute([
            ':ip' => $ipAddress,
            ':endpoint' => $endpoint,
            ':status' => $status
        ]);
    } catch (Exception $e) {
        // Do not interrupt the flow if logging fails
        error_log("Failed to insert security log: " . $e->getMessage());
    }
}
