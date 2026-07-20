#!/usr/bin/env python3
"""
Validate exercises.json against exercises.schema.json and enforce cross-platform
parity with the sibling repo (ChaosBank-iOS <-> ChaosBank-Android).

Usage:
    check_exercises.py <exercises.json> <OWN_PREFIX> [<sibling_raw_url>]

- Structural / schema validation is a HARD gate (uses `jsonschema` if installed,
  otherwise an equivalent stdlib check).
- Cross-platform parity compares the SET of defect names (identical on both
  platforms; only the id prefix differs). If the sibling can't be fetched, the
  parity step is skipped (never fails CI on a network hiccup).
"""
import json
import sys
import urllib.request

CATEGORIES = {"money", "validation", "localization", "state", "concurrency",
              "ui", "accessibility", "security", "network", "performance"}
DIFFICULTIES = {"junior", "middle", "senior"}
REQUIRED = {"id", "title", "difficulty", "category", "feature", "defects",
            "launchArgument", "condition", "expectedClean", "expectedBuggy", "task", "keyLocators"}
ALLOWED = REQUIRED | {"profile"}
NON_EMPTY_STR = ("title", "task", "expectedClean", "expectedBuggy", "feature", "launchArgument")


def fail(msg):
    print(f"✗ {msg}")
    sys.exit(1)


def structural_check(entries, prefix):
    if not isinstance(entries, list) or not entries:
        fail("exercises.json must be a non-empty array")
    ids = set()
    names = []
    for i, e in enumerate(entries):
        where = f"[{i}] {e.get('id', '?')}"
        if not isinstance(e, dict):
            fail(f"{where}: not an object")
        keys = set(e)
        missing = REQUIRED - keys
        if missing:
            fail(f"{where}: missing keys {sorted(missing)}")
        extra = keys - ALLOWED
        if extra:
            fail(f"{where}: unexpected keys {sorted(extra)}")
        if e["category"] not in CATEGORIES:
            fail(f"{where}: bad category {e['category']!r}")
        if e["difficulty"] not in DIFFICULTIES:
            fail(f"{where}: bad difficulty {e['difficulty']!r}")
        if not isinstance(e["defects"], list) or not e["defects"] or not all(isinstance(d, str) and d for d in e["defects"]):
            fail(f"{where}: defects must be a non-empty list of strings")
        if not isinstance(e["keyLocators"], list) or not all(isinstance(k, str) for k in e["keyLocators"]):
            fail(f"{where}: keyLocators must be a list of strings")
        if not isinstance(e["id"], str) or not e["id"].startswith(prefix + "-"):
            fail(f"{where}: id must start with {prefix!r}-")
        if e["id"] in ids:
            fail(f"{where}: duplicate id")
        ids.add(e["id"])
        for k in NON_EMPTY_STR:
            if not isinstance(e[k], str) or not e[k].strip():
                fail(f"{where}: {k} must be a non-empty string")
        names.extend(e["defects"])
    return names


def schema_check(entries, schema_path):
    try:
        import jsonschema  # optional
    except ImportError:
        return  # structural_check already covers the same rules
    with open(schema_path) as fh:
        schema = json.load(fh)
    jsonschema.validate(entries, schema)


def main():
    if len(sys.argv) < 3:
        fail("usage: check_exercises.py <exercises.json> <OWN_PREFIX> [<sibling_raw_url>]")
    path, prefix = sys.argv[1], sys.argv[2]
    sibling_url = sys.argv[3] if len(sys.argv) > 3 else None

    with open(path) as fh:
        entries = json.load(fh)

    own_names = structural_check(entries, prefix)
    schema_check(entries, path.replace("exercises.json", "exercises.schema.json"))
    print(f"✓ schema/structure OK: {len(entries)} exercises, {len(set(own_names))} distinct defects")

    if not sibling_url:
        return
    try:
        raw = urllib.request.urlopen(sibling_url, timeout=25).read()
        sibling = json.loads(raw)
    except Exception as ex:  # network/availability — advisory only
        print(f"! parity skipped (could not fetch sibling: {ex})")
        return

    sib_names = set()
    for e in sibling:
        sib_names.update(e.get("defects", []))
    own = set(own_names)
    if own != sib_names:
        only_own = sorted(own - sib_names)
        only_sib = sorted(sib_names - own)
        fail("cross-platform parity broken\n"
             f"    only here ({len(only_own)}): {only_own}\n"
             f"    only sibling ({len(only_sib)}): {only_sib}")
    print(f"✓ cross-platform parity OK: {len(own)} defects match the sibling app")


if __name__ == "__main__":
    main()
