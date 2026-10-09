#!/usr/bin/env python3
"""
WioSpor Repository Validator

Validates:
- repo.json structure
- plugins.json validity
- plugin URL accessibility
- artifact file size
- artifact SHA-256 hash
- unique internalName across plugins
- binary integrity (not HTML/JSON error)

Exits with non-zero on ANY failure.
"""

import sys
import json
import hashlib
import urllib.request
import urllib.error
from pathlib import Path
from typing import List, Dict, Tuple

USER_AGENT = "CloudStream/4.0"
TIMEOUT = 15
MIN_ARTIFACT_SIZE = 512  # Minimum expected .cs3 size in bytes

class ValidationError(Exception):
    """Raised when validation fails."""
    pass

def load_json(path: Path) -> dict:
    """Load and parse JSON file."""
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        raise ValidationError(f"Failed to load {path}: {e}")

def validate_repo_json(repo_data: dict) -> str:
    """
    Validate repo.json structure.
    Returns the pluginLists URL.
    """
    if not isinstance(repo_data, dict):
        raise ValidationError("repo.json must be an object")
    
    if "pluginLists" not in repo_data:
        raise ValidationError("repo.json missing 'pluginLists'")
    
    plugin_lists = repo_data.get("pluginLists", [])
    if not isinstance(plugin_lists, list) or len(plugin_lists) == 0:
        raise ValidationError("pluginLists must be a non-empty array")
    
    return plugin_lists[0]

def fetch_plugins_json(url: str) -> list:
    """Fetch and parse plugins.json from URL or file."""
    if url.startswith("http"):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                if resp.status != 200:
                    raise ValidationError(f"plugins.json URL returned {resp.status}")
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.URLError as e:
            raise ValidationError(f"Failed to fetch plugins.json from {url}: {e}")
        except json.JSONDecodeError as e:
            raise ValidationError(f"plugins.json is not valid JSON: {e}")
    else:
        # Local file path
        return load_json(Path(url))

def validate_plugin_entry(plugin: dict, index: int) -> Tuple[str, str, str]:
    """
    Validate single plugin entry.
    Returns (internalName, url, name) or raises ValidationError.
    """
    if not isinstance(plugin, dict):
        raise ValidationError(f"Plugin {index} is not an object")
    
    internal_name = plugin.get("internalName")
    url = plugin.get("url")
    name = plugin.get("name", f"Plugin {index}")
    
    if not internal_name:
        raise ValidationError(f"Plugin '{name}' missing internalName")
    if not url:
        raise ValidationError(f"Plugin '{internal_name}' missing url")
    
    return internal_name, url, name

def download_artifact(url: str, name: str) -> Tuple[bytes, int]:
    """
    Download artifact from URL.
    Returns (binary_data, actual_size).
    Raises ValidationError on failure.
    """
    try:
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            if resp.status != 200:
                raise ValidationError(f"  ✗ {name}: HTTP {resp.status}")
            
            body = resp.read()
            actual_size = len(body)
            
            if actual_size < MIN_ARTIFACT_SIZE:
                raise ValidationError(
                    f"  ✗ {name}: artifact too small ({actual_size} bytes, minimum {MIN_ARTIFACT_SIZE})"
                )
            
            # Check if response is HTML/JSON error instead of binary
            if body[:100].lower().startswith(b"<!doctype") or body[:1] == b"{":
                raise ValidationError(
                    f"  ✗ {name}: received HTML/JSON error response instead of binary artifact"
                )
            
            return body, actual_size
    except urllib.error.URLError as e:
        raise ValidationError(f"  ✗ {name}: download failed - {e}")
    except urllib.error.HTTPError as e:
        raise ValidationError(f"  ✗ {name}: HTTP error {e.code}")

def verify_artifact(
    plugin: dict,
    artifact_data: bytes,
    actual_size: int,
    name: str
) -> None:
    """
    Verify artifact size and SHA-256 hash.
    Raises ValidationError on mismatch.
    """
    expected_size = plugin.get("fileSize")
    expected_hash = plugin.get("fileHash")
    
    # Verify size
    if expected_size and actual_size != expected_size:
        raise ValidationError(
            f"  ✗ {name}: size mismatch (expected {expected_size}, got {actual_size})"
        )
    
    # Verify SHA-256
    if expected_hash:
        actual_hash_hex = hashlib.sha256(artifact_data).hexdigest()
        actual_hash = f"sha256-{actual_hash_hex}"
        
        if actual_hash != expected_hash:
            raise ValidationError(
                f"  ✗ {name}: SHA-256 mismatch\n"
                f"    Expected: {expected_hash}\n"
                f"    Got:      {actual_hash}"
            )

def main() -> int:
    """Run full repository validation."""
    print("=" * 70)
    print("WioSpor Repository Validator")
    print("=" * 70)
    
    try:
        root = Path(__file__).resolve().parent.parent
        repo_json_path = root / "repo.json"
        plugins_json_path = root / "plugins.json"
        
        # 1. Validate repo.json
        print("\n[1/5] Validating repo.json...")
        repo_data = load_json(repo_json_path)
        plugins_url = validate_repo_json(repo_data)
        print(f"  ✓ repo.json is valid")
        print(f"  ✓ pluginLists URL: {plugins_url}")
        
        # 2. Load plugins.json
        print("\n[2/5] Loading plugins.json...")
        if plugins_url.startswith("http"):
            print(f"  ℹ Fetching from {plugins_url}")
            plugins = fetch_plugins_json(plugins_url)
        else:
            plugins = load_json(plugins_json_path)
        print(f"  ✓ Loaded {len(plugins)} plugins")
        
        # 3. Check for duplicate internalNames
        print("\n[3/5] Checking for duplicate internalNames...")
        internal_names: Dict[str, int] = {}
        for idx, plugin in enumerate(plugins):
            try:
                internal_name, _, _ = validate_plugin_entry(plugin, idx)
                if internal_name in internal_names:
                    raise ValidationError(
                        f"Duplicate internalName: '{internal_name}' at index {idx} "
                        f"and {internal_names[internal_name]}"
                    )
                internal_names[internal_name] = idx
            except ValidationError as e:
                raise e
        print(f"  ✓ All {len(internal_names)} internalNames are unique")
        
        # 4. Validate and download artifacts
        print("\n[4/5] Downloading and verifying artifacts...")
        success_count = 0
        for idx, plugin in enumerate(plugins, 1):
            try:
                internal_name, url, name = validate_plugin_entry(plugin, idx)
                
                # Download
                artifact_data, actual_size = download_artifact(url, name)
                
                # Verify
                verify_artifact(plugin, artifact_data, actual_size, name)
                
                print(f"  ✓ {name} ({actual_size} bytes)")
                success_count += 1
            except ValidationError as e:
                print(str(e))
                return 1
        
        print(f"\n  ✓ All {success_count}/{len(plugins)} plugins downloaded and verified")
        
        # 5. Summary
        print("\n[5/5] Validation Summary")
        print("=" * 70)
        print(f"✓ repo.json:               VALID")
        print(f"✓ plugins.json:            {len(plugins)} entries")
        print(f"✓ unique internalNames:    {len(internal_names)}")
        print(f"✓ artifacts downloaded:    {success_count}/{len(plugins)}")
        print(f"✓ size verified:           {success_count}/{len(plugins)}")
        print(f"✓ SHA-256 verified:        {success_count}/{len(plugins)}")
        print("=" * 70)
        print("✓ Repository is valid and ready for distribution!")
        print("=" * 70)
        
        return 0
    
    except ValidationError as e:
        print(f"\n✗ Validation failed:")
        print(f"  {e}")
        print("\n" + "=" * 70)
        print("✗ Repository validation FAILED")
        print("=" * 70)
        return 1
    except Exception as e:
        print(f"\n✗ Unexpected error: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
