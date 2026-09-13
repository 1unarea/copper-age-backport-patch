#!/usr/bin/env python3
"""
Extracts the changelog section for a specific version from CHANGELOG.md.
Prints the changelog to stdout. Falls back to a generic message if not found.

Usage: extract-changelog.py <version>
  e.g. extract-changelog.py 0.1.2
"""
import re
import sys

VERSION = sys.argv[1] if len(sys.argv) > 1 else ""

changelog_path = "CHANGELOG.md"
try:
    with open(changelog_path, "r", encoding="utf-8") as f:
        content = f.read()
except FileNotFoundError:
    print(f"Copper Age Backport Patch v{VERSION}")
    sys.exit(0)

# Match ## [0.1.2] or ## 0.1.2 section
pattern = re.compile(
    r"^##\s+\[?" + re.escape(VERSION) + r"\]?[^\n]*\n(.*?)(?=^##\s|\Z)",
    re.MULTILINE | re.DOTALL,
)
match = pattern.search(content)
if match:
    print(match.group(1).strip())
else:
    print(f"Copper Age Backport Patch v{VERSION}")
