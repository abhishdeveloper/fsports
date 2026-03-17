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

if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

$userPayload = authenticateJWT();
$userId = (int)($userPayload['sub'] ?? 0);

if (!$userId) {
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}

$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, true);

if (!$input) {
    Response::json(400, ['error' => 'Invalid JSON payload.']);
}

$sportId = filter_var($input['sport_id'] ?? null, FILTER_VALIDATE_INT);
$teamAId = filter_var($input['team_a_id'] ?? null, FILTER_VALIDATE_INT);
$teamBId = filter_var($input['team_b_id'] ?? null, FILTER_VALIDATE_INT);
$startTime = trim((string)($input['start_time'] ?? ''));

if (!$sportId || !$teamAId || !$teamBId || empty($startTime)) {
    Response::json(400, ['error' => 'sport_id, team_a_id, team_b_id, and start_time are required.']);
}

if ($teamAId === $teamBId) {
    Response::json(400, ['error' => 'Team A and Team B cannot be the same.']);
}

try {
    $pdo = Database::getConnection();

    // Verify Admin Role
    $stmtAdmin = $pdo->prepare("SELECT role FROM users WHERE id = :id LIMIT 1");
    $stmtAdmin->execute([':id' => $userId]);
    $user = $stmtAdmin->fetch(PDO::FETCH_ASSOC);

    if (!$user || $user['role'] !== 'admin') {
        Response::json(403, ['error' => 'Forbidden: You do not have admin privileges.']);
    }

    $insertStmt = $pdo->prepare("
        INSERT INTO matches (sport_id, team_a_id, team_b_id, start_time, match_status)
        VALUES (:sport_id, :team_a_id, :team_b_id, :start_time, 'upcoming')
    ");

    $insertStmt->execute([
        ':sport_id' => $sportId,
        ':team_a_id' => $teamAId,
        ':team_b_id' => $teamBId,
        ':start_time' => $startTime
    ]);

    Response::json(201, ['message' => 'Match created successfully.']);

} catch (Exception $e) {
    // If the check constraint `chk_different_teams` or foreign keys fail, it hits here
    error_log("Admin Create Match Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while creating the match.']);
}