const API_BASE_URL = ''; // Empty string natively resolves to the current domain in a Shared Hosting environment.

/**
 * A centralized wrapper around the native Fetch API to securely handle
 * sending HttpOnly cookies (credentials) and parse JSON responses automatically.
 */
class Api {

    /**
     * Executes a network request securely.
     *
     * @param {string} endpoint - The relative API path (e.g. '/Api/Auth/login.php')
     * @param {string} method - 'GET' or 'POST'
     * @param {object} body - The JSON payload (optional)
     * @returns {Promise<object>} The parsed JSON response
     */
    static async request(endpoint, method = 'GET', body = null) {
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        };

        const config = {
            method,
            headers,
            credentials: 'include' // CRITICAL: This sends the HttpOnly cookies securely
        };

        if (body) {
            config.body = JSON.stringify(body);
        }

        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
            const data = await response.json();

            // Global 401 Unauthorized Interceptor
            if (response.status === 401) {
                console.warn("Session expired or unauthorized. Redirecting to login...");
                // Clear any non-sensitive local state if needed
                localStorage.removeItem('user_role');
                window.location.href = 'login.html';
                return Promise.reject("Unauthorized");
            }

            if (!response.ok) {
                // If the backend threw a failsafe 500 error containing 'details' (like a database configuration typo)
                // append it so the user can debug their shared hosting setup visually.
                let errorMsg = data.error || 'An unexpected error occurred.';
                if (data.details) {
                    errorMsg += `: ${data.details}`;
                }
                return Promise.reject(errorMsg);
            }

            return data;
        } catch (error) {
            console.error(`API Error on ${endpoint}:`, error);
            // If the network completely fails (e.g. server down or .htaccess block), catch it here
            throw error instanceof TypeError ? "Failed to connect to the server. Check your connection or .htaccess configuration." : error;
        }
    }

    static get(endpoint) {
        return this.request(endpoint, 'GET');
    }

    static post(endpoint, body) {
        return this.request(endpoint, 'POST', body);
    }

    static async logout() {
        try {
            await this.post('/Api/Auth/logout.php', {});
        } catch (error) {
            console.error('Logout API failed:', error);
        } finally {
            localStorage.removeItem('user_role');
            window.location.href = 'login.html';
        }
    }
}