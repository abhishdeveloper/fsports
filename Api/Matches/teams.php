<?php

declare(strict_types=1);

namespace App\Api\Matches;

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

authenticateJWT();

try {
    $pdo = Database::getConnection();

    $stmt = $pdo->prepare("SELECT id, team_name, branch_name FROM teams ORDER BY team_name ASC");
    $stmt->execute();

    $teams = $stmt->fetchAll(PDO::FETCH_ASSOC);
    Response::json(200, ['data' => $teams ?: []]);

} catch (Exception $e) {
    error_log("Teams Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while fetching teams.']);
}