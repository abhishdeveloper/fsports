<?php

declare(strict_types=1);

namespace App\Api\Leaderboard;

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
$userPayload = authenticateJWT();

try {
    $pdo = Database::getConnection();

    // 2. Fetch Top 50 Users using DENSE_RANK()
    // DENSE_RANK assigns consecutive ranks even if there are ties
    // Regular RANK() would skip ranks (e.g., 1, 1, 3). DENSE_RANK gives (1, 1, 2).
    $query = "
        SELECT
            DENSE_RANK() OVER (ORDER BY total_coins DESC) as rank,
            username,
            branch,
            total_coins
        FROM users
        WHERE role != 'admin' -- Optional: Exclude admins from the leaderboard to be fair
        ORDER BY total_coins DESC, id ASC -- Tie-breaker: older accounts appear first if ranks match
        LIMIT 50
    ";

    $stmt = $pdo->prepare($query);
    $stmt->execute();

    $leaderboard = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Format integer strings to actual integers if PDO returned them as strings
    $formattedLeaderboard = array_map(function($user) {
        return [
            'rank' => (int)$user['rank'],
            'username' => $user['username'],
            'branch' => $user['branch'],
            'total_coins' => (int)$user['total_coins']
        ];
    }, $leaderboard);

    Response::json(200, ['data' => $formattedLeaderboard]);

} catch (Exception $e) {
    error_log("Leaderboard Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while fetching the leaderboard.']);
}