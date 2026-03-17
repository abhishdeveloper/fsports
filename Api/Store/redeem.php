<?php

declare(strict_types=1);

namespace App\Api\Store;

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

$rewardId = filter_var($input['reward_id'] ?? null, FILTER_VALIDATE_INT);

if (!$rewardId) {
    Response::json(400, ['error' => 'A valid reward_id integer is required.']);
}

/**
 * Generates an unambiguous 6-character alphanumeric code.
 * Excludes confusing characters like 0, O, 1, I, L.
 */
function generateRedemptionCode(): string {
    $chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    $code = '';
    for ($i = 0; $i < 6; $i++) {
        $code .= $chars[random_int(0, strlen($chars) - 1)];
    }
    return $code;
}

try {
    $pdo = Database::getConnection();

    // 1. Begin Safe Transaction
    // Critical for preventing race conditions (e.g., negative coin balances or overselling out-of-stock perks)
    $pdo->beginTransaction();

    // 2. Lock and Check Reward Stock
    $rewardStmt = $pdo->prepare("SELECT title, cost, stock_quantity, is_active FROM rewards WHERE id = :id FOR UPDATE");
    $rewardStmt->execute([':id' => $rewardId]);
    $reward = $rewardStmt->fetch(PDO::FETCH_ASSOC);

    if (!$reward || !$reward['is_active']) {
        $pdo->rollBack();
        Response::json(404, ['error' => 'Reward not found or no longer active.']);
    }

    if ((int)$reward['stock_quantity'] <= 0) {
        $pdo->rollBack();
        Response::json(409, ['error' => 'Reward is out of stock.']);
    }

    $cost = (int)$reward['cost'];

    // 3. Lock and Check User Balance
    $userStmt = $pdo->prepare("SELECT total_coins FROM users WHERE id = :id FOR UPDATE");
    $userStmt->execute([':id' => $userId]);
    $user = $userStmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        $pdo->rollBack();
        Response::json(404, ['error' => 'User not found.']);
    }

    if ((int)$user['total_coins'] < $cost) {
        $pdo->rollBack();
        Response::json(400, ['error' => 'Insufficient funds. Keep predicting to earn more coins!']);
    }

    // 4. Deduct Coins
    $deductStmt = $pdo->prepare("UPDATE users SET total_coins = total_coins - :cost WHERE id = :id");
    $deductStmt->execute([':cost' => $cost, ':id' => $userId]);

    // 5. Decrement Stock
    $decrementStmt = $pdo->prepare("UPDATE rewards SET stock_quantity = stock_quantity - 1 WHERE id = :id");
    $decrementStmt->execute([':id' => $rewardId]);

    // 6. Generate and Insert Unique Redemption Code
    $insertStmt = $pdo->prepare("
        INSERT INTO redemptions (user_id, reward_id, redemption_code, status)
        VALUES (:user_id, :reward_id, :code, 'active')
    ");

    $codeAssigned = false;
    $maxRetries = 5;
    $retries = 0;
    $finalCode = '';

    while (!$codeAssigned && $retries < $maxRetries) {
        $candidateCode = generateRedemptionCode();
        try {
            $insertStmt->execute([
                ':user_id' => $userId,
                ':reward_id' => $rewardId,
                ':code' => $candidateCode
            ]);
            $finalCode = $candidateCode;
            $codeAssigned = true;
        } catch (PDOException $e) {
            // Check for MySQL duplicate entry error code (1062) on idx_redemption_code
            if ($e->getCode() === '23000' && strpos($e->getMessage(), '1062') !== false) {
                $retries++;
            } else {
                throw $e; // Re-throw other PDO exceptions
            }
        }
    }

    if (!$codeAssigned) {
        $pdo->rollBack();
        Response::json(500, ['error' => 'Failed to generate a unique redemption code. Please try again.']);
    }

    // 7. Commit Transaction
    $pdo->commit();

    Response::json(201, [
        'message' => "Successfully redeemed '{$reward['title']}'. Enjoy!",
        'redemption_code' => $finalCode
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Redeem Transaction Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while processing the redemption.']);
}