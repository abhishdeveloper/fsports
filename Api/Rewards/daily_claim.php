<?php

declare(strict_types=1);

namespace App\Api\Rewards;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;
use DateTime;

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

$BASE_REWARD = 100;
$STREAK_MULTIPLIER = 10; // Extra 10 coins per consecutive day (e.g., Day 1 = 100, Day 2 = 110, Day 7 = 160)
$MAX_STREAK = 7;

try {
    $pdo = Database::getConnection();

    // We must lock the row FOR UPDATE to prevent race conditions (double-claiming via concurrent requests)
    $pdo->beginTransaction();

    $stmt = $pdo->prepare("SELECT last_claim_time, current_streak FROM users WHERE id = :id FOR UPDATE");
    $stmt->execute([':id' => $userId]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        $pdo->rollBack();
        Response::json(404, ['error' => 'User not found.']);
    }

    $now = new DateTime();
    $lastClaimTime = $user['last_claim_time'] ? new DateTime($user['last_claim_time']) : null;
    $streak = (int)$user['current_streak'];

    // Cooldown Validation
    if ($lastClaimTime) {
        $interval = $now->diff($lastClaimTime);
        $hoursSinceLastClaim = ($interval->days * 24) + $interval->h + ($interval->i / 60);

        if ($hoursSinceLastClaim < 24) {
            $pdo->rollBack();
            $hoursLeft = 24 - $hoursSinceLastClaim;
            Response::json(403, [
                'error' => 'Daily reward already claimed.',
                'hours_left' => round($hoursLeft, 1)
            ]);
        }

        // Streak Validation: If more than 48 hours have passed, the streak breaks.
        if ($hoursSinceLastClaim > 48) {
            $streak = 0;
        }
    }

    // Increment streak (cap at MAX_STREAK)
    $streak++;
    if ($streak > $MAX_STREAK) {
        $streak = $MAX_STREAK;
    }

    // Calculate Reward
    $rewardCoins = $BASE_REWARD + (($streak - 1) * $STREAK_MULTIPLIER);

    // Update User
    $updateStmt = $pdo->prepare("
        UPDATE users
        SET total_coins = total_coins + :reward,
            last_claim_time = CURRENT_TIMESTAMP,
            current_streak = :streak
        WHERE id = :id
    ");

    $updateStmt->execute([
        ':reward' => $rewardCoins,
        ':streak' => $streak,
        ':id' => $userId
    ]);

    $pdo->commit();

    Response::json(200, [
        'message' => 'Daily reward claimed successfully!',
        'reward_coins' => $rewardCoins,
        'current_streak' => $streak,
        'max_streak' => $MAX_STREAK
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Daily Claim Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while processing the daily claim.']);
}