<?php

declare(strict_types=1);

namespace App\Api\Users;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';
require_once __DIR__ . '/../Middleware/auth_middleware.php';

use function App\Api\Middleware\authenticateJWT;

if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'GET') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

$userPayload = authenticateJWT();
$userId = (int)($userPayload['sub'] ?? 0);

if (!$userId) {
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}

try {
    $pdo = Database::getConnection();

    // 1. Fetch User Profile Data & Global Rank
    // Using DENSE_RANK to find their exact competitive standing
    $profileQuery = "
        SELECT
            u.id,
            u.username,
            u.branch,
            u.total_coins,
            (
                SELECT COUNT(DISTINCT total_coins) + 1
                FROM users
                WHERE total_coins > u.total_coins AND role != 'admin'
            ) AS global_rank
        FROM users u
        WHERE u.id = :user_id
        LIMIT 1
    ";

    $profileStmt = $pdo->prepare($profileQuery);
    $profileStmt->execute([':user_id' => $userId]);
    $profile = $profileStmt->fetch(PDO::FETCH_ASSOC);

    if (!$profile) {
        Response::json(404, ['error' => 'User profile not found.']);
    }

    // 2. Fetch Prediction History (Joined with matches, teams, and questions)
    $historyQuery = "
        SELECT
            p.id AS prediction_id,
            p.selected_option,
            p.status AS prediction_status,
            p.locked_at,
            q.question_text,
            q.correct_option,
            q.points_multiplier,
            q.option_a,
            q.option_b,
            q.option_c,
            q.option_d,
            m.start_time,
            m.match_status,
            s.sport_name,
            t_a.team_name AS team_a_name,
            t_b.team_name AS team_b_name
        FROM predictions p
        JOIN questions q ON p.question_id = q.id
        JOIN matches m ON q.match_id = m.id
        JOIN sports s ON m.sport_id = s.id
        JOIN teams t_a ON m.team_a_id = t_a.id
        JOIN teams t_b ON m.team_b_id = t_b.id
        WHERE p.user_id = :user_id
        ORDER BY p.locked_at DESC
        LIMIT 50 -- Reasonable limit for history view
    ";

    $historyStmt = $pdo->prepare($historyQuery);
    $historyStmt->execute([':user_id' => $userId]);
    $history = $historyStmt->fetchAll(PDO::FETCH_ASSOC);

    // 3. Format the JSON Response accurately for the Android client
    $formattedHistory = array_map(function($item) {
        // Map the correct option letter to the actual text if the match is completed
        $correctAnswerText = null;
        $selectedAnswerText = null;

        $optionsMap = [
            'A' => $item['option_a'],
            'B' => $item['option_b'],
            'C' => $item['option_c'],
            'D' => $item['option_d']
        ];

        if (isset($optionsMap[$item['selected_option']])) {
            $selectedAnswerText = $optionsMap[$item['selected_option']];
        }

        // Only reveal the correct answer if the backend has resolved it
        if ($item['correct_option'] && isset($optionsMap[$item['correct_option']])) {
            $correctAnswerText = $optionsMap[$item['correct_option']];
        }

        return [
            'prediction_id' => (int)$item['prediction_id'],
            'match_name' => $item['team_a_name'] . ' vs ' . $item['team_b_name'],
            'sport_name' => $item['sport_name'],
            'question_text' => $item['question_text'],
            'selected_answer' => $selectedAnswerText,
            'correct_answer' => $correctAnswerText, // Can be null if pending
            'status' => $item['prediction_status'], // 'pending', 'won', 'lost'
            'points_multiplier' => (float)$item['points_multiplier'],
            'locked_at' => $item['locked_at']
        ];
    }, $history);

    Response::json(200, [
        'profile' => [
            'username' => $profile['username'],
            'branch' => $profile['branch'],
            'total_coins' => (int)$profile['total_coins'],
            'global_rank' => (int)$profile['global_rank']
        ],
        'history' => $formattedHistory
    ]);

} catch (Exception $e) {
    error_log("User History Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while fetching your profile history.']);
}