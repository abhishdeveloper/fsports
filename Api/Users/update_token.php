<?php

declare(strict_types=1);

namespace App\Api\Users;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
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

$fcmToken = trim((string)($input['fcm_token'] ?? ''));

if (empty($fcmToken)) {
    Response::json(400, ['error' => 'fcm_token is required.']);
}

try {
    $pdo = Database::getConnection();

    $stmt = $pdo->prepare("UPDATE users SET fcm_token = :fcm_token WHERE id = :id");
    $stmt->execute([
        ':fcm_token' => $fcmToken,
        ':id' => $userId
    ]);

    Response::json(200, ['message' => 'FCM token updated successfully.']);

} catch (Exception $e) {
    error_log("Update FCM Token Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while saving the token.']);
}