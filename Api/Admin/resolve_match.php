<?php

declare(strict_types=1);

namespace App\Api\Admin;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';
require_once __DIR__ . '/../Middleware/auth_middleware.php';

use function App\Api\Middleware\authenticateJWT;

// Only allow POST requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

$BASE_POINTS = 100;

// 1. Authenticate Request
$userPayload = authenticateJWT();
$userId = (int)($userPayload['sub'] ?? 0);

if (!$userId) {
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}

// Fetch JSON payload
$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, true);

if (!$input) {
    Response::json(400, ['error' => 'Invalid JSON payload.']);
}

$matchId = filter_var($input['match_id'] ?? null, FILTER_VALIDATE_INT);
$resolutions = $input['resolutions'] ?? null; // format: { "question_id": "A", "question_id2": "C" }

if (!$matchId || !is_array($resolutions) || empty($resolutions)) {
    Response::json(400, ['error' => 'match_id and a non-empty resolutions object are required.']);
}

try {
    $pdo = Database::getConnection();

    // 2. Verify Admin Role via direct DB query (safer than JWT)
    $stmtAdmin = $pdo->prepare("SELECT role FROM users WHERE id = :id LIMIT 1");
    $stmtAdmin->execute([':id' => $userId]);
    $user = $stmtAdmin->fetch(PDO::FETCH_ASSOC);

    if (!$user || $user['role'] !== 'admin') {
        Response::json(403, ['error' => 'Forbidden: You do not have admin privileges.']);
    }

    // 3. Verify match exists and is not already completed
    $stmtMatch = $pdo->prepare("SELECT match_status FROM matches WHERE id = :match_id LIMIT 1");
    $stmtMatch->execute([':match_id' => $matchId]);
    $match = $stmtMatch->fetch(PDO::FETCH_ASSOC);

    if (!$match) {
        Response::json(404, ['error' => 'Match not found.']);
    }
    if ($match['match_status'] === 'completed' || $match['match_status'] === 'cancelled') {
        Response::json(400, ['error' => 'Match is already resolved or cancelled.']);
    }

    // 4. Begin Resolution Transaction
    $pdo->beginTransaction();

    // Prepare statements for updating questions
    $updateQuestionStmt = $pdo->prepare("
        UPDATE questions
        SET correct_option = :correct_option
        WHERE id = :question_id AND match_id = :match_id
    ");

    foreach ($resolutions as $qIdStr => $correctOption) {
        $qId = (int)$qIdStr;
        $opt = strtoupper(trim((string)$correctOption));

        if (!in_array($opt, ['A', 'B', 'C', 'D'], true)) {
            $pdo->rollBack();
            Response::json(400, ['error' => "Invalid correct_option '$opt' for question $qId."]);
        }

        $updateQuestionStmt->execute([
            ':correct_option' => $opt,
            ':question_id' => $qId,
            ':match_id' => $matchId
        ]);

        // If row count is 0, the question ID doesn't belong to this match_id or doesn't exist
        if ($updateQuestionStmt->rowCount() === 0) {
            $pdo->rollBack();
            Response::json(400, ['error' => "Question ID $qId is invalid or does not belong to this match."]);
        }
    }

    // 5. Update Predictions: Mark as 'won' or 'lost' based on the newly set correct_option
    // We join the predictions table with the questions table to compare selected vs correct
    $resolvePredictionsStmt = $pdo->prepare("
        UPDATE predictions p
        JOIN questions q ON p.question_id = q.id
        SET p.status = CASE
            WHEN p.selected_option = q.correct_option THEN 'won'
            ELSE 'lost'
        END
        WHERE q.match_id = :match_id AND p.status = 'pending'
    ");
    $resolvePredictionsStmt->execute([':match_id' => $matchId]);

    // 6. Award Points to Users
    // Calculate total won points per user for this match and add them to total_coins
    $awardPointsStmt = $pdo->prepare("
        UPDATE users u
        JOIN (
            SELECT p.user_id, SUM(:base_points * q.points_multiplier) as total_earned
            FROM predictions p
            JOIN questions q ON p.question_id = q.id
            WHERE q.match_id = :match_id AND p.status = 'won'
            GROUP BY p.user_id
        ) as winners ON u.id = winners.user_id
        SET u.total_coins = u.total_coins + winners.total_earned
    ");
    $awardPointsStmt->execute([
        ':base_points' => $BASE_POINTS,
        ':match_id' => $matchId
    ]);

    // 7. Complete the Match
    $completeMatchStmt = $pdo->prepare("
        UPDATE matches
        SET match_status = 'completed'
        WHERE id = :match_id
    ");
    $completeMatchStmt->execute([':match_id' => $matchId]);

    // 8. Commit Transaction
    $pdo->commit();

    Response::json(200, ['message' => 'Match resolved successfully. Points awarded.']);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Admin Resolve Match Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while resolving the match.']);
}
