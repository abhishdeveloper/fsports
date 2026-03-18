<?php

declare(strict_types=1);

namespace App\Api\Utils;

use Google\Auth\Credentials\ServiceAccountCredentials;
use GuzzleHttp\Client;
use Exception;

class FirebaseHelper
{
    private const PROJECT_ID = 'your-firebase-project-id'; // Must be replaced with real ID
    private const CREDENTIALS_PATH = __DIR__ . '/../../config/firebase_credentials.json';
    private const FCM_API_URL = 'https://fcm.googleapis.com/v1/projects/%s/messages:send';

    /**
     * Gets a valid OAuth2 Access Token using the Service Account JSON file.
     *
     * @return string The OAuth2 Access Token
     * @throws Exception If authentication fails
     */
    private static function getAccessToken(): string
    {
        if (!file_exists(self::CREDENTIALS_PATH)) {
            throw new Exception("Firebase credentials file not found at " . self::CREDENTIALS_PATH);
        }

        $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
        $credentials = new ServiceAccountCredentials($scopes, self::CREDENTIALS_PATH);

        $tokenArray = $credentials->fetchAuthToken();

        if (isset($tokenArray['access_token'])) {
            return $tokenArray['access_token'];
        }

        throw new Exception("Failed to fetch Firebase OAuth2 access token.");
    }

    /**
     * Internal method to send the HTTP v1 payload to FCM.
     *
     * @param array $messagePayload The structured 'message' object for FCM
     * @return bool True if successful, false otherwise
     */
    private static function send(array $messagePayload): bool
    {
        try {
            $accessToken = self::getAccessToken();
            $url = sprintf(self::FCM_API_URL, self::PROJECT_ID);

            $client = new Client();
            $response = $client->post($url, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $accessToken,
                    'Content-Type' => 'application/json',
                ],
                'json' => ['message' => $messagePayload]
            ]);

            return $response->getStatusCode() === 200;
        } catch (Exception $e) {
            error_log("Firebase Send Error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Sends a push notification to a globally subscribed topic (e.g., 'all_users').
     *
     * @param string $topic The topic name (without /topics/ prefix)
     * @param string $title Notification title
     * @param string $body Notification body
     * @return bool
     */
    public static function sendToTopic(string $topic, string $title, string $body): bool
    {
        $payload = [
            'topic' => $topic,
            'notification' => [
                'title' => $title,
                'body' => $body
            ],
            // Optional: Android specific configurations
            'android' => [
                'priority' => 'high',
                'notification' => [
                    'sound' => 'default'
                ]
            ]
        ];

        return self::send($payload);
    }

    /**
     * Sends a push notification directly to a single device using its FCM Token.
     *
     * @param string $fcmToken The user's device token
     * @param string $title Notification title
     * @param string $body Notification body
     * @return bool
     */
    public static function sendToUser(string $fcmToken, string $title, string $body): bool
    {
        if (empty($fcmToken)) {
            return false;
        }

        $payload = [
            'token' => $fcmToken,
            'notification' => [
                'title' => $title,
                'body' => $body
            ],
            'android' => [
                'priority' => 'high',
                'notification' => [
                    'sound' => 'default'
                ]
            ]
        ];

        return self::send($payload);
    }
}