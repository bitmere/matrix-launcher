#!/usr/bin/env python3
"""Fail-closed Matrix Actions release checks; never handles signing passwords."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

REPO = 'bitmere/matrix-launcher'
PIN = 'a21fc7cb0cf12ab08744e03a84fc45be7fd50aafb7397f054d250ce1a5151b9b'
APK = 'Matrix-Launcher.apk'
CHECKSUM = APK + '.sha256'
ASSETS = (APK, CHECKSUM)
MAX_APK_BYTES = 256 * 1024 * 1024
CHECKSUM_BYTES = 64 + 2 + len(APK) + 1


def require(condition, message):
    if not condition:
        raise ValueError(message)


def command(args, **kwargs):
    return subprocess.run(args, timeout=300, **kwargs)


def run(*args):
    return subprocess.check_output(args, text=True, timeout=180).strip()


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def metadata(tag):
    props = {}
    for line in Path('version.properties').read_text().splitlines():
        line = line.strip()
        if not line or line.startswith(('#', '!')):
            continue
        key, separator, value = line.partition('=')
        key, value = key.strip(), value.strip()
        require(separator and key not in props, 'Malformed or duplicate version property')
        props[key] = value
    require(all(key in props for key in ('versionName', 'versionCode', 'releaseChannel')), 'Missing version property')
    version, raw_code, channel = (props[key] for key in ('versionName', 'versionCode', 'releaseChannel'))
    require(re.fullmatch(r'\d+\.\d+\.\d+(-beta\.\d+)?', version), 'Invalid versionName')
    require(re.fullmatch(r'[1-9]\d*', raw_code), 'Invalid versionCode')
    require(tag == f'v{version}', 'Tag must exactly match versionName')
    require(channel in ('stable', 'beta') and ('-beta.' in version) == (channel == 'beta'), 'Invalid release channel')
    changelog = Path('CHANGELOG.md').read_text()
    match = re.search(r'^## ' + re.escape(version) + r'(?:[ \t][^\n]*)?\n(.*?)(?=^## |\Z)', changelog, re.M | re.S)
    require(match and match[1].strip(), 'Missing reviewed changelog entry')
    notes = match[1].strip()
    require(not any(line.lstrip().lower().startswith('android-version-code') for line in notes.splitlines()),
            'Reviewed notes contain a version trailer')
    notes += f'\n\nAndroid-Version-Code: {raw_code}'
    return dict(versionName=version, versionCode=int(raw_code), prerelease=channel == 'beta', notes=notes)


class APIError(ValueError):
    pass


def api(path, method='GET', payload=None, upload=None):
    args = ['gh', 'api', '--hostname', 'github.com', '--include', '--method', method,
            '-H', 'X-GitHub-Api-Version: 2022-11-28', path]
    options = dict(text=True, capture_output=True)
    if payload is not None:
        args += ['--input', '-', '-H', 'Content-Type: application/json']
        options['input'] = json.dumps(payload)
    if upload is not None:
        args += ['--input', str(upload), '-H', 'Content-Type: application/octet-stream',
                 '-H', f'Content-Length: {upload.stat().st_size}']
    for attempt in range(4):
        try:
            result = command(args, **options)
        except subprocess.TimeoutExpired:
            if method != 'GET' or attempt == 3:
                raise
            time.sleep(2 ** attempt)
            continue
        body = result.stdout.replace('\r\n', '\n')
        status = None
        # gh --include uses HTTP/2.0 as well as HTTP/1.1; allow intermediate headers.
        while body.startswith('HTTP/'):
            header, separator, body = body.partition('\n\n')
            match = re.match(r'HTTP/\S+\s+(\d{3})(?:\s|$)', header)
            require(separator and match, 'Missing GitHub API response status')
            status = int(match[1])
        if status is None and result.returncode != 0:
            if method == 'GET' and attempt < 3:
                time.sleep(2 ** attempt)
                continue
            raise APIError('GitHub API transport failed (no HTTP status)')
        require(status is not None, 'Missing GitHub API response status')
        if method == 'GET' and status in (429, 500, 502, 503, 504) and attempt < 3:
            time.sleep(2 ** attempt)
            continue
        if method == 'GET' and status == 404:
            return None
        if result.returncode != 0 or status not in (200, 201):
            raise APIError(f'GitHub API {method} failed (HTTP {status})')
        try:
            return json.loads(body)
        except json.JSONDecodeError as error:
            raise ValueError('Invalid GitHub API JSON') from error


def repo_path():
    require(os.environ['GITHUB_REPOSITORY'] == REPO, 'Unexpected repository')
    return f'repos/{REPO}'


def listing(path):
    items = []
    for page in range(1, 1001):
        batch = api(f'{path}?per_page=100&page={page}')
        require(isinstance(batch, list) and all(isinstance(x, dict) for x in batch), 'Invalid GitHub list response')
        items.extend(batch)
        if len(batch) < 100:
            return items
    raise ValueError('GitHub pagination limit exceeded')


def validate_release(release, meta, tag, source):
    require(isinstance(release, dict), 'Missing or invalid release response')
    require(type(release.get('id')) is int and release['id'] > 0, 'Invalid release ID')
    require(type(release.get('draft')) is bool and type(release.get('prerelease')) is bool, 'Invalid release flags')
    require(release.get('target_commitish') == source, 'Release source metadata mismatch')
    pending = release['draft'] and re.fullmatch(r'untagged-[A-Za-z0-9]+', str(release.get('tag_name')))
    require((release.get('tag_name') == tag or pending) and release['prerelease'] == meta['prerelease'], 'Release tag/channel mismatch')
    require(release.get('name') == f'Matrix Launcher {tag}' and isinstance(release.get('body'), str)
            and release['body'].strip() == meta['notes'].strip(), 'Release notes/title mismatch')
    require(run('git', 'rev-parse', f'refs/tags/{tag}^{{commit}}') == source, 'Release source mismatch')
    return release


def release_state(meta, tag, source, releases=None):
    # GET-by-tag is documented for published releases. Draft URLs may be untagged-*.
    if releases is None:
        releases = listing(repo_path() + '/releases')
    matches = [r for r in releases if r.get('tag_name') == tag or
               (r.get('draft') is True and str(r.get('tag_name', '')).startswith('untagged-')
                and r.get('name') == f'Matrix Launcher {tag}')]
    require(len(matches) <= 1, 'Ambiguous releases for tag; refusing mutation')
    if not matches:
        return None
    release = validate_release(matches[0], meta, tag, source)
    return dict(release, assets=release_assets(release['id']))


def release_assets(release_id):
    assets = listing(f'{repo_path()}/releases/{release_id}/assets')
    names = set()
    for asset in assets:
        require(type(asset.get('id')) is int and asset['id'] > 0 and isinstance(asset.get('name'), str), 'Invalid release asset')
        require(asset['name'] not in names, 'Duplicate release asset name')
        names.add(asset['name'])
        require(asset.get('state') == 'uploaded' and type(asset.get('size')) is int and asset['size'] > 0,
                'Incomplete release asset; inspect draft before retrying')
    return assets


def download_asset(asset, dest):
    with open(dest, 'wb') as output:
        command(['gh', 'api', '--hostname', 'github.com', '-H', 'Accept: application/octet-stream',
                 f'{repo_path()}/releases/assets/{asset["id"]}'], stdout=output, check=True)
    require(dest.stat().st_size == asset['size'], 'Release asset download size mismatch')


def recover(meta, tag, source, work):
    release = release_state(meta, tag, source)
    apk = next((a for a in release['assets'] if a['name'] == 'Matrix-Launcher.apk'), None) if release else None
    if apk:
        download_asset(apk, work / APK)
    elif release:
        require(release['draft'], 'Published release missing APK; refusing rebuild')
        require(not any(a['name'] == 'Matrix-Launcher.apk.sha256' for a in release['assets']),
                'Draft checksum exists without APK; refusing rebuild')
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write(f'reuse={str(apk is not None).lower()}\n')


def verify_apk(meta, apk):
    pin = os.environ.get('MATRIX_RELEASE_CERT_SHA256', '').lower()
    require(pin == PIN, 'Configure the full approved MATRIX_RELEASE_CERT_SHA256 fingerprint')
    require(apk.is_file() and 0 < apk.stat().st_size <= MAX_APK_BYTES, 'Invalid APK size')
    build_tools = Path(os.environ['ANDROID_HOME']) / 'build-tools'
    candidates = [p for p in build_tools.iterdir() if re.fullmatch(r'\d+\.\d+\.\d+', p.name)
                  and (p / 'apksigner').is_file() and (p / 'aapt').is_file()]
    require(candidates, 'No complete stable Android build-tools installation')
    tool_dir = max(candidates, key=lambda p: tuple(map(int, p.name.split('.'))))
    certs = run(str(tool_dir / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk))
    # Diagnostic: print what actually signed the APK (and the raw apksigner
    # output) before comparing, so a mismatch can be diagnosed from the
    # release log alone. A certificate fingerprint is a public identifier.
    print('apksigner --print-certs output:')
    print(certs)
    # apksigner --print-certs formats differ by build-tools version: older
    # versions print "Signer #1 certificate SHA-256 digest: ...", newer ones
    # print "V2 Signer: certificate SHA-256 digest: ...". Match the digest
    # regardless of the prefix, and never the public-key digest line.
    signers = re.findall(r'^(?:Signer #\d+|V\d+ Signer:?)[ \t]+certificate SHA-256 digest:[ \t]*([0-9a-fA-F]{64})[ \t]*$', certs, re.M)
    print(f'APK signer fingerprints: {[s.lower() for s in signers]}')
    require(signers, 'No signer certificate SHA-256 digest found in apksigner output')
    require({s.lower() for s in signers} == {pin}, 'APK signer differs from approved full fingerprint')
    badging = run(str(tool_dir / 'aapt'), 'dump', 'badging', str(apk))
    package = re.search(r'^package:[ \t]+(.*)$', badging, re.M)
    fields = dict(re.findall(r"(\w+)='([^']*)'", package[1])) if package else {}
    require(tuple(fields.get(k) for k in ('name', 'versionCode', 'versionName')) ==
            ('com.matrix.launcher', str(meta['versionCode']), meta['versionName']), 'APK package/version mismatch')
    require(not re.search(r'^application-debuggable', badging, re.M), 'Debuggable release rejected')


def checksum_line(sha):
    return f'{sha}  {APK}\n'.encode('ascii')


def verified_checksum(work):
    apk = work / APK
    require(0 < apk.stat().st_size <= MAX_APK_BYTES, 'Invalid APK size')
    sha = digest(apk)
    require((work / CHECKSUM).read_bytes() == checksum_line(sha), 'Local checksum differs')
    return sha


def guard(releases, version_code):
    for release in releases:
        require(type(release.get('draft')) is bool and type(release.get('prerelease')) is bool,
                'Invalid release flags')
        if not release['draft'] and not release['prerelease']:
            require(release_code(release.get('body')) <= version_code, 'Stable release rollback refused')


def release_code(body):
    require(isinstance(body, str), 'Missing release notes')
    lines = body.rstrip('\r\n').splitlines()
    match = re.fullmatch(r'Android-Version-Code: ([1-9][0-9]*)', lines[-1]) if lines else None
    require(match and sum(line.lstrip().lower().startswith('android-version-code') for line in lines) == 1,
            'Missing or invalid release version trailer')
    return int(match[1])


def check_assets(release, work):
    for name in ASSETS:
        asset = next((a for a in release['assets'] if a['name'] == name), None)
        require(asset is not None, 'Published release missing required asset')
        existing = work / ('existing-' + name)
        download_asset(asset, existing)
        require(existing.read_bytes() == (work / name).read_bytes(), 'Existing release asset differs; never overwrite')


def publish(meta, tag, source, work):
    verified_checksum(work)
    releases = listing(repo_path() + '/releases')
    guard(releases, meta['versionCode'])
    release = release_state(meta, tag, source, releases)
    if release is None:
        try:
            # Capture the ID directly. Never interpret gh's human-facing release URL.
            release = api(repo_path() + '/releases', 'POST', dict(tag_name=tag, target_commitish=source,
                          name=f'Matrix Launcher {tag}', body=meta['notes'], draft=True, prerelease=meta['prerelease']))
            validate_release(release, meta, tag, source)
            release = dict(release, assets=release_assets(release['id']))
        except (APIError, subprocess.TimeoutExpired):
            # Creation may have succeeded despite a lost response, or another writer won.
            release = release_state(meta, tag, source)
            if release is None:
                raise
    release_id = release['id']
    # Check all existing bytes before uploading anything to a partially populated draft.
    for asset in release['assets']:
        if asset['name'] in ASSETS:
            existing = work / ('existing-' + asset['name'])
            download_asset(asset, existing)
            require(existing.read_bytes() == (work / asset['name']).read_bytes(), 'Existing release asset differs; never overwrite')
    if release['draft'] and release['tag_name'] != tag:
        release = validate_release(api(f'{repo_path()}/releases/{release_id}', 'PATCH', {'tag_name': tag}), meta, tag, source)
        release = dict(release, assets=release_assets(release_id))
    for name in ASSETS:
        asset = next((a for a in release['assets'] if a['name'] == name), None)
        if asset is None:
            require(release['draft'], 'Published release missing required asset')
            try:
                api(f'https://uploads.github.com/{repo_path()}/releases/{release_id}/assets?name={name}',
                    'POST', upload=work / name)
            except (APIError, subprocess.TimeoutExpired):
                # Do not retry uploads or replace assets: reconcile a lost/duplicate response.
                assets = release_assets(release_id)
                require(any(a['name'] == name for a in assets), 'Upload failed; no completed asset found')
    # Verify stored bytes, including newly uploaded assets, before making a draft public.
    fresh = validate_release(api(f'{repo_path()}/releases/{release_id}'), meta, tag, source)
    require(fresh['id'] == release_id, 'Release ID changed')
    fresh = dict(fresh, assets=release_assets(release_id))
    check_assets(fresh, work)
    if fresh['draft']:
        guard(listing(repo_path() + '/releases'), meta['versionCode'])
        endpoint = f'{repo_path()}/releases/{release_id}'
        try:
            published = api(endpoint, 'PATCH', {'draft': False, 'tag_name': tag})
        except (APIError, subprocess.TimeoutExpired):
            published = api(endpoint)
        validate_release(published, meta, tag, source)
        require(published['id'] == release_id and published['draft'] is False and published['tag_name'] == tag, 'Release remained a draft')


class PublicRedirect(urllib.request.HTTPRedirectHandler):
    max_redirections = 4

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        target = urllib.parse.urlsplit(newurl)
        require(target.scheme == 'https' and target.netloc == 'release-assets.githubusercontent.com',
                'Unexpected public download redirect')
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def public_bytes(url, limit):
    request = urllib.request.Request(url, headers={'User-Agent': 'Matrix-release-gate'})
    with urllib.request.build_opener(PublicRedirect()).open(request, timeout=30) as response:
        data = response.read(limit + 1)
    require(len(data) <= limit, 'Public response exceeds size limit')
    return data


def anonymous(meta, tag, work):
    sha = verified_checksum(work)
    endpoint = f'https://api.github.com/repos/{REPO}/releases/tags/{tag}'
    for attempt in range(3):
        try:
            release = json.loads(public_bytes(endpoint, 256 * 1024))
            break
        except urllib.error.HTTPError as error:
            if error.code != 404 or attempt == 2:
                raise
            time.sleep(2 ** attempt)
    require(isinstance(release, dict) and release.get('tag_name') == tag
            and release.get('name') == f'Matrix Launcher {tag}' and release.get('draft') is False
            and release.get('prerelease') is meta['prerelease'], 'Public release metadata mismatch')
    require(release_code(release.get('body')) == meta['versionCode'], 'Public release version mismatch')
    assets = release.get('assets')
    require(isinstance(assets, list) and all(isinstance(asset, dict) for asset in assets),
            'Invalid public asset list')
    selected = []
    for name in ASSETS:
        matches = [asset for asset in assets if asset.get('name') == name]
        require(len(matches) == 1, 'Missing or duplicate public asset')
        asset = matches[0]
        url = f'https://github.com/{REPO}/releases/download/{tag}/{name}'
        size = (work / name).stat().st_size
        require(asset.get('browser_download_url') == url and asset.get('state') == 'uploaded'
                and type(asset.get('size')) is int and asset['size'] == size, 'Public asset metadata mismatch')
        selected.append((name, url, size))
    for name, url, size in selected:
        data = public_bytes(url, size)
        require(data == (work / name).read_bytes(), 'Public asset bytes differ')
        if name == APK:
            require(hashlib.sha256(data).hexdigest() == sha, 'Public APK SHA-256 differs')
    return sha


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['prepare', 'recover', 'verify', 'publish', 'anonymous'])
    args = parser.parse_args()
    tag, source = os.environ['RELEASE_TAG'], os.environ['SOURCE_SHA']
    meta = metadata(tag)
    work = Path(os.environ['RELEASE_WORK'])
    work.mkdir(parents=True, exist_ok=True)
    apk = work / APK
    if args.command == 'prepare':
        (work / 'notes.md').write_text(meta['notes'])
    elif args.command == 'recover':
        recover(meta, tag, source, work)
    elif args.command == 'verify':
        verify_apk(meta, apk)
        (work / CHECKSUM).write_bytes(checksum_line(digest(apk)))
    elif args.command == 'publish':
        publish(meta, tag, source, work)
    else:
        sha = anonymous(meta, tag, work)
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
            summary.write(f'\nSource: `{source}`\n\nRelease: https://github.com/{REPO}/releases/tag/{tag}\n'
                          f'\nAPK SHA-256: `{sha}`\n')


if __name__ == '__main__':
    main()
