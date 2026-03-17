#!/usr/bin/env python3

import requests
import jwt
import time
import json
import concurrent.futures
import threading

"""
Phase 9: Security Auditing and Local Deployment - Automated Stress Testing Script

This script performs three critical tests against our PHP API endpoints:
1. Token Tampering (Validating auth_middleware.php rejects forged signatures)
2. Expired Token (Validating auth_middleware.php rejects expired payloads)
3. Endpoint Brute-forcing (Validating PDO transactions and UNIQUE constraints hold up against concurrent race conditions)

Configuration:
Replace the TARGET_URL, REAL_SECRET, and VALID_USER_ID with your local setup.
"""

# --- Configuration ---
TARGET_URL = "http://localhost:8000"  # Update this to your local server's address
REAL_SECRET = "your_super_secret_access_key_here_that_is_long_enough_to_satisfy_pyjwt_requirements"  # Matches JWT_ACCESS_SECRET in your .env
VALID_USER_ID = 1  # Replace with a valid user ID from your local database

# For Test 3: The Brute-force payload
BRUTE_FORCE_ENDPOINT = f"{TARGET_URL}/Api/Predictions/submit_bulk.php"
# A sample question ID and option that the user HAS NOT answered yet
TEST_MATCH_ID = 1
TEST_QUESTION_ID = 1
TEST_OPTION = "A"

# Global counters for brute-force reporting
success_count = 0
conflict_count = 0
error_count = 0
lock = threading.Lock()

def generate_token(secret, user_id, expires_in=3600, role="user"):
    """Generates a valid JWT token."""
    now = int(time.time())
    payload = {
        "iss": "college-sports-app",
        "sub": user_id,
        "role": role,
        "iat": now,
        "exp": now + expires_in
    }
    return jwt.encode(payload, secret, algorithm="HS256")

def print_separator(title):
    print(f"\n{'='*20} {title} {'='*20}\n")

# --- Test 1: Token Tampering ---
def test_token_tampering():
    print_separator("TEST 1: Token Tampering")
    print("[*] Goal: Forge a JWT with the 'admin' role without knowing the secret key.")

    # We know the payload structure, so we create a fake one
    now = int(time.time())
    forged_payload = {
        "iss": "college-sports-app",
        "sub": VALID_USER_ID,
        "role": "admin", # Tampering attempt!
        "iat": now,
        "exp": now + 3600
    }

    # We sign it with a fake secret
    fake_secret = "hacked_secret_123"
    forged_token = jwt.encode(forged_payload, fake_secret, algorithm="HS256")

    headers = {"Authorization": f"Bearer {forged_token}"}

    print(f"[*] Sending request with forged token to {TARGET_URL}/Api/Matches/list.php...")
    try:
        response = requests.get(f"{TARGET_URL}/Api/Matches/list.php", headers=headers)

        print(f"[>] Response Status: {response.status_code}")
        print(f"[>] Response Body: {response.text}")

        if response.status_code == 401:
            print("[+] PASS: Server successfully rejected the tampered token.")
        else:
            print("[-] FAIL: Server accepted a tampered token! Check auth_middleware.php immediately.")
    except Exception as e:
        print(f"[!] Request failed: {e}")


# --- Test 2: Expired Token ---
def test_expired_token():
    print_separator("TEST 2: Expired Token Verification")
    print("[*] Goal: Ensure auth_middleware.php drops expired JWTs.")

    # Generate a token that expired 10 minutes ago
    expired_token = generate_token(REAL_SECRET, VALID_USER_ID, expires_in=-600)

    headers = {"Authorization": f"Bearer {expired_token}"}

    print(f"[*] Sending request with expired token to {TARGET_URL}/Api/Leaderboard/top.php...")
    try:
        response = requests.get(f"{TARGET_URL}/Api/Leaderboard/top.php", headers=headers)

        print(f"[>] Response Status: {response.status_code}")
        print(f"[>] Response Body: {response.text}")

        if response.status_code == 401:
             print("[+] PASS: Server successfully rejected the expired token.")
        else:
             print("[-] FAIL: Server accepted an expired token! Verify 'exp' claim validation in Firebase/JWT.")
    except Exception as e:
        print(f"[!] Request failed: {e}")


# --- Test 3: Endpoint Brute-forcing (Race Conditions) ---
def make_prediction_request(token, payload):
    """Worker function to hit the submit_bulk endpoint."""
    global success_count, conflict_count, error_count

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(BRUTE_FORCE_ENDPOINT, headers=headers, json=payload)

        with lock:
            if response.status_code == 201:
                success_count += 1
            elif response.status_code == 409:
                conflict_count += 1
            else:
                error_count += 1
                print(f"[!] Unexpected Status {response.status_code}: {response.text}")

    except Exception as e:
        with lock:
            error_count += 1
            print(f"[!] Thread Error: {e}")

def test_endpoint_bruteforce():
    print_separator("TEST 3: Endpoint Brute-forcing & Transaction Integrity")
    print(f"[*] Goal: Fire 100 concurrent requests to {BRUTE_FORCE_ENDPOINT}.")
    print("[*] Expected behavior: Exactly ONE request should succeed (201). The other 99 should hit the PDO UNIQUE constraint and cleanly return 409 Conflict.")

    # Generate a valid token
    valid_token = generate_token(REAL_SECRET, VALID_USER_ID)

    # Prepare the bulk submission payload
    payload = [
        {
            "question_id": TEST_QUESTION_ID,
            "selected_option": TEST_OPTION
        }
    ]

    num_requests = 100
    print(f"[*] Launching {num_requests} threads...")

    start_time = time.time()

    # Use ThreadPoolExecutor to fire requests concurrently
    with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(make_prediction_request, valid_token, payload) for _ in range(num_requests)]
        concurrent.futures.wait(futures)

    end_time = time.time()

    print(f"\n[*] Stress test completed in {end_time - start_time:.2f} seconds.")
    print(f"[>] Total 201 Successes (Inserted): {success_count}")
    print(f"[>] Total 409 Conflicts (Rejected): {conflict_count}")
    print(f"[>] Total Errors / Other Statuses:  {error_count}")

    if success_count == 1 and conflict_count == (num_requests - 1) and error_count == 0:
        print("\n[+] PASS: Database integrity held! PDO Transactions and UNIQUE constraints successfully mitigated the race condition.")
    elif success_count == 0 and conflict_count == num_requests:
         print("\n[~] NOTE: 0 Successes. It appears you have already submitted this prediction in the database prior to running the test. All constraints held.")
    else:
        print("\n[-] FAIL: Database integrity compromised or server crashed under load. Investigate deadlocks or missing UNIQUE constraints.")

if __name__ == "__main__":
    print("Starting DevSecOps Automated Security Audit...")
    test_token_tampering()
    test_expired_token()
    test_endpoint_bruteforce()
    print("\nAudit Complete.")