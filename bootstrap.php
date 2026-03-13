<?php

declare(strict_types=1);

// Error Handling configuration
ini_set('display_errors', '0'); // Do not display raw errors to clients
error_reporting(E_ALL);         // Log everything to server log

$autoloadPath = __DIR__ . '/vendor/autoload.php';

if (!file_exists($autoloadPath)) {
    header('HTTP/1.1 500 Internal Server Error');
    exit('Autoloader not found. Please run composer install.');
}

require_once $autoloadPath;

// Load environment variables securely
$envPath = __DIR__;
if (file_exists($envPath . '/.env')) {
    try {
        $dotenv = Dotenv\Dotenv::createImmutable($envPath);
        $dotenv->load();

        // Validate required variables
        $dotenv->required(['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS']);
    } catch (Exception $e) {
        error_log("Dotenv Error: " . $e->getMessage());
        header('HTTP/1.1 500 Internal Server Error');
        exit('A configuration loading error occurred.');
    }
} else {
    // Fallback if environment variables are provided by the server environment directly
    if (!isset($_ENV['DB_NAME'])) {
        $_ENV['DB_HOST'] = getenv('DB_HOST');
        $_ENV['DB_PORT'] = getenv('DB_PORT');
        $_ENV['DB_NAME'] = getenv('DB_NAME');
        $_ENV['DB_USER'] = getenv('DB_USER');
        $_ENV['DB_PASS'] = getenv('DB_PASS');
        $_ENV['DB_CHARSET'] = getenv('DB_CHARSET');
        $_ENV['JWT_ACCESS_SECRET'] = getenv('JWT_ACCESS_SECRET');
        $_ENV['JWT_REFRESH_SECRET'] = getenv('JWT_REFRESH_SECRET');
        $_ENV['ALLOWED_ORIGIN'] = getenv('ALLOWED_ORIGIN');
    }
}
