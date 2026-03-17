<?php

declare(strict_types=1);

namespace App\Api\Admin;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use PDOException;
use Exception;

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

$redemptionCode = trim((string)($input['redemption_code'] ?? ''));

if (empty($redemptionCode) || strlen($redemptionCode) !== 6) {
    Response::json(400, ['error' => 'A valid 6-character redemption code is required.']);
}

try {
    $pdo = Database::getConnection();

    // 1. Verify Admin Role via direct DB query (safer than JWT)
    $stmtAdmin = $pdo->prepare("SELECT role FROM users WHERE id = :id LIMIT 1");
    $stmtAdmin->execute([':id' => $userId]);
    $user = $stmtAdmin->fetch(PDO::FETCH_ASSOC);

    if (!$user || $user['role'] !== 'admin') {
        Response::json(403, ['error' => 'Forbidden: You do not have admin privileges.']);
    }

    // 2. Mark code as 'claimed'
    $updateStmt = $pdo->prepare("
        UPDATE redemptions
        SET status = 'claimed'
        WHERE redemption_code = :code AND status = 'active'
    ");

    $updateStmt->execute([':code' => $redemptionCode]);

    if ($updateStmt->rowCount() === 0) {
        Response::json(404, ['error' => 'Code is invalid, already claimed, or does not exist.']);
    }

    Response::json(200, ['message' => 'Reward successfully claimed and verified.']);

} catch (Exception $e) {
    error_log("Admin Claim Reward Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while updating the redemption status.']);
}