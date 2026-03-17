<?php

declare(strict_types=1);

namespace App\Api\Store;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;

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

    // Fetch user's redemptions joined with reward details
    // Orders 'active' first, then sorts by redemption date descending
    $stmt = $pdo->prepare("
        SELECT
            r.redemption_code,
            r.status,
            r.redeemed_at,
            rw.title,
            rw.description
        FROM redemptions r
        JOIN rewards rw ON r.reward_id = rw.id
        WHERE r.user_id = :user_id
        ORDER BY
            CASE WHEN r.status = 'active' THEN 1 ELSE 2 END ASC,
            r.redeemed_at DESC
    ");
    $stmt->execute([':user_id' => $userId]);

    $passes = $stmt->fetchAll(PDO::FETCH_ASSOC);

    Response::json(200, ['data' => $passes ?: []]);

} catch (Exception $e) {
    error_log("My Passes Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while fetching your digital passes.']);
}