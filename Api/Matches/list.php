<?php

declare(strict_types=1);

namespace App\Api\Matches;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';
// Require Middleware
require_once __DIR__ . '/../Middleware/auth_middleware.php';

use function App\Api\Middleware\authenticateJWT;

// Only allow GET requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'GET') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// 1. Authenticate Request
// If invalid or missing, this function will automatically halt execution and return 401
$userPayload = authenticateJWT();

try {
    $pdo = Database::getConnection();

    // 2. Fetch all matches (excluding cancelled), joined with sports and teams
    $query = "
        SELECT
            m.id AS match_id,
            m.start_time,
            m.match_status,
            s.sport_name,
            t_a.team_name AS team_a_name,
            t_a.branch_name AS team_a_branch,
            t_b.team_name AS team_b_name,
            t_b.branch_name AS team_b_branch
        FROM matches m
        JOIN sports s ON m.sport_id = s.id
        JOIN teams t_a ON m.team_a_id = t_a.id
        JOIN teams t_b ON m.team_b_id = t_b.id
        WHERE m.match_status != 'cancelled'
        ORDER BY m.start_time ASC
    ";

    $stmt = $pdo->prepare($query);
    $stmt->execute();

    $matches = $stmt->fetchAll();

    // 3. Group the response into three arrays
    $response = [
        'upcoming'  => [],
        'live'      => [],
        'completed' => []
    ];

    foreach ($matches as $match) {
        $status = $match['match_status'];

        // Format the match array for the response
        $formattedMatch = [
            'id' => $match['match_id'],
            'sport' => $match['sport_name'],
            'status' => $status,
            'team_a' => [
                'name' => $match['team_a_name'],
                'branch' => $match['team_a_branch']
            ],
            'team_b' => [
                'name' => $match['team_b_name'],
                'branch' => $match['team_b_branch']
            ],
            'start_time' => $match['start_time']
        ];

        // Safely assign to the correct group
        if (isset($response[$status])) {
            $response[$status][] = $formattedMatch;
        }
    }

    Response::json(200, $response);

} catch (Exception $e) {
    error_log("Matches List Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred.']);
}
