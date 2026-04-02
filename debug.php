<?php

// FORCE ALL ERRORS TO SHOW ON THE SCREEN FOR DEBUGGING
ini_set('display_errors', '1');
ini_set('display_startup_errors', '1');
error_reporting(E_ALL);

echo "<h1>HestiaCP / Shared Hosting Diagnostic Tool</h1>";
echo "<p>Running PHP Version: " . phpversion() . "</p><hr>";

try {
    echo "<h3>1. Testing File Permissions & Paths</h3>";
    $configPath = __DIR__ . '/Config/credentials.php';
    if (file_exists($configPath)) {
        echo "<p style='color:green'>✔ Config/credentials.php found.</p>";
        require_once $configPath;
    } else {
        throw new Exception("❌ Config/credentials.php is MISSING. Did you rename credentials.example.php?");
    }

    echo "<h3>2. Testing Autoloader & Bootstrap</h3>";
    $bootstrapPath = __DIR__ . '/bootstrap.php';
    if (file_exists($bootstrapPath)) {
        require_once $bootstrapPath;
        echo "<p style='color:green'>✔ bootstrap.php loaded successfully.</p>";
    } else {
        throw new Exception("❌ bootstrap.php is MISSING.");
    }

    echo "<h3>3. Testing Database Connection (PDO)</h3>";
    // Check if the constants are defined from credentials.php
    if (!defined('DB_HOST') || !defined('DB_NAME') || !defined('DB_USER') || !defined('DB_PASS')) {
        throw new Exception("❌ Database constants are not defined in Config/credentials.php.");
    }

    echo "<p>Attempting to connect to database: <strong>" . DB_NAME . "</strong> on <strong>" . DB_HOST . "</strong>...</p>";

    // Attempt the connection directly using our class
    $pdo = \App\Config\Database::getConnection();

    if ($pdo instanceof PDO) {
        echo "<p style='color:green'>✔ PDO Connection Successful!</p>";
    }

    echo "<hr><h2>✅ ALL TESTS PASSED!</h2><p>If you still see a 500 error on the API endpoints, the issue is likely with your web server (Nginx/Apache) configuration or the .htaccess file.</p>";

} catch (PDOException $e) {
    echo "<p style='color:red'><strong>❌ PDO Database Error:</strong> " . $e->getMessage() . "</p>";
    echo "<p>This means your credentials in Config/credentials.php are wrong, or the database user doesn't have permissions, or the MySQL server is offline.</p>";
} catch (Exception $e) {
    echo "<p style='color:red'><strong>❌ System Error:</strong> " . $e->getMessage() . "</p>";
} catch (Error $e) {
    echo "<p style='color:red'><strong>❌ Fatal PHP Error (Syntax/Typo):</strong> " . $e->getMessage() . "</p>";
    echo "<p>File: " . $e->getFile() . " on Line: " . $e->getLine() . "</p>";
}

echo "<hr><p><em>Delete this file (debug.php) immediately after you finish testing to secure your server.</em></p>";
