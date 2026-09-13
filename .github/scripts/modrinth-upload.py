#!/usr/bin/env python3
"""
Uploads a mod jar to Modrinth via REST API.
Used by GitHub Actions release workflow.
"""
import argparse
import json
import os
import sys
import urllib.request
import urllib.error


def upload(token, project_id, version, loader, jar_path, changelog_path):
    with open(changelog_path, "r", encoding="utf-8") as f:
        changelog = f.read().strip()

    with open(jar_path, "rb") as f:
        jar_data = f.read()

    jar_filename = os.path.basename(jar_path)

    metadata = {
        "name": f"v{version} ({loader.capitalize()})",
        "version_number": f"{version}-{loader}",
        "changelog": changelog,
        "dependencies": [
            {
                "project_id": "a1llHwl4",   # Copper Age Backport
                "dependency_type": "required"
            }
        ],
        "game_versions": ["1.21.1"],
        "version_type": "release",
        "loaders": [loader],
        "featured": True,
        "project_id": project_id,
        "file_parts": ["file"],
        "primary_file": "file",
    }

    if loader == "fabric":
        metadata["dependencies"].append({
            "project_id": "P7dR8mSH",   # Fabric API
            "dependency_type": "required"
        })

    boundary = "----FormBoundary" + os.urandom(8).hex()
    body_parts = []

    # Part 1: metadata JSON
    meta_json = json.dumps(metadata).encode("utf-8")
    body_parts.append(
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="data"\r\n'
        f"Content-Type: application/json\r\n\r\n".encode("utf-8")
        + meta_json + b"\r\n"
    )

    # Part 2: file
    body_parts.append(
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{jar_filename}"\r\n'
        f"Content-Type: application/java-archive\r\n\r\n".encode("utf-8")
        + jar_data + b"\r\n"
    )

    body_parts.append(f"--{boundary}--\r\n".encode("utf-8"))
    body = b"".join(body_parts)

    req = urllib.request.Request(
        "https://api.modrinth.com/v2/version",
        data=body,
        headers={
            "Authorization": token,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "User-Agent": "copper-age-backport-patch/release-bot",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read())
            print(f"[OK] Uploaded {loader} v{version} to Modrinth.")
            print(f"     Version ID: {result.get('id')}")
            print(f"     URL: https://modrinth.com/mod/{project_id}/version/{result.get('id')}")
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8", errors="replace")
        print(f"[ERROR] HTTP {e.code} uploading {loader} to Modrinth: {error_body}", file=sys.stderr)
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Upload mod to Modrinth")
    parser.add_argument("--token",      required=True, help="Modrinth API token")
    parser.add_argument("--project-id", required=True, help="Modrinth project ID")
    parser.add_argument("--version",    required=True, help="Version number (e.g. 0.1.2)")
    parser.add_argument("--loader",     required=True, choices=["neoforge", "fabric"])
    parser.add_argument("--jar",        required=True, help="Path to jar file")
    parser.add_argument("--changelog",  required=True, help="Path to changelog markdown file")
    args = parser.parse_args()

    upload(
        token=args.token,
        project_id=args.project_id,
        version=args.version,
        loader=args.loader,
        jar_path=args.jar,
        changelog_path=args.changelog,
    )


if __name__ == "__main__":
    main()
