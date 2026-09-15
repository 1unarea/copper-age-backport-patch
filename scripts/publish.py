#!/usr/bin/env python3
"""
Multi-platform publishing script for Copper Age Backport Patch.
Supports GitHub Releases, Modrinth, and CurseForge.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import requests

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def read_gradle_properties():
    props = {}
    prop_file = os.path.expanduser("~/.gradle/gradle.properties")
    if os.path.isfile(prop_file):
        with open(prop_file, "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    props[k.strip()] = v.strip()
    return props

PROPS = read_gradle_properties()

CURSEFORGE_TOKEN = os.getenv("CURSEFORGE_TOKEN") or PROPS.get("curseforgeToken", "")
CURSEFORGE_PROJECT_ID = os.getenv("CURSEFORGE_PROJECT_ID") or PROPS.get("curseforgeProjectId", "1696121")

MODRINTH_TOKEN = os.getenv("MODRINTH_TOKEN") or PROPS.get("modrinthToken", "")
MODRINTH_PROJECT_ID = os.getenv("MODRINTH_PROJECT_ID") or PROPS.get("modrinthProjectId", "4Gz003Sy")

def get_mod_version():
    build_gradle = os.path.join(ROOT_DIR, "build.gradle")
    with open(build_gradle, "r") as f:
        content = f.read()
    m = re.search(r"version\s*=\s*['\"]([^'\"]+)['\"]", content)
    if m:
        return m.group(1)
    return "1.0.0"

def get_changelog(version):
    changelog_file = os.path.join(ROOT_DIR, "CHANGELOG.md")
    if os.path.isfile(changelog_file):
        with open(changelog_file, "r") as f:
            text = f.read()
        pattern = rf"##\s+\[?{re.escape(version)}\]?[^\n]*\n(.*?)(?=^##\s|\Z)"
        m = re.search(pattern, text, re.DOTALL | re.MULTILINE)
        if m:
            cl = m.group(1).strip()
            if cl.endswith("---"):
                cl = cl[:-3].strip()
            return cl
    return f"Release v{version}"

def publish_github(version, changelog):
    print("\n--- Publishing to GitHub ---")
    neoforge_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-neoforge-1.21.1-{version}.jar")
    fabric_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-fabric-1.21.1-{version}.jar")

    if not os.path.exists(neoforge_jar) or not os.path.exists(fabric_jar):
        print("Error: Build jars not found. Run './gradlew assemble' first.")
        return False

    tag_name = f"v{version}"
    cmd = [
        "gh", "release", "create", tag_name,
        neoforge_jar, fabric_jar,
        "--title", tag_name,
        "--notes", changelog,
        "--latest"
    ]
    res = subprocess.run(cmd, cwd=ROOT_DIR)
    if res.returncode == 0:
        print(f"Successfully published GitHub release {tag_name}")
        return True
    else:
        print("GitHub release failed or already exists.")
        return False

def publish_modrinth(version, changelog):
    print("\n--- Publishing to Modrinth ---")
    if not MODRINTH_TOKEN:
        print("Error: Modrinth token not found. Set MODRINTH_TOKEN or modrinthToken in ~/.gradle/gradle.properties")
        return False

    base_url = "https://api.modrinth.com/v2"
    headers = {
        "Authorization": MODRINTH_TOKEN,
        "User-Agent": f"1unarea/copper-age-backport-patch/{version}"
    }

    neoforge_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-neoforge-1.21.1-{version}.jar")
    fabric_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-fabric-1.21.1-{version}.jar")

    # NeoForge uploaded first, then Fabric
    targets = [
        {
            "name": f"v{version} (NeoForge)",
            "version_number": f"{version}-neoforge",
            "loaders": ["neoforge"],
            "jar": neoforge_jar,
            "deps": [
                {"project_id": "a1llHwl4", "dependency_type": "required"},  # Copper Age Backport
                {"project_id": "uXeEiQk1", "dependency_type": "optional"},  # Tool Trims
                {"project_id": "6xwxDTgf", "dependency_type": "optional"},  # Vanilla Backport
                {"project_id": "nvQzSEkH", "dependency_type": "optional"},  # Jade
                {"project_id": "LNytGWDc", "dependency_type": "optional"},  # Create
                {"project_id": "5sy6g3kz", "dependency_type": "optional"},  # Better Combat
            ]
        },
        {
            "name": f"v{version} (Fabric)",
            "version_number": f"{version}-fabric",
            "loaders": ["fabric"],
            "jar": fabric_jar,
            "deps": [
                {"project_id": "a1llHwl4", "dependency_type": "required"},  # Copper Age Backport
                {"project_id": "P7dR8mSH", "dependency_type": "required"},  # Fabric API
                {"project_id": "uXeEiQk1", "dependency_type": "optional"},  # Tool Trims
                {"project_id": "6xwxDTgf", "dependency_type": "optional"},  # Vanilla Backport
                {"project_id": "nvQzSEkH", "dependency_type": "optional"},  # Jade
                {"project_id": "LNytGWDc", "dependency_type": "optional"},  # Create
                {"project_id": "5sy6g3kz", "dependency_type": "optional"},  # Better Combat
            ]
        }
    ]

    success = True
    for t in targets:
        print(f"Uploading {t['name']}...")
        metadata = {
            "name": t["name"],
            "version_number": t["version_number"],
            "changelog": changelog,
            "dependencies": t["deps"],
            "game_versions": ["1.21.1"],
            "version_type": "release",
            "loaders": t["loaders"],
            "featured": True,
            "status": "listed",
            "requested_status": "listed",
            "project_id": MODRINTH_PROJECT_ID,
            "file_parts": ["file_0"],
            "primary_file": "file_0"
        }
        filename = os.path.basename(t["jar"])
        with open(t["jar"], "rb") as f:
            files = {
                "data": (None, json.dumps(metadata), "application/json"),
                "file_0": (filename, f, "application/java-archive")
            }
            resp = requests.post(f"{base_url}/version", headers=headers, files=files)
            if resp.status_code in (200, 201):
                print(f"Successfully uploaded {t['name']} (ID: {resp.json().get('id')})")
            else:
                print(f"Failed to upload {t['name']}: {resp.status_code} - {resp.text}")
                success = False

    return success

def publish_curseforge(version, changelog):
    print("\n--- Publishing to CurseForge ---")
    if not CURSEFORGE_TOKEN:
        print("Error: CurseForge token not found. Set CURSEFORGE_TOKEN or curseforgeToken in ~/.gradle/gradle.properties")
        return False

    api_url = f"https://minecraft.curseforge.com/api/projects/{CURSEFORGE_PROJECT_ID}/upload-file"
    headers = {
        "X-Api-Token": CURSEFORGE_TOKEN
    }

    # CurseForge game version IDs:
    # 11779: Minecraft 1.21.1
    # 7499: Fabric
    # 10150: NeoForge
    # 11135: Java 21
    # 9638: Client
    # 9639: Server
    common_versions = [11779, 11135, 9638, 9639]

    neoforge_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-neoforge-1.21.1-{version}.jar")
    fabric_jar = os.path.join(ROOT_DIR, f"build/libs/copper_age_patch-fabric-1.21.1-{version}.jar")

    # NeoForge uploaded first, then Fabric
    targets = [
        {
            "name": f"v{version} (NeoForge)",
            "versions": common_versions + [10150],
            "jar": neoforge_jar
        },
        {
            "name": f"v{version} (Fabric)",
            "versions": common_versions + [7499],
            "jar": fabric_jar
        }
    ]

    success = True
    for t in targets:
        print(f"Uploading {t['name']} to CurseForge...")
        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": t["name"],
            "gameVersions": t["versions"],
            "releaseType": "release"
        }
        filename = os.path.basename(t["jar"])
        with open(t["jar"], "rb") as f:
            files = {
                "file": (filename, f, "application/java-archive"),
                "metadata": (None, json.dumps(metadata), "application/json")
            }
            resp = requests.post(api_url, headers=headers, files=files)
            if resp.status_code == 200:
                print(f"Successfully uploaded {t['name']} (ID: {resp.json().get('id')})")
            else:
                print(f"Failed to upload {t['name']}: {resp.status_code} - {resp.text}")
                success = False

    return success

def main():
    parser = argparse.ArgumentParser(description="Publish Copper Age Backport Patch")
    parser.add_argument("--github", action="store_true", help="Publish to GitHub Releases")
    parser.add_argument("--modrinth", action="store_true", help="Publish to Modrinth")
    parser.add_argument("--curseforge", action="store_true", help="Publish to CurseForge")
    parser.add_argument("--all", action="store_true", help="Publish to all platforms")

    args = parser.parse_args()

    if not (args.github or args.modrinth or args.curseforge or args.all):
        parser.print_help()
        sys.exit(1)

    version = get_mod_version()
    changelog = get_changelog(version)
    print(f"Target version: {version}")

    if args.all or args.github:
        publish_github(version, changelog)

    if args.all or args.modrinth:
        publish_modrinth(version, changelog)

    if args.all or args.curseforge:
        publish_curseforge(version, changelog)

if __name__ == "__main__":
    main()
