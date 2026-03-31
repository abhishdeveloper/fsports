<?php

declare(strict_types=1);

// Error Handling configuration
ini_set('display_errors', '0'); // Do not display raw errors to clients
error_reporting(E_ALL);         // Log everything to server log

// Load the secure static configuration
$credentialsPath = __DIR__ . '/Config/credentials.php';
if (!file_exists($credentialsPath)) {
    error_log("Missing Config/credentials.php file. Check documentation.");
    header('HTTP/1.1 500 Internal Server Error');
    exit('A critical configuration error occurred.');
}
require_once $credentialsPath;

/**
 * Custom Zero-Dependency PSR-4 Autoloader
 * Replaces Composer's vendor/autoload.php for Shared Hosting compatibility.
 * Automatically loads any class in the 'App\' namespace based on its directory path.
 */
spl_autoload_register(function ($class) {
    // Project-specific namespace prefix
    $prefix = 'App\\';

    // Base directory for the namespace prefix
    $base_dir = __DIR__ . '/';

    // Does the class use the namespace prefix?
    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) {
        return; // No, move to the next registered autoloader
    }

    // Get the relative class name
    $relative_class = substr($class, $len);

    // Replace the namespace prefix with the base directory, replace namespace
    // separators with directory separators, append with .php
    $file = $base_dir . str_replace('\\', '/', $relative_class) . '.php';

    // If the file exists, require it
    if (file_exists($file)) {
        require $file;
    }
});
