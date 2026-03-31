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

$matchId = filter_var($input['match_id'] ?? null, FILTER_VALIDATE_INT);
$newStatus = trim((string)($input['new_status'] ?? ''));

$allowedStatuses = ['upcoming', 'live', 'completed', 'cancelled'];

if (!$matchId || !in_array($newStatus, $allowedStatuses, true)) {
    Response::json(400, ['error' => "Valid match_id and new_status (upcoming, live, completed, cancelled) are required."]);
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

    $updateStmt = $pdo->prepare("UPDATE matches SET match_status = :new_status WHERE id = :match_id");
    $updateStmt->execute([
        ':new_status' => $newStatus,
        ':match_id' => $matchId
    ]);

    if ($updateStmt->rowCount() === 0) {
         Response::json(404, ['error' => 'Match not found or status already matches the requested state.']);
    }

    // Phase 14: Trigger Global Push Notification when a Match goes LIVE
    if ($newStatus === 'live') {
        // Fetch team names for a detailed notification
        $teamsStmt = $pdo->prepare("
            SELECT t_a.team_name as team_a, t_b.team_name as team_b
            FROM matches m
            JOIN teams t_a ON m.team_a_id = t_a.id
            JOIN teams t_b ON m.team_b_id = t_b.id
            WHERE m.id = :match_id
        ");
        $teamsStmt->execute([':match_id' => $matchId]);
        $teams = $teamsStmt->fetch(PDO::FETCH_ASSOC);

        $matchTitle = $teams ? "{$teams['team_a']} vs {$teams['team_b']} is LIVE! 🏏" : "A match is LIVE! 🏏";

        // Push Notifications disabled in Zero-Dependency Shared Hosting Mode
    }

    Response::json(200, ['message' => "Match status updated to '$newStatus'."]);

} catch (Exception $e) {
    error_log("Admin Update Match Status Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while updating the match status.']);
}