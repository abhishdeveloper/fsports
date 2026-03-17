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

// Only allow POST requests for executing the seeder to prevent accidental GET triggers
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// 1. Authenticate Request
$userPayload = authenticateJWT();
$userId = (int)($userPayload['sub'] ?? 0);

if (!$userId) {
    Response::json(401, ['error' => 'Invalid authentication token payload.']);
}

try {
    $pdo = Database::getConnection();

    // 2. Verify Admin Role via direct DB query
    $stmtAdmin = $pdo->prepare("SELECT role FROM users WHERE id = :id LIMIT 1");
    $stmtAdmin->execute([':id' => $userId]);
    $user = $stmtAdmin->fetch(PDO::FETCH_ASSOC);

    if (!$user || $user['role'] !== 'admin') {
        Response::json(403, ['error' => 'Forbidden: You do not have admin privileges to run the seeder.']);
    }

    // 3. Begin Safe Transaction
    $pdo->beginTransaction();

    // --- PHASE 1: AGGRESSIVE DATA CLEARING ---
    // Wipe dependencies strictly in reverse order to bypass ON DELETE RESTRICT constraints safely
    $pdo->exec("DELETE FROM predictions");
    $pdo->exec("DELETE FROM questions");
    $pdo->exec("DELETE FROM matches");
    $pdo->exec("DELETE FROM players");
    $pdo->exec("DELETE FROM teams");
    $pdo->exec("DELETE FROM sports");

    // Reset Auto Increment Counters
    $pdo->exec("ALTER TABLE sports AUTO_INCREMENT = 1");
    $pdo->exec("ALTER TABLE teams AUTO_INCREMENT = 1");
    $pdo->exec("ALTER TABLE players AUTO_INCREMENT = 1");
    $pdo->exec("ALTER TABLE matches AUTO_INCREMENT = 1");
    $pdo->exec("ALTER TABLE questions AUTO_INCREMENT = 1");
    $pdo->exec("ALTER TABLE predictions AUTO_INCREMENT = 1");


    // --- PHASE 2: SEED SPORTS & TEAMS ---
    $pdo->exec("INSERT INTO sports (sport_name) VALUES ('Cricket')");
    $sportId = (int)$pdo->lastInsertId();

    $teamsData = [
        ['CSK', 'Chennai'],
        ['MI', 'Mumbai'],
        ['RCB', 'Bangalore'],
        ['KKR', 'Kolkata'],
        ['RR', 'Rajasthan'],
        ['DC', 'Delhi']
    ];

    $teamStmt = $pdo->prepare("INSERT INTO teams (team_name, branch_name) VALUES (?, ?)");
    $teamIds = [];
    foreach ($teamsData as $index => $team) {
        $teamStmt->execute([$team[0], $team[1]]);
        $teamIds[$team[0]] = (int)$pdo->lastInsertId();
    }


    // --- PHASE 3: SEED MARQUEE PLAYERS ---
    $playersData = [
        // CSK
        ['MS Dhoni', 'Wicketkeeper', 'CSK'],
        ['Ravindra Jadeja', 'All-rounder', 'CSK'],
        ['Ruturaj Gaikwad', 'Batsman', 'CSK'],
        // MI
        ['Rohit Sharma', 'Batsman', 'MI'],
        ['Jasprit Bumrah', 'Bowler', 'MI'],
        ['Suryakumar Yadav', 'Batsman', 'MI'],
        // RCB
        ['Virat Kohli', 'Batsman', 'RCB'],
        ['Faf du Plessis', 'Batsman', 'RCB'],
        ['Mohammed Siraj', 'Bowler', 'RCB'],
        // KKR
        ['Shreyas Iyer', 'Batsman', 'KKR'],
        ['Sunil Narine', 'All-rounder', 'KKR'],
        ['Andre Russell', 'All-rounder', 'KKR'],
        // RR
        ['Sanju Samson', 'Wicketkeeper', 'RR'],
        ['Jos Buttler', 'Batsman', 'RR'],
        ['Yuzvendra Chahal', 'Bowler', 'RR'],
        // DC
        ['Rishabh Pant', 'Wicketkeeper', 'DC'],
        ['David Warner', 'Batsman', 'DC'],
        ['Axar Patel', 'All-rounder', 'DC']
    ];

    $playerStmt = $pdo->prepare("INSERT INTO players (team_id, player_name, role) VALUES (?, ?, ?)");
    foreach ($playersData as $player) {
        $playerStmt->execute([$teamIds[$player[2]], $player[0], $player[1]]);
    }


    // --- PHASE 4: DUMMY SCHEDULE (FIRST 5 MATCHES APRIL 2026) ---
    $matchesData = [
        ['CSK', 'RCB', '2026-04-01 19:30:00'],
        ['MI', 'DC',   '2026-04-02 15:30:00'],
        ['KKR', 'RR',  '2026-04-02 19:30:00'],
        ['CSK', 'MI',  '2026-04-04 19:30:00'],
        ['RCB', 'KKR', '2026-04-05 19:30:00']
    ];

    $matchStmt = $pdo->prepare("INSERT INTO matches (sport_id, team_a_id, team_b_id, start_time, match_status) VALUES (?, ?, ?, ?, 'upcoming')");
    $matchIds = [];
    foreach ($matchesData as $match) {
        $matchStmt->execute([$sportId, $teamIds[$match[0]], $teamIds[$match[1]], $match[2]]);
        $matchIds[] = [
            'id' => (int)$pdo->lastInsertId(),
            'team_a_name' => $match[0],
            'team_b_name' => $match[1]
        ];
    }


    // --- PHASE 5: QUESTIONS (POLLS) ---
    $questionStmt = $pdo->prepare("
        INSERT INTO questions (match_id, question_text, option_a, option_b, option_c, option_d, points_multiplier)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    ");

    foreach ($matchIds as $matchObj) {
        $mId = $matchObj['id'];
        $teamA = $matchObj['team_a_name'];
        $teamB = $matchObj['team_b_name'];

        // Q1: Toss
        $questionStmt->execute([
            $mId,
            "Who will win the toss?",
            $teamA,
            $teamB,
            null,
            null,
            1.00 // Lower risk multiplier
        ]);

        // Q2: Most Sixes
        $questionStmt->execute([
            $mId,
            "Which team will hit the most sixes?",
            $teamA,
            $teamB,
            "Tie",
            null,
            1.50 // Medium risk multiplier
        ]);

        // Q3: 170+ Score
        $questionStmt->execute([
            $mId,
            "Will the first innings score be 170+?",
            "Yes",
            "No",
            null,
            null,
            2.00 // Higher risk multiplier
        ]);
    }

    // 4. Commit Transaction
    $pdo->commit();

    Response::json(201, [
        'message' => 'IPL 2026 Test Environment Successfully Seeded.',
        'details' => 'Database cleanly wiped. Inserted 1 Sport, 6 Teams, 18 Marquee Players, 5 Upcoming Matches, and 15 Prediction Questions.'
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("IPL 2026 Seeder Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred while executing the database seeder.']);
}