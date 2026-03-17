# Security Auditing & Local Deployment Playbook

This playbook serves as a comprehensive guide to setting up a local LEMP environment for testing, verifying SSL pinning against MITM attacks, executing automated security audits on the PHP backends, and finalizing the pre-flight checklist for VPS deployment.

---

## Task 1: Local Server Sandbox Setup (LEMP on Debian/Parrot OS)

Execute these terminal commands to spin up a secure, local high-performance Nginx web server, PHP processor, and MariaDB database.

### 1. Install Dependencies
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install nginx mariadb-server php-fpm php-mysql php-xml php-curl php-mbstring openssl -y
```

### 2. Configure Local Database
Secure the installation and create the test database.
```bash
sudo mysql_secure_installation # Follow prompts to set root password and remove test dbs
sudo mysql -u root -p
```
Inside the MariaDB shell:
```sql
CREATE DATABASE college_sports CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'devuser'@'localhost' IDENTIFIED BY 'secretpassword';
GRANT ALL PRIVILEGES ON college_sports.* TO 'devuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
*Note: Run your `database/schema.sql` against this database.*

### 3. Generate a Self-Signed SSL Certificate
To test HTTPS locally, generate a self-signed certificate valid for 365 days.
```bash
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/localhost.key \
    -out /etc/nginx/ssl/localhost.crt \
    -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
```

### 4. Configure Nginx Server Block
Create a new server block configuration.
```bash
sudo nano /etc/nginx/sites-available/college-sports.conf
```
Add the following configuration, pointing `root` to your project directory (e.g., `/var/www/college-sports-app`):
```nginx
server {
    listen 80;
    server_name localhost;
    return 301 https://$host$request_uri; # Force HTTPS
}

server {
    listen 443 ssl http2;
    server_name localhost;

    ssl_certificate /etc/nginx/ssl/localhost.crt;
    ssl_certificate_key /etc/nginx/ssl/localhost.key;

    # Modern SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    root /var/www/college-sports-app; # IMPORTANT: Update this path
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    # Hide dot files (e.g., .env, .git)
    location ~ /\. {
        deny all;
    }

    # Pass PHP scripts to FastCGI server
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.2-fpm.sock; # Ensure PHP version matches your install
    }
}
```
Enable the site and restart Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/college-sports.conf /etc/nginx/sites-enabled/
sudo nginx -t # Test configuration
sudo systemctl restart nginx
```

---

## Task 2: Android SSL Pinning Verification (MITM Test)

This section details how to verify that OkHttp's `CertificatePinner` is forcefully rejecting unauthorized interception proxies like Burp Suite.

### 1. Proxy Configuration
1. Open **Burp Suite** and navigate to `Proxy > Options`. Note the listener IP and port (e.g., `127.0.0.1:8080`).
2. Export the Burp CA Certificate (`Export CA certificate > Certificate in DER format`) and rename it to `cacert.cer`.

### 2. Emulator Configuration
1. Launch an Android Emulator (preferably a non-Google Play image so you have root access).
2. Go to Android Settings > Network & Internet > Wi-Fi > Select the active network > Pencil icon > Advanced options.
3. Set **Proxy** to `Manual`, enter your computer's local IP (e.g., `192.168.1.5`) and the Burp port (`8080`).
4. Install the Burp Certificate on the emulator: Drag and drop `cacert.cer` onto the emulator, or go to Settings > Security > Encryption & credentials > Install a certificate > CA certificate.

### 3. Verification & Expected Behavior
1. Ensure the `CertificatePinner` in `ApiClient.kt` is currently configured with your *real* server's hash or a dummy hash, *not* Burp's hash.
2. Launch the College Sports app and attempt to log in or refresh the lobby.
3. **Expected Behavior (App):** The network call will instantly fail. `MatchViewModel` or `PredictionViewModel` should emit an Error state. Logcat will display a fatal `javax.net.ssl.SSLPeerUnverifiedException: Certificate pinning failure!`.
4. **Expected Behavior (Burp Suite):** Look at the HTTP History tab. You will *not* see the raw JSON requests. Instead, you will see a failed TLS handshake connection attempt. The traffic is completely unreadable. The fortress holds.

---

## Task 4: Production Pre-Flight Checklist

Before deploying this codebase to your live VPS, ensure the following configurations are set to prevent critical security leaks.

### 🔴 Critical Security Flags
- [ ] **Disable PHP Error Reporting:** In `bootstrap.php`, ensure `ini_set('display_errors', '0');` is active. Never expose stack traces or DB errors to clients.
- [ ] **Secure `.env` File:** Ensure your `.env` file is outside the public web root (if possible) or that Nginx strictly blocks access to it via the `location ~ /\. { deny all; }` directive.
- [ ] **Update JWT Secrets:** Replace `JWT_ACCESS_SECRET` and `JWT_REFRESH_SECRET` in `.env` with strong, cryptographically random 64+ character strings.
- [ ] **Restrict CORS:** Update `ALLOWED_ORIGIN` in `.env` from `*` to your actual app's custom schema or restrict it entirely if only the native app communicates with the API.
- [ ] **Update API Base URL:** Change the `BASE_URL` in Android's `ApiClient.kt` from localhost to your production domain (e.g., `https://api.collegesports.com/`).
- [ ] **Inject Real SSL Hash:** Update the `CertificatePinner` in `ApiClient.kt` with the actual SHA-256 hash of your production domain's SSL certificate.

### 🟡 Performance & Optimization
- [ ] **Composer Autoloader:** Run `composer dump-autoload -o --no-dev` on the production server to optimize class loading.
- [ ] **Remove Logging Interceptors:** In `ApiClient.kt`, remove or disable the `HttpLoggingInterceptor` to ensure no sensitive data is printed to the device's Logcat in release builds.
- [ ] **ProGuard / R8:** Ensure `minifyEnabled true` is set in the Android `build.gradle.kts` to obfuscate the Kotlin code.