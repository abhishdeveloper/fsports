# Phase 16: Production Deployment & Launch Playbook

Welcome to the finish line! This playbook is your step-by-step manual for taking your secure, highly-gamified college sports prediction app from a local Parrot OS sandbox and deploying it to a production Ubuntu/Debian VPS under your custom domain (e.g., `predict.myaayucare.com`).

---

## Task 1: VPS Provisioning & Security Hardening

Connect to your fresh Ubuntu/Debian VPS via SSH as root or a user with sudo privileges.

### 1. Update and Install LEMP Stack
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install nginx mariadb-server php-fpm php-mysql php-xml php-curl php-mbstring openssl ufw zip unzip -y
```

### 2. Secure the Server (UFW Firewall)
Close off all ports except SSH (22), HTTP (80), and HTTPS (443).
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw enable
```

### 3. Crucial PHP Production Settings
Locate your `php.ini` file (typically `/etc/php/8.2/fpm/php.ini` or similar depending on your PHP version).
```bash
sudo nano /etc/php/8.x/fpm/php.ini
```
Find and update the following values to ensure raw stack traces are never exposed to attackers:
```ini
display_errors = Off
display_startup_errors = Off
log_errors = On
error_log = /var/log/php_errors.log
expose_php = Off
```
Restart PHP-FPM:
```bash
sudo systemctl restart php8.x-fpm
```

---

## Task 2: Domain Setup & SSL Certificate

Assuming you have pointed the A Record of `predict.myaayucare.com` (or `play.abhish.in`) to your VPS IP address in your DNS provider (e.g., Cloudflare, Namecheap).

### 1. Nginx Server Block Configuration
Create the config file:
```bash
sudo nano /etc/nginx/sites-available/predict.myaayucare.com
```
Add the following template:
```nginx
server {
    listen 80;
    server_name predict.myaayucare.com;
    root /var/www/sports-app/public; # Assuming you place your index.php here or root
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    # Crucial Security: Block direct access to hidden files like .env and .git
    location ~ /\. {
        deny all;
    }

    # PHP-FPM Processing
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.x-fpm.sock; # Adjust PHP version
    }
}
```
Enable the site and reload Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/predict.myaayucare.com /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 2. Install Let's Encrypt SSL (Certbot)
Retrofit explicitly requires HTTPS with a valid certificate.
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d predict.myaayucare.com
```
Follow the prompts and select **Redirect** to force all HTTP traffic to HTTPS.

---

## Task 3: Database & File Migration

### 1. Export Local Database (From Parrot OS)
Run this command on your local machine:
```bash
mysqldump -u root -p college_sports > college_sports_backup.sql
```

### 2. Import to Live VPS
Secure the live MySQL installation first:
```bash
sudo mysql_secure_installation
```
Log into MySQL on the VPS and create the database/user:
```sql
CREATE DATABASE college_sports CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'prod_user'@'localhost' IDENTIFIED BY 'STRONG_RANDOM_PASSWORD';
GRANT ALL PRIVILEGES ON college_sports.* TO 'prod_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
Transfer the SQL file to your VPS:
```bash
scp college_sports_backup.sql user@your_vps_ip:~
```
Import it on the VPS:
```bash
mysql -u prod_user -p college_sports < college_sports_backup.sql
```

### 3. Securely Upload PHP Files
Zip your local project directory (excluding `vendor/` and `.git/`). Transfer it via SCP to the VPS.
```bash
scp project.zip user@your_vps_ip:~
```
On the VPS, extract it to `/var/www/sports-app/` and set proper permissions:
```bash
sudo unzip project.zip -d /var/www/sports-app/
sudo chown -R www-data:www-data /var/www/sports-app/
sudo chmod -R 755 /var/www/sports-app/
```
Install dependencies on the VPS (ensure Composer is installed):
```bash
cd /var/www/sports-app/
composer install --no-dev --optimize-autoloader
```
**CRITICAL:** Create your production `.env` file on the VPS and fill in the live database credentials and strong, unique JWT secrets.

---

## Task 4: Android Production Build Checklist (Kotlin)

Before generating the Release APK for your friends to install, you must finalize the Android client.

### 1. Update Retrofit BASE_URL
In `ApiClient.kt`, change the local IP to your new live domain:
```kotlin
private const val BASE_URL = "https://predict.myaayucare.com/"
```

### 2. Extract & Update the SHA-256 SSL Pin
You must pin the newly generated Let's Encrypt certificate to prevent MITM attacks. Run this command on your local terminal to fetch the live hash:
```bash
openssl s_client -servername predict.myaayucare.com -connect predict.myaayucare.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
```
Copy the output hash and replace the dummy hash in `ApiClient.kt`:
```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("predict.myaayucare.com", "sha256/YOUR_NEW_BASE64_HASH_HERE=")
    .build()
```

### 3. Remove Logging Interceptors
In `ApiClient.kt`, comment out or remove the `HttpLoggingInterceptor` to ensure sensitive network payloads (like tokens or answers) are not printed to the Android system Logcat in the release build.

### 4. Enable R8 / ProGuard (Code Obfuscation)
In your app-level `build.gradle.kts`, ensure minification is enabled for the release type. This shrinks the APK size and aggressively obfuscates the Kotlin logic to deter reverse engineering.
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```
*(Note: You may need to add specific `@Keep` annotations or ProGuard rules for Gson data models like `MatchData` to ensure they aren't stripped during compilation).*

### 5. Generate Signed Release APK / AAB
1. In Android Studio, go to **Build > Generate Signed Bundle / APK**.
2. Select **APK** (if distributing directly) or **Android App Bundle** (if uploading to Google Play).
3. Create a new Keystore (e.g., `sports_keystore.jks`) and store the password in a highly secure password manager. **Do not lose this file.**
4. Select the `release` build variant and finish.

🚀 **Congratulations! Your highly secure, gamified Campus Sports Prediction app is now live!**