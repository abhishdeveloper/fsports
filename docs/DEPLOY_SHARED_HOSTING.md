# 🚀 Deploying Your Sports App via Shared Hosting & FileZilla

This guide contains the exact steps required to take this repository and safely deploy it onto a standard Shared Web Hosting provider (like HostGator, GoDaddy, Hostinger, Bluehost) using cPanel, phpMyAdmin, and FileZilla.

---

## Task 1: Prepare the Local Files (Zero-Dependency)

This project has been completely refactored to require **zero external dependencies**. There is no need to run Composer or configure `.env` files.

1. **Download the Repository:** Download this codebase as a ZIP file to your local computer and extract it.
2. **Configure the Credentials:**
    * Navigate to the `Config/` directory.
    * Rename `credentials.example.php` to `credentials.php`.
    * Open `credentials.php` in any text editor.
    * Update the `DB_HOST` (usually `localhost`), `DB_NAME`, `DB_USER`, and `DB_PASS` with the real credentials you will create in Step 2.
    * Set highly secure, long random strings for `JWT_ACCESS_SECRET` and `JWT_REFRESH_SECRET`.
    * Set `ALLOWED_ORIGIN` to your actual domain name (e.g., `https://yoursite.com`).
    * Save and close the file.

---

## Task 2: Configure the Live MySQL Database (cPanel)

1. Log into your hosting provider's **cPanel**.
2. Navigate to **MySQL® Databases**.
    * **Create New Database:** Name it something like `yourcpaneluser_collegesports`.
    * **Add New User:** Create a strong, random password. Note it down.
    * **Add User To Database:** Select the user you just created, select the database, and grant **ALL PRIVILEGES**.
3. Navigate back to cPanel home, open **phpMyAdmin**.
    * Click on your newly created database on the left sidebar.
    * Click the **Import** tab at the top.
    * Click **Choose File** and select the `database/schema.sql` file from your local folder.
    * Click **Go**. This will build the entire relational schema, including foreign keys and constraints.

*(Note: Your database is now empty. You must create an admin user or run the `seed_ipl_2026.php` script later to populate it).*

---

## Task 3: Upload Files via FileZilla (FTP)

1. Open **FileZilla** on your local machine.
2. Connect to your server using the FTP credentials provided by your host (Hostname, Username, Password, Port).
3. On the right side (Remote site), double-click to enter your `public_html` directory (or the root directory of your add-on domain).
4. On the left side (Local site), navigate to your prepared local project folder.
5. Select **ALL files and folders** in your project directory:
    * `Api/`
    * `Config/` (Make sure `credentials.php` is inside)
    * `css/`
    * `database/`
    * `docs/`
    * `js/`
    * `tests/`
    * `.htaccess` (CRITICAL: This secures your `Config` folder and databases from hackers)
    * `*.html` (login.html, lobby.html, index.html, etc.)
    * `bootstrap.php`
6. Drag and drop them into the `public_html` directory on the remote server. Wait for the upload to finish completely.

---

## Task 4: Verify Security & Execute Initial Seeding

Because you are on Shared Hosting, we cannot execute terminal commands. The provided `.htaccess` file acts as your firewall.

### 1. Verify Security
Open your web browser and attempt to access your sensitive files directly:
* `https://yoursite.com/.env`
* `https://yoursite.com/database/schema.sql`
* `https://yoursite.com/Config/Database.php`

**Expected Behavior:** All of these URLs MUST return a **403 Forbidden** error. If they download or display text, your `.htaccess` file was either not uploaded or is being ignored by your Apache server. Fix this immediately before proceeding.

### 2. Create the Admin User
Currently, your database is empty. You need an admin account to access `admin.html` and trigger the seeder.

1. Go back to **phpMyAdmin** in cPanel.
2. Select the `users` table and click **Insert**.
3. Fill in the fields:
    * `username`: admin
    * `email`: admin@college.edu
    * `roll_number`: ADMIN1
    * `branch`: Staff
    * `password_hash`: You MUST generate an Argon2id hash. You can write a temporary quick 2-line PHP script to output `password_hash('your_password', PASSWORD_ARGON2ID);` and copy the resulting string here.
    * `role`: **admin** (CRITICAL)
4. Click **Go**.

### 3. Run the Database Seeder
1. Open `https://yoursite.com/index.html` in your mobile or desktop browser.
2. Log in using the admin credentials you just created.
3. The lobby will be empty. Because you are an admin, click the purple gear FAB floating in the bottom right corner.
4. You are now in the **Command Center** (`admin.html`).
5. Click **Wipe & Seed IPL 2026**. Wait for the loading spinner.
6. When the success alert appears, click the back arrow to return to the Lobby.

🎉 **Congratulations! Your Mobile-Centric Web App is live, populated with test data, and secured against XSS and directory traversal attacks.**