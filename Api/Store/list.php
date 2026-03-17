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

authenticateJWT();

try {
    $pdo = Database::getConnection();

    // Fetch active rewards that are currently in stock
    $stmt = $pdo->prepare("
        SELECT id, title, description, cost, stock_quantity
        FROM rewards
        WHERE is_active = 1 AND stock_quantity > 0
        ORDER BY cost ASC
    ");
    $stmt->execute();

    $rewards = $stmt->fetchAll(PDO::FETCH_ASSOC);

    Response::json(200, ['data' => $rewards ?: []]);

} catch (Exception $e) {
    error_log("Rewards Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while fetching the rewards.']);
}