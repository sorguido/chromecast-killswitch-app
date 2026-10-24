#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Collect release runtime sources from the checked-in, checksum-pinned manifest.

--update-manifest is a maintainer operation: review every changed URL and hash
before committing. Normal collection refuses unreviewed bytes or a changed lock.
No credentials, local caches, or APKs are copied into this source package.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import shutil
import urllib.error
import urllib.request
import zipfile
import tarfile

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'scripts/runtime-sources.json'
LOCK = ROOT / 'app/gradle.lockfile'
SPAKE2 = 'com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1'
UPSTREAM = [
    ('androidx-graphics-path-8a05a22af450d589ef911d772a001a49dcb05b71.tar.gz',
     'https://android.googlesource.com/platform/frameworks/support/+archive/8a05a22af450d589ef911d772a001a49dcb05b71/graphics/graphics-path.tar.gz',
     'Graphics Path 1.0.1 complete module including JNI/C++ and CMake; revision from the official AndroidX 1.0.1 release commit range.'),
    ('conscrypt-fd209de6167201e4f3bca5b99c6db9f08d1843c6.tar.gz',
     'https://codeload.github.com/google/conscrypt/tar.gz/fd209de6167201e4f3bca5b99c6db9f08d1843c6',
     'Complete Conscrypt 2.5.3 source, native JNI, build scripts and upstream notices.'),
    ('boringssl-49f0329110a1d93a5febc2bceceedc655d995420.tar.gz',
     'https://codeload.github.com/google/boringssl/tar.gz/49f0329110a1d93a5febc2bceceedc655d995420',
     'BoringSSL revision identified by the resolved Conscrypt 2.5.3 AAR, including native sources and build scripts.'),
]


def digest(path: Path) -> str:
    return hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()


def coordinates() -> list[str]:
    return sorted(line.split('=', 1)[0] for line in LOCK.read_text().splitlines()
                  if '=' in line and 'releaseRuntimeClasspath' in line.split('=', 1)[1].split(','))


def fetch(url: str, path: Path, expected: str | None = None) -> str:
    if path.exists() and (expected is None or digest(path) == expected):
        return digest(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix(path.suffix + '.part')
    try:
        request = urllib.request.Request(url, headers={'User-Agent': 'ChromecastKillSwitch-source-collector/1.1'})
        with urllib.request.urlopen(request, timeout=90) as source, temp.open('wb') as target:
            shutil.copyfileobj(source, target)
        actual = digest(temp)
        if expected and actual != expected:
            raise ValueError(f'Checksum mismatch: {path.name}')
        temp.replace(path)
        return actual
    finally:
        temp.unlink(missing_ok=True)


def validate_archive(path: Path) -> None:
    if path.suffix == '.jar':
        with zipfile.ZipFile(path) as archive:
            if archive.testzip():
                raise ValueError(f'Invalid ZIP: {path.name}')
    elif path.name.endswith('.tar.gz'):
        with tarfile.open(path, 'r:gz') as archive:
            if not archive.getmembers():
                raise ValueError(f'Empty tar: {path.name}')


def update_manifest(output: Path) -> dict:
    coords = coordinates()
    entries = []
    metadata_only = []
    def collect_coord(coord: str) -> tuple[list[dict], list[str]]:
        if coord == SPAKE2:
            return [], []
        group, artifact, version = coord.split(':')
        base = 'https://dl.google.com/dl/android/maven2' if group.startswith('androidx.') else 'https://repo.maven.apache.org/maven2'
        location = f'{group.replace(".", "/")}/{artifact}/{version}'
        result = []
        missing = []
        for suffix in ('.pom', '-sources.jar'):
            if (artifact.endswith('-bom') or coord == 'org.jetbrains.kotlin:kotlin-stdlib-common:2.2.10') and suffix == '-sources.jar':
                missing.append(coord)
                continue
            filename = f'{artifact}-{version}{suffix}'
            relative = f'maven/{location}/{filename}'
            url = f'{base}/{location}/{filename}'
            try:
                sha = fetch(url, output / relative)
            except urllib.error.HTTPError as error:
                if error.code == 404 and suffix == '-sources.jar':
                    raise RuntimeError(f'No source JAR for {coord}; explicitly investigate before adding an exception') from error
                raise
            validate_archive(output / relative)
            result.append({'path': relative, 'url': url, 'sha256': sha, 'coordinate': coord})
        return result, missing
    with ThreadPoolExecutor(max_workers=8) as executor:
        for found, missing in executor.map(collect_coord, coords):
            entries.extend(found)
            metadata_only.extend(missing)
    for filename, url, description in UPSTREAM:
        relative = f'upstream/{filename}'
        sha = fetch(url, output / relative)
        validate_archive(output / relative)
        entries.append({'path': relative, 'url': url, 'sha256': sha, 'description': description})
    return {'schema_version': 1, 'lock_sha256': digest(LOCK), 'configuration': 'releaseRuntimeClasspath',
            'coordinates': coords, 'metadata_only': metadata_only,
            'in_repository': {SPAKE2: 'vendor/spake2-source; complete Java and native submodule archives, licenses and rebuild script',
                              'LibADB': 'vendor/libadb; patched sources, licenses and build script'},
            'files': sorted(entries, key=lambda entry: entry['path'])}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / 'development/release-sources')
    parser.add_argument('--update-manifest', action='store_true')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    if args.update_manifest:
        manifest = update_manifest(output)
        MANIFEST.write_text(json.dumps(manifest, indent=2) + '\n')
    else:
        manifest = json.loads(MANIFEST.read_text())
        if digest(LOCK) != manifest['lock_sha256'] or coordinates() != manifest['coordinates']:
            raise SystemExit('Runtime dependency lock changed; review and regenerate the source manifest first.')
        def collect(entry: dict) -> None:
            path = output / entry['path']
            fetch(entry['url'], path, entry['sha256'])
            validate_archive(path)
        with ThreadPoolExecutor(max_workers=8) as executor:
            list(executor.map(collect, manifest['files']))
    shutil.copy2(MANIFEST, output / 'runtime-sources.json')
    for name in ('LICENSE', 'THIRD_PARTY_NOTICES.md'):
        shutil.copy2(ROOT / name, output / name)
    if (ROOT / 'LICENSE-ADDITIONAL-PERMISSION').exists():
        shutil.copy2(ROOT / 'LICENSE-ADDITIONAL-PERMISSION', output / 'LICENSE-ADDITIONAL-PERMISSION')
    for relative in ('vendor/licenses', 'vendor/spake2-source', 'vendor/libadb'):
        shutil.copytree(ROOT / relative, output / relative, dirs_exist_ok=True,
                        ignore=shutil.ignore_patterns('build', '.gradle', '.kotlin'))
    (output / 'app').mkdir(exist_ok=True)
    shutil.copy2(LOCK, output / 'app/gradle.lockfile')
    # Companion app archive supplies the full build; preserve notice-link paths here too.
    (output / 'docs').mkdir(exist_ok=True)
    for path in (ROOT / 'docs').glob('*.md'):
        shutil.copy2(path, output / 'docs' / path.name)
    (output / 'scripts').mkdir(exist_ok=True)
    shutil.copy2(MANIFEST, output / 'scripts/runtime-sources.json')
    shutil.copy2(Path(__file__), output / 'scripts/collect-runtime-sources.py')
    (output / 'README.md').write_text('''# Runtime dependency sources\n\nThis package accompanies the app source archive of the same release.\nThe app archive contains LibADB, SPAKE2, Gradle wrapper, locked dependencies,\nverification metadata, build scripts, and instructions for signing your own APK.\n\n`runtime-sources.json` identifies the exact `releaseRuntimeClasspath` coordinates,\nsource URLs and SHA-256 checksums. Maven source JARs contain preferred source\nforms; POM files retain component provenance and upstream SCM/license details.\nMetadata-only BOM and legacy common-library POMs have no executable code. Test-only dependencies are excluded.\nConscrypt and its exact embedded BoringSSL source revision include native sources,\nbuild scripts, and original notices in the upstream archives. Graphics Path
native C++/JNI and CMake sources are included at the official 1.0.1 revision.
The Graphics Path revision is the upper commit of the release range linked at
https://developer.android.com/jetpack/androidx/releases/graphics#graphics-path-1.0.1
(the complete module retains its upstream copyright and license headers).\n\nRun `python3 scripts/collect-runtime-sources.py` from the matching app source\ncheckout to download and verify this package again. No original private signing\nkey or ADB identity is required or included. Follow `docs/BUILD.md` in that checkout\nfor app compilation and `vendor/spake2-source/README.md` for replacing SPAKE2.\nA dependency rebuilt or modified from these sources needs its own validation.\n''')
    files = sorted(path for path in output.rglob('*') if path.is_file() and path.name != 'SHA256SUMS')
    (output / 'SHA256SUMS').write_text(''.join(f'{digest(path)}  {path.relative_to(output).as_posix()}\n' for path in files))
    print(f'Collected {len(manifest["files"])} pinned files for {len(manifest["coordinates"])} runtime coordinates in {output}')


if __name__ == '__main__':
    main()
