"""Check an isolated demo stack, including persistence across container recreation.

Run in CI with DEMO_ENABLED=true and the matching DEMO_PASSWORD in the environment.
The script creates a bug and recreates the project's app and db containers.
"""

import http.cookiejar
import json
import os
import subprocess
import urllib.request


def check(condition, message):
    if not condition:
        raise AssertionError(message)


base_url = "http://127.0.0.1:" + os.environ.get("APP_PORT", "8080")
cookies = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))


def request(path, data=None, csrf=None):
    headers = {"Origin": base_url}
    if data is not None:
        headers["Content-Type"] = "application/json"
    if csrf:
        headers["X-CSRF-Token"] = csrf
    req = urllib.request.Request(
        base_url + path,
        data=None if data is None else json.dumps(data).encode(),
        headers=headers,
    )
    with client.open(req, timeout=10) as response:
        body = response.read()
        content_type = response.headers.get("Content-Type", "")
        return response.status, json.loads(body) if "application/json" in content_type else body


status, html = request("/")
check(status == 200 and b"BugTrack" in html, "Web client is unavailable")
status, user = request("/api/auth/login", {
    "email": "zakhar@example.org",
    "password": os.environ["DEMO_PASSWORD"],
})
check(status == 200 and user["email"] == "zakhar@example.org", "Demo login failed")
status, projects = request("/api/projects")
check(status == 200, "Projects are unavailable")
shop = next(project for project in projects if project["key"] == "SHOP")
status, bug = request("/api/projects/" + shop["id"] + "/bugs", {
    "title": "Docker persistence check",
    "description": "Created by the Docker smoke check",
    "steps": "Create a bug and recreate both containers",
    "expected": "The bug and session survive",
    "actual": "Verifying persistence",
    "environment": "GitHub Actions Docker Compose",
    "severity": "MINOR",
    "priority": "LOW",
}, user["csrfToken"])
check(status == 201 and bug["status"] == "NEW", "Bug creation failed")
bug_id = bug["id"]
status, persisted_bug = request("/api/bugs/" + bug_id)
check(status == 200, "Created bug cannot be read from the database")
check(all(persisted_bug[key] == value for key, value in bug.items() if key != "createdAt"),
      "Persisted bug fields differ from the creation response")
status, history = request("/api/bugs/" + bug_id + "/history")
check(status == 200 and any(item["action"] == "CREATED" for item in history), "Creation history is missing")

subprocess.run([
    "docker", "compose", "up", "-d", "--force-recreate", "--wait",
    "--wait-timeout", "120",
], check=True)

status, user_after = request("/api/auth/me")
check(status == 200 and user_after["id"] == user["id"], "Session was lost")
status, bug_after = request("/api/bugs/" + bug_id)
check(status == 200 and bug_after == persisted_bug, "Persisted bug was lost or changed")
status, history_after = request("/api/bugs/" + bug_id + "/history")
check(status == 200 and history_after == history, "History changed after recreation")
print("Docker smoke check passed: web client, login, creation, history, persistent data and session")
