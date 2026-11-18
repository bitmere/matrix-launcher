"""Offline release boundary cases; no signing secrets, GitHub writes or Gradle."""
import copy
import io
import json
import subprocess
import urllib.error
from contextlib import ExitStack
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import release_gate as gate


class PublicationGateTest(unittest.TestCase):
    def setUp(self):
        self.meta = dict(versionName='2.7.6', versionCode=155, prerelease=False, notes='Reviewed notes')
        self.meta['notes'] += '\n\nAndroid-Version-Code: 155'

    def test_metadata_requires_exact_tag_notes_positive_code_and_channel(self):
        with tempfile.TemporaryDirectory() as directory:
            old = Path.cwd()
            try:
                os.chdir(directory)
                Path('version.properties').write_text('versionName=2.7.6\nversionCode=155\nreleaseChannel=stable\n')
                Path('CHANGELOG.md').write_text('# Changelog\n\n## 2.7.6 — stable\n\n- Reviewed change\n\n## 2.7.4\nOld\n')
                self.assertEqual(gate.metadata('v2.7.6')['versionCode'], 155)
                self.assertTrue(gate.metadata('v2.7.6')['notes'].endswith('Android-Version-Code: 155'))
                with self.assertRaises(ValueError):
                    gate.metadata('main')
                Path('version.properties').write_text('versionName=2.7.6\nversionCode=0\nreleaseChannel=stable\n')
                with self.assertRaises(ValueError):
                    gate.metadata('v2.7.6')
                Path('version.properties').write_text('versionName=2.7.6-beta.1\nversionCode=155\nreleaseChannel=stable\n')
                with self.assertRaises(ValueError):
                    gate.metadata('v2.7.6-beta.1')
                Path('version.properties').write_text('versionName=2.7.6\nversionCode=155\nreleaseChannel=stable\n')
                Path('CHANGELOG.md').write_text('## 2.7.4\nOld notes\n')
                with self.assertRaises(ValueError):
                    gate.metadata('v2.7.6')
            finally:
                os.chdir(old)

    def test_prefix_only_or_retired_signer_rejected_before_tool_invocation(self):
        for pin in ('', gate.PIN[:8], 'bd4e6bf1' + 'a' * 56):
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin), self.assertRaisesRegex(ValueError, 'fingerprint'):
                gate.verify_apk(self.meta, Path('not-needed.apk'))

    def test_verify_apk_prints_actual_signer_before_comparison(self):
        # Synthetic fixture output, never an actual certificate identity.
        other = '1' * 64
        certs = ('Verified using v1 scheme (JAR signing): true\n'
                 'Signer #1 certificate DN: CN=Fixture\n'
                 f'Signer #1 certificate SHA-256 digest: {other}\n')
        pin = gate.PIN
        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            (tools / 'apksigner').write_text('')
            (tools / 'aapt').write_text('')
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin, ANDROID_HOME=directory), \
                 patch.object(gate, 'run', return_value=certs), \
                 patch('sys.stdout', new_callable=io.StringIO) as out:
                with self.assertRaisesRegex(ValueError, 'APK signer differs'):
                    apk = Path(directory) / 'fixture.apk'
                    apk.write_bytes(b'fixture')
                    gate.verify_apk(self.meta, apk)
        printed = out.getvalue()
        self.assertIn(other, printed)
        self.assertIn('Signer #1 certificate SHA-256 digest', printed)

    def test_verify_apk_prints_matching_signer_and_passes(self):
        pin = gate.PIN
        certs = ('Verified using v1 scheme (JAR signing): true\n'
                 f'Signer #1 certificate SHA-256 digest: {pin}\n')
        badging = ("package: name='com.matrix.launcher' versionCode='155' versionName='2.7.6'\n")

        def fake_run(*args):
            return certs if 'apksigner' in args[0] else badging

        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            (tools / 'apksigner').write_text('')
            (tools / 'aapt').write_text('')
            apk = Path(directory) / 'fake.apk'
            apk.write_bytes(b'fake-apk')
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin, ANDROID_HOME=directory), \
                 patch.object(gate, 'run', side_effect=fake_run), \
                 patch('sys.stdout', new_callable=io.StringIO) as out:
                gate.verify_apk(self.meta, apk)
        self.assertIn(pin, out.getvalue())
        self.assertIn(f"APK signer fingerprints: ['{pin}']", out.getvalue())

    def test_verify_apk_accepts_v2_signer_format(self):
        # Newer build-tools print the verbose --print-certs digest as
        # "V2 Signer: ..." instead of "Signer #1 ..."; the public-key
        # digest line must never be picked up. Synthetic fixtures only.
        pin = gate.PIN
        other = '2' * 64
        certs = ('Verified using v1 scheme (JAR signing): true\n'
                 f'V2 Signer: certificate SHA-256 digest: {pin}\n'
                 f'V2 Signer: public key SHA-256 digest: {other}\n')
        badging = ("package: name='com.matrix.launcher' versionCode='155' versionName='2.7.6'\n")

        def fake_run(*args):
            return certs if 'apksigner' in args[0] else badging

        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            (tools / 'apksigner').write_text('')
            (tools / 'aapt').write_text('')
            apk = Path(directory) / 'fake.apk'
            apk.write_bytes(b'fake-apk')
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin, ANDROID_HOME=directory), \
                 patch.object(gate, 'run', side_effect=fake_run), \
                 patch('sys.stdout', new_callable=io.StringIO) as out:
                gate.verify_apk(self.meta, apk)
        self.assertIn(f"APK signer fingerprints: ['{pin}']", out.getvalue())

    def test_verify_apk_fails_loudly_without_certificate_digest(self):
        # Output with only a public-key digest must not be treated as an
        # empty signer list to compare against the pin.
        other = '2' * 64
        certs = ('Verified using v1 scheme (JAR signing): true\n'
                 f'Signer #1 public key SHA-256 digest: {other}\n')
        pin = gate.PIN
        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            (tools / 'apksigner').write_text('')
            (tools / 'aapt').write_text('')
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin, ANDROID_HOME=directory), \
                 patch.object(gate, 'run', return_value=certs), \
                 patch('sys.stdout', new_callable=io.StringIO) as out:
                with self.assertRaisesRegex(ValueError, 'No signer certificate SHA-256 digest found'):
                    apk = Path(directory) / 'fixture.apk'
                    apk.write_bytes(b'fixture')
                    gate.verify_apk(self.meta, apk)
        self.assertIn(other, out.getvalue())

    def test_release_reuse_requires_source_channel_and_notes(self):
        release = dict(id=17, draft=True, assets=[], tag_name='v2.7.6', target_commitish='abc', prerelease=False,
                       name='Matrix Launcher v2.7.6', body=self.meta['notes'])
        with patch.dict(os.environ, GITHUB_REPOSITORY='bitmere/matrix-launcher'), patch.object(gate, 'run', return_value='abc'):
            with patch.object(gate, 'listing', side_effect=[[release], []]):
                self.assertEqual(gate.release_state(self.meta, 'v2.7.6', 'abc'), release)
            for key, value in [('target_commitish', 'other'), ('prerelease', True), ('body', 'Changed')]:
                with patch.object(gate, 'listing', return_value=[dict(release, **{key: value})]), self.assertRaises(ValueError):
                    gate.release_state(self.meta, 'v2.7.6', 'abc')

    def test_recovery_downloads_existing_apk_and_marks_reuse(self):
        asset = dict(name='Matrix-Launcher.apk', id=7)
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            output = work / 'output'
            with patch.dict(os.environ, GITHUB_OUTPUT=str(output)), patch.object(gate, 'release_state', return_value=dict(assets=[asset])), patch.object(gate, 'download_asset') as download:
                gate.recover(self.meta, 'v2.7.6', 'abc', work)
                download.assert_called_once_with(asset, work / 'Matrix-Launcher.apk')
                self.assertEqual(output.read_text(), 'reuse=true\n')

    def test_recovery_only_rebuilds_an_empty_draft(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            output = work / 'output'
            with patch.dict(os.environ, GITHUB_OUTPUT=str(output)):
                with patch.object(gate, 'release_state', return_value=dict(assets=[], draft=False)), self.assertRaisesRegex(ValueError, 'refusing rebuild'):
                    gate.recover(self.meta, 'v2.7.6', 'abc', work)
                with patch.object(gate, 'release_state', return_value=dict(assets=[], draft=True)):
                    gate.recover(self.meta, 'v2.7.6', 'abc', work)
                    self.assertEqual(output.read_text(), 'reuse=false\n')

    def test_divergent_private_asset_never_uploads_a_replacement(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            (work / 'Matrix-Launcher.apk').write_bytes(b'verified')
            sha = gate.digest(work / 'Matrix-Launcher.apk')
            (work / 'Matrix-Launcher.apk.sha256').write_text(f'{sha}  Matrix-Launcher.apk\n')
            release = dict(id=17, assets=[dict(name='Matrix-Launcher.apk', id=7)], draft=False)
            def divergent_download(asset, destination):
                destination.write_bytes(b'divergent')
            with patch.dict(os.environ, GITHUB_REPOSITORY=gate.REPO), patch.object(gate, 'listing', return_value=[]), patch.object(gate, 'release_state', return_value=release), patch.object(gate, 'download_asset', side_effect=divergent_download), patch.object(gate.subprocess, 'run') as mutation:
                with self.assertRaisesRegex(ValueError, 'never overwrite'):
                    gate.publish(self.meta, 'v2.7.6', 'abc', work)
                mutation.assert_not_called()

    def test_http_auth_failure_is_not_absent_release(self):
        response = type('Response', (), {'stdout': 'HTTP/2.0 403 Forbidden\n\n{}', 'returncode': 1})()
        with patch.object(gate.subprocess, 'run', return_value=response), self.assertRaises(ValueError):
            gate.api('repos/bitmere/matrix-launcher/releases/tags/v2.7.6')
        response.stdout = 'HTTP/2.0 404 Not Found\n\n{}'
        with patch.object(gate.subprocess, 'run', return_value=response):
            self.assertIsNone(gate.api('repos/bitmere/matrix-launcher/releases/tags/v2.7.6'))




class ReleaseLifecycleTest(unittest.TestCase):
    # Offline fake REST service: no certificate identities, credentials or GitHub state.
    def setUp(self):
        PublicationGateTest.setUp(self)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        self.apk = b'optimized-fixture'
        self.sha = gate.hashlib.sha256(self.apk).hexdigest()
        (self.work / 'Matrix-Launcher.apk').write_bytes(self.apk)
        (self.work / 'Matrix-Launcher.apk.sha256').write_text(f'{self.sha}  Matrix-Launcher.apk\n')
        (self.work / 'notes.md').write_text(self.meta['notes'])
        self.release = dict(id=448631718, tag_name='v2.7.6', target_commitish='abc', draft=True,
                            prerelease=False, name='Matrix Launcher v2.7.6', body=self.meta['notes'], assets=[])
        self.releases = [self.release]
        self.bytes = {}
        self.calls = []
        self.lose_create = False
        self.lose_upload = False
        self.corrupt_upload = False
        stack = ExitStack()
        self.addCleanup(stack.close)
        stack.enter_context(patch.dict(os.environ, GITHUB_REPOSITORY='bitmere/matrix-launcher'))
        stack.enter_context(patch.object(gate, 'api', side_effect=self.fake_api))
        stack.enter_context(patch.object(gate, 'run', return_value='abc'))
        stack.enter_context(patch.object(gate, 'download_asset', side_effect=self.download))
        # Any unmocked subprocess would be an offline test bug.
        stack.enter_context(patch.object(gate, 'command', side_effect=AssertionError('Unexpected subprocess'), create=True))
        stack.enter_context(patch.object(gate.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, '', '')))

    def fake_api(self, path, method='GET', payload=None, upload=None):
        self.calls.append((path, method, payload, upload))
        if '/releases/tags/' in path:
            return None  # The exact reported by-tag 404 / listed draft scenario.
        if method == 'POST' and path.endswith('/releases'):
            self.release.update(payload)
            self.releases = [self.release]
            if self.lose_create:
                raise gate.APIError('Lost response')
            return copy.deepcopy(self.release)
        if method == 'POST' and 'uploads.github.com/' in path:
            name = path.split('name=')[1]
            asset = dict(id=len(self.bytes) + 1, name=name, state='uploaded', size=upload.stat().st_size)
            self.release['assets'].append(asset)
            self.bytes[asset['id']] = b'corrupt' if self.corrupt_upload else upload.read_bytes()
            if self.lose_upload:
                raise gate.APIError('Lost upload response')
            return copy.deepcopy(asset)
        if method == 'PATCH':
            self.assertIn('/releases/448631718', path)
            if payload.get('draft') is False:
                self.assertEqual({a['name'] for a in self.release['assets']}, {'Matrix-Launcher.apk', 'Matrix-Launcher.apk.sha256'})
            self.release.update(payload)
            return copy.deepcopy(self.release)
        if '/assets?' in path:
            return copy.deepcopy(self.release['assets'])
        if '/releases?' in path:
            return copy.deepcopy(self.releases)
        if path.endswith('/releases/448631718'):
            return copy.deepcopy(self.release)
        raise AssertionError((path, method))

    def download(self, asset, destination):
        destination.write_bytes(self.bytes[asset['id']])

    def publish(self):
        gate.publish(self.meta, 'v2.7.6', 'abc', self.work)

    def test_stable_rollback_prevents_publication(self):
        self.releases.append(dict(tag_name='v2.7.7', draft=False, prerelease=False,
                                  body='Reviewed notes\n\nAndroid-Version-Code: 156'))
        with self.assertRaisesRegex(ValueError, 'rollback'):
            self.publish()
        self.assertTrue(all(call[1] == 'GET' for call in self.calls))

    def test_stable_rollback_rechecked_before_making_draft_public(self):
        def after_uploads(asset, destination):
            self.download(asset, destination)
            if len(self.release['assets']) == 2 and len(self.releases) == 1:
                self.releases.append(dict(tag_name='v2.7.7', draft=False, prerelease=False,
                                          body='Android-Version-Code: 156'))

        with patch.object(gate, 'download_asset', side_effect=after_uploads):
            with self.assertRaisesRegex(ValueError, 'rollback'):
                self.publish()
        self.assertTrue(self.release['draft'])
        self.assertEqual(len(self.release['assets']), 2)

    def test_listed_untagged_url_draft_uploads_and_publishes_by_id(self):
        self.release['html_url'] = 'https://github.com/bitmere/matrix-launcher/releases/untagged-2a6aca463bfb5e1dbc45'
        self.publish()
        self.assertFalse(self.release['draft'])
        uploads = [c for c in self.calls if c[3] is not None]
        self.assertEqual(len(uploads), 2)
        self.assertTrue(all('/releases/448631718/assets?' in c[0] for c in uploads))
        self.assertFalse(any('/tags/' in c[0] for c in self.calls))
        self.assertFalse(any(c[1] == 'DELETE' for c in self.calls))
        self.calls.clear()
        self.publish()
        self.assertTrue(all(c[1] == 'GET' for c in self.calls))

    def test_actual_pseudo_tag_is_rebound_without_deleting_draft(self):
        self.release['tag_name'] = 'untagged-2a6aca463bfb5e1dbc45'
        self.publish()
        self.assertEqual(self.release['tag_name'], 'v2.7.6')
        self.assertFalse(self.release['draft'])

    def test_create_captures_id_and_handles_lost_response(self):
        for lost in (False, True):
            with self.subTest(lost=lost):
                self.releases = []
                self.release['assets'] = []
                self.release['draft'] = True
                self.lose_create = lost
                self.publish()
                self.assertFalse(self.release['draft'])

    def test_lost_upload_response_reconciles_without_overwriting(self):
        self.lose_upload = True
        self.publish()
        self.assertEqual(len(self.release['assets']), 2)

    def test_corrupt_upload_prevents_publication(self):
        self.corrupt_upload = True
        with self.assertRaisesRegex(ValueError, 'never overwrite'):
            self.publish()
        self.assertTrue(self.release['draft'])

    def test_ambiguous_release_never_mutates(self):
        self.releases.append(dict(self.release, id=99))
        with self.assertRaisesRegex(ValueError, 'Ambiguous'):
            self.publish()
        self.assertTrue(all(c[1] == 'GET' for c in self.calls))

    def test_malformed_release_and_assets_fail_closed(self):
        for key, bad in [('body', None), ('id', None), ('draft', None), ('prerelease', 0), ('target_commitish', None)]:
            with self.subTest(key=key):
                original = self.release[key]
                self.release[key] = bad
                with self.assertRaises(ValueError):
                    self.publish()
                self.release[key] = original
        good = dict(id=1, name='Matrix-Launcher.apk', state='uploaded', size=3)
        for assets in [[dict(good, state='starter')], [dict(good, size=0)], [dict(good, id=None)], [good, good]]:
            self.release['assets'] = assets
            with self.subTest(assets=assets), self.assertRaises(ValueError):
                self.publish()

    def test_published_missing_asset_is_never_repaired_by_upload(self):
        self.release['draft'] = False
        with self.assertRaisesRegex(ValueError, 'missing required asset'):
            self.publish()
        self.assertTrue(all(c[1] == 'GET' for c in self.calls))

    def test_local_bytes_and_checksum_cannot_change_after_verification(self):
        for filename in gate.ASSETS:
            path = self.work / filename
            previous = path.read_bytes()
            path.write_bytes(b'{}')
            with self.subTest(filename=filename), self.assertRaises(ValueError):
                self.publish()
            path.write_bytes(previous)
        self.assertEqual(self.calls, [])

    def test_recovery_rejects_checksum_only_draft(self):
        self.release['assets'] = [dict(id=1, name='Matrix-Launcher.apk.sha256', state='uploaded', size=80)]
        with self.assertRaisesRegex(ValueError, 'checksum exists without APK'):
            gate.recover(self.meta, 'v2.7.6', 'abc', self.work)

class TransportAndParsingTest(unittest.TestCase):
    setUp = PublicationGateTest.setUp
    def response(self, status=200, body='{}', protocol='2.0', code=0):
        return subprocess.CompletedProcess([], code, f'HTTP/{protocol} {status}\r\nContent-Type: application/json\r\n\r\n{body}', '')

    def test_api_accepts_versions_statuses_json_and_intermediate_headers(self):
        for protocol in ('1.1', '2', '2.0'):
            with self.subTest(protocol=protocol), patch.object(gate.subprocess, 'run', return_value=self.response(protocol=protocol)) as call:
                self.assertEqual(gate.api('repos/bitmere/matrix-launcher/releases'), {})
                self.assertEqual(call.call_args.kwargs['timeout'], 300)
        response = self.response(status=201, body='{"id":1}')
        response.stdout = 'HTTP/1.1 100 Continue\r\n\r\n' + response.stdout
        with patch.object(gate.subprocess, 'run', return_value=response) as call:
            self.assertEqual(gate.api('repos/bitmere/matrix-launcher/releases', 'POST', {'draft': True}), {'id': 1})
            self.assertEqual(json.loads(call.call_args.kwargs['input']), {'draft': True})

    def test_reads_retry_transient_errors_writes_do_not(self):
        with patch.object(gate.subprocess, 'run', side_effect=[self.response(503, code=1), self.response()]) as call, patch.object(gate.time, 'sleep'):
            self.assertEqual(gate.api('path'), {})
            self.assertEqual(call.call_count, 2)
        with patch.object(gate.subprocess, 'run', return_value=self.response(503, code=1)) as call, self.assertRaises(ValueError):
            gate.api('path', 'POST', {})
        self.assertEqual(call.call_count, 1)

    def test_bad_api_shapes_and_statuses_fail_explicitly(self):
        for response in [self.response(403, code=1), self.response(body='invalid'), self.response(code=1),
                         subprocess.CompletedProcess([], 0, '{}', '')]:
            with self.subTest(response=response), patch.object(gate.subprocess, 'run', return_value=response), self.assertRaises(ValueError):
                gate.api('path')
        for response in (None, {}, [None]):
            with patch.object(gate, 'api', return_value=response), self.assertRaisesRegex(ValueError, 'list response'):
                gate.listing('path')

    def test_release_and_asset_lists_paginate(self):
        page = [dict(id=i) for i in range(100)]
        with patch.object(gate, 'api', side_effect=[page, [{'id': 101}]]) as call:
            self.assertEqual(len(gate.listing('path')), 101)
            self.assertEqual(call.call_args_list[1].args[0], 'path?per_page=100&page=2')

    def test_signer_repeats_badging_order_and_multiple_signers(self):
        pin = gate.PIN
        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            for name in ('apksigner', 'aapt'):
                (tools / name).touch()
            # A higher preview/incomplete installation must not be selected.
            preview = tools.parent / '99.0.0-rc1'
            preview.mkdir()
            (preview / 'apksigner').touch()
            apk = Path(directory) / 'fixture.apk'
            apk.write_bytes(b'fixture')
            certs = f'Signer #1 certificate SHA-256 digest: {pin.upper()}\nV2 Signer: certificate SHA-256 digest: {pin}\n'
            badging = "package: versionName='2.7.6' platformBuildVersionCode='36' name='com.matrix.launcher' versionCode='155'\n"
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin, ANDROID_HOME=directory), patch('sys.stdout', new_callable=io.StringIO):
                with patch.object(gate, 'run', side_effect=[certs, badging]) as run:
                    gate.verify_apk(self.meta, apk)
                    self.assertEqual(Path(run.call_args_list[0].args[0]).parent, tools)
                for output, badge, message in [(certs + 'Signer #2 certificate SHA-256 digest: ' + 'e' * 64, badging, 'signer differs'),
                                               (certs, badging + 'application-debuggable\n', 'Debuggable'),
                                               (certs, badging.replace('com.matrix.launcher', 'example.invalid'), 'package/version')]:
                    with patch.object(gate, 'run', side_effect=[output, badge]), self.assertRaisesRegex(ValueError, message):
                        gate.verify_apk(self.meta, apk)

    def test_download_requires_exact_declared_size(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, GITHUB_REPOSITORY=gate.REPO):
            def download(args, **kwargs):
                kwargs['stdout'].write(b'x')
            with patch.object(gate, 'command', side_effect=download), self.assertRaisesRegex(ValueError, 'size mismatch'):
                gate.download_asset(dict(id=1, size=2), Path(directory) / 'apk')



class WorkflowAndSigningTest(unittest.TestCase):
    def setUp(self):
        # Workflow copies its controlling files before checking out the version tag.
        control = Path(os.environ.get('RELEASE_WORK', '/nonexistent'))
        self.workflow = (control / 'release.yml' if (control / 'release.yml').is_file() else Path('.github/workflows/release.yml')).read_text()
        self.gradle = (control / 'build.gradle' if (control / 'build.gradle').is_file() else Path('app/build.gradle')).read_text()

    def python_block(self, label):
        section = self.workflow.split(label, 1)[1]
        lines = section.split("<<'PYTHON'\n", 1)[1].split('\n          PYTHON', 1)[0].splitlines()
        return '\n'.join(line[10:] for line in lines)

    def test_workflow_keeps_dispatch_permissions_tag_binding_and_current_gate_tests(self):
        self.assertIn('workflow_dispatch:', self.workflow)
        self.assertNotIn('push:\n', self.workflow)
        self.assertIn('cancel-in-progress: false', self.workflow)
        self.assertIn('contents: write', self.workflow)
        self.assertEqual(self.workflow.count('test "$DISPATCH_REF" = refs/heads/main'), 2)
        self.assertIn('test "$SOURCE_SHA" = "$VALIDATED_SOURCE_SHA"', self.workflow)
        self.assertIn('+refs/heads/main:refs/remotes/origin/main', self.workflow)
        self.assertEqual(self.workflow.count('python3 -m unittest discover -s "$RELEASE_WORK"'), 2)
        self.assertEqual(self.workflow.count('cp tools/release_gate.py tools/test_release_gate.py'), 2)
        self.assertIn('matrix-${{ github.job }}-validation-reports-${{ github.run_attempt }}', self.workflow)
        self.assertIn('matrix-launcher-release-${{ github.run_attempt }}', self.workflow)
        self.assertEqual(self.workflow.count('${{ github.run_attempt }}'), 3)
        self.assertIn('if: always()', self.workflow)
        self.assertIn('rm -f "$RELEASE_WORK/release.jks"', self.workflow)
        self.assertIn("GH_TOKEN: ''", self.workflow)
        self.assertIn('--no-configuration-cache', self.workflow)
        self.assertIn('mapping/release/mapping.txt', self.workflow)
        # runner is permitted at step level, never inside a job env mapping.
        for env in gate.re.split(r'^    env:\n', self.workflow, flags=gate.re.M)[1:]:
            self.assertNotIn('runner.', env.split('    steps:', 1)[0])

    def test_restore_overrides_store_path_and_terminates_java_continuations(self):
        code = self.python_block('Restore ephemeral signing material')
        compile(code, '<restore>', 'exec')
        for props in ('storeFile=old\nkeyAlias=fixture', 'storeFile:old\nkeyAlias=fixture\\', 'storeFile=old\r\nkeyAlias=fixture\r\n'):
            with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, RELEASE_WORK=directory, KEYSTORE='Zml4dHVyZQ==', PROPS=props):
                exec(code, {})
                self.assertEqual((Path(directory) / 'release.jks').read_bytes(), b'fixture')
                restored = (Path(directory) / 'keystore.properties').read_text()
                self.assertTrue(restored.endswith(f'\n\nstoreFile={directory}/release.jks\n'))
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, RELEASE_WORK=directory, KEYSTORE='invalid!', PROPS='fixture'), self.assertRaises(ValueError):
            exec(code, {})

    def test_signing_validation_is_explicit_and_no_debug_fallback_exists(self):
        self.assertIn("System.getenv('GITHUB_ACTIONS') != 'true'", self.gradle)
        self.assertIn("['storeFile', 'storePassword', 'keyAlias', 'keyPassword'].any", self.gradle)
        self.assertIn('Release signing keystore is missing', self.gradle)
        self.assertIn('signingConfig keyFile ? signingConfigs.release : null', self.gradle)
        self.assertNotIn('signingConfigs.debug', self.gradle)
        self.assertIn('minifyEnabled = true', self.gradle)
        self.assertIn('shrinkResources = true', self.gradle)

    def test_workflow_pin_requires_full_rotation_and_accepts_exact_pin(self):
        code = self.python_block('Check signing configuration')
        for pin in ('', gate.PIN[:8], 'a' * 64):
            with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=pin), self.assertRaises(AssertionError):
                exec(code, {})
        with patch.dict(os.environ, MATRIX_RELEASE_CERT_SHA256=gate.PIN):
            exec(code, {})


class MetadataExtraTest(unittest.TestCase):
    def test_duplicate_missing_and_malformed_version_properties(self):
        with tempfile.TemporaryDirectory() as directory:
            old = Path.cwd()
            try:
                os.chdir(directory)
                for props in ('versionName=2.7.6\nversionName=2.7.6', 'versionCode=155', 'invalid'):
                    Path('version.properties').write_text(props)
                    with self.subTest(props=props), self.assertRaises(ValueError):
                        gate.metadata('v2.7.6')
            finally:
                os.chdir(old)

    def test_beta_is_prerelease_and_properties_allow_comments_and_spacing(self):
        with tempfile.TemporaryDirectory() as directory:
            old = Path.cwd()
            try:
                os.chdir(directory)
                Path('version.properties').write_text('  # comment\n! comment\nversionName = 2.7.6-beta.1\nversionCode=155\nreleaseChannel=beta\n')
                Path('CHANGELOG.md').write_text('## 2.7.6-beta.1\nReviewed beta\n')
                self.assertTrue(gate.metadata('v2.7.6-beta.1')['prerelease'])
            finally:
                os.chdir(old)



class ExtraTransportTest(unittest.TestCase):
    def test_connection_loss_without_headers_retries_reads_and_reconciles_writes(self):
        lost = subprocess.CompletedProcess([], 1, '', 'connection lost')
        okay = subprocess.CompletedProcess([], 0, 'HTTP/2.0 200 OK\n\n[]', '')
        with patch.object(gate.subprocess, 'run', side_effect=[lost, okay]) as call, patch.object(gate.time, 'sleep'):
            self.assertEqual(gate.api('path'), [])
            self.assertEqual(call.call_count, 2)
        with patch.object(gate.subprocess, 'run', return_value=lost) as call, self.assertRaises(gate.APIError):
            gate.api('path', 'POST', {})
        self.assertEqual(call.call_count, 1)

    def test_upload_sets_content_length_and_binary_input(self):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / 'fixture.apk'
            apk.write_bytes(b'fixture')
            response = subprocess.CompletedProcess([], 0, 'HTTP/2.0 201 Created\n\n{"id":1}', '')
            with patch.object(gate.subprocess, 'run', return_value=response) as call:
                gate.api('https://uploads.github.com/repos/bitmere/matrix-launcher/releases/1/assets?name=fixture.apk', 'POST', upload=apk)
            args = call.call_args.args[0]
            self.assertIn('Content-Length: 7', args)
            self.assertIn('Content-Type: application/octet-stream', args)
            self.assertEqual(args[args.index('--input') + 1], str(apk))

    def test_read_timeouts_retry_but_mutation_timeout_propagates(self):
        response = subprocess.CompletedProcess([], 0, 'HTTP/2.0 200 OK\n\n[]', '')
        with patch.object(gate.subprocess, 'run', side_effect=[subprocess.TimeoutExpired('gh', 300), response]) as call, patch.object(gate.time, 'sleep'):
            self.assertEqual(gate.api('path'), [])
            self.assertEqual(call.call_count, 2)
        with patch.object(gate.subprocess, 'run', side_effect=subprocess.TimeoutExpired('gh', 300)) as call, self.assertRaises(subprocess.TimeoutExpired):
            gate.api('path', 'PATCH', {})
        self.assertEqual(call.call_count, 1)


class CommandFlowTest(unittest.TestCase):
    def test_prepare_recover_verify_publish_anonymous_entrypoints(self):
        with tempfile.TemporaryDirectory() as directory:
            old = Path.cwd()
            work = Path(directory) / 'work'
            try:
                os.chdir(directory)
                Path('version.properties').write_text('versionName=2.7.6\nversionCode=155\nreleaseChannel=stable\n')
                Path('CHANGELOG.md').write_text('## 2.7.6\nReviewed notes\n')
                env = dict(RELEASE_TAG='v2.7.6', SOURCE_SHA='abc', RELEASE_WORK=str(work),
                           GITHUB_REPOSITORY='bitmere/matrix-launcher', GITHUB_OUTPUT=str(Path(directory) / 'output'),
                           GITHUB_STEP_SUMMARY=str(Path(directory) / 'summary'))
                with patch.dict(os.environ, env), ExitStack() as stack:
                    state = stack.enter_context(patch.object(gate, 'release_state', return_value=None))
                    verify = stack.enter_context(patch.object(gate, 'verify_apk'))
                    publish = stack.enter_context(patch.object(gate, 'publish'))
                    anon = stack.enter_context(patch.object(gate, 'anonymous'))
                    for command in ('prepare', 'recover', 'verify', 'publish', 'anonymous'):
                        if command == 'verify':
                            (work / 'Matrix-Launcher.apk').write_bytes(b'optimized-fixture')
                        with patch.object(sys, 'argv', ['release_gate.py', command]):
                            gate.main()
                    self.assertIn('Android-Version-Code: 155', (work / 'notes.md').read_text())
                    self.assertEqual(Path(env['GITHUB_OUTPUT']).read_text(), 'reuse=false\n')
                    verify.assert_called_once()
                    publish.assert_called_once()
                    anon.assert_called_once()
            finally:
                os.chdir(old)



class OriginalBehaviorRegressionTest(unittest.TestCase):
    setUp = PublicationGateTest.setUp

    def test_list_discovery_works_when_tag_endpoint_returns_404(self):
        release = dict(id=448631718, draft=True, prerelease=False, tag_name='v2.7.6', target_commitish='abc',
                       name='Matrix Launcher v2.7.6', body=self.meta['notes'], assets=[])
        def fixture_api(path, *args, **kwargs):
            if '/tags/' in path:
                return None
            if '/assets?' in path:
                return []
            return [release]
        with patch.dict(os.environ, GITHUB_REPOSITORY=gate.REPO), patch.object(gate, 'api', side_effect=fixture_api), patch.object(gate, 'run', return_value='abc'):
            self.assertIsNotNone(gate.release_state(self.meta, 'v2.7.6', 'abc'))

    def test_null_notes_fail_with_explicit_release_validation(self):
        release = dict(id=1, draft=True, prerelease=False, tag_name='v2.7.6', target_commitish='abc',
                       name='Matrix Launcher v2.7.6', body=None, assets=[])
        def fixture_api(path, *args, **kwargs):
            return release if '/tags/' in path else [release]
        with patch.dict(os.environ, GITHUB_REPOSITORY=gate.REPO), patch.object(gate, 'api', side_effect=fixture_api), patch.object(gate, 'run', return_value='abc'), self.assertRaises(ValueError):
            gate.release_state(self.meta, 'v2.7.6', 'abc')

    def test_get_transient_failure_recovers(self):
        responses = [subprocess.CompletedProcess([], 1, 'HTTP/2.0 503 Unavailable\n\n{}', ''),
                     subprocess.CompletedProcess([], 0, 'HTTP/2.0 200 OK\n\n[]', '')]
        with patch.object(gate.subprocess, 'run', side_effect=responses), patch('time.sleep'):
            self.assertEqual(gate.api('repos/bitmere/matrix-launcher/releases'), [])

    def test_aapt_fields_can_appear_in_another_order(self):
        pin = gate.PIN
        with tempfile.TemporaryDirectory() as directory:
            tools = Path(directory) / 'build-tools' / '36.0.0'
            tools.mkdir(parents=True)
            (tools / 'apksigner').touch()
            (tools / 'aapt').touch()
            apk = Path(directory) / 'fixture.apk'
            apk.write_bytes(b'fixture')
            certs = f'Signer #1 certificate SHA-256 digest: {pin}\n'
            badging = "package: versionName='2.7.6' name='com.matrix.launcher' versionCode='155'\n"
            with patch.dict(os.environ, ANDROID_HOME=directory, MATRIX_RELEASE_CERT_SHA256=pin), patch.object(gate, 'run', side_effect=[certs, badging]), patch('sys.stdout', new_callable=io.StringIO):
                gate.verify_apk(self.meta, apk)


class PublicReleaseTest(unittest.TestCase):
    setUp = PublicationGateTest.setUp

    def test_anonymous_compares_public_metadata_and_both_assets(self):
        tag = 'v2.7.6'
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            apk = b'optimized APK'
            sha = gate.hashlib.sha256(apk).hexdigest()
            blobs = dict(zip(gate.ASSETS, (apk, gate.checksum_line(sha))))
            assets = []
            for name, blob in blobs.items():
                (work / name).write_bytes(blob)
                assets.append(dict(name=name, size=len(blob), state='uploaded',
                                   browser_download_url=f'https://github.com/{gate.REPO}/releases/download/{tag}/{name}'))
            release = dict(tag_name=tag, name=f'Matrix Launcher {tag}', draft=False, prerelease=False,
                           body=self.meta['notes'], assets=assets)
            with patch.object(gate, 'public_bytes', side_effect=[json.dumps(release).encode(), *blobs.values()]) as fetch:
                self.assertEqual(gate.anonymous(self.meta, tag, work), sha)
                self.assertEqual(fetch.call_args_list[0].args[0],
                                 f'https://api.github.com/repos/{gate.REPO}/releases/tags/{tag}')
            for bad in (dict(release, draft=True), dict(release, body='missing trailer'),
                        dict(release, assets=assets[:1]), dict(release, assets=assets + assets[:1]),
                        dict(release, assets=[dict(assets[0], size=1), assets[1]])):
                with patch.object(gate, 'public_bytes', return_value=json.dumps(bad).encode()), self.assertRaises(ValueError):
                    gate.anonymous(self.meta, tag, work)
            for public_apk, public_checksum in ((b'x' * len(apk), blobs[gate.CHECKSUM]),
                                               (apk, b'x' * gate.CHECKSUM_BYTES)):
                with patch.object(gate, 'public_bytes', side_effect=[json.dumps(release).encode(), public_apk, public_checksum]):
                    with self.assertRaisesRegex(ValueError, 'Public asset bytes differ'):
                        gate.anonymous(self.meta, tag, work)

    def test_checksum_requires_exact_lowercase_spacing_and_lf(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            (work / gate.APK).write_bytes(b'bytes')
            sha = gate.digest(work / gate.APK)
            line = gate.checksum_line(sha)
            for bad in (line.rstrip(), line.replace(b'\n', b'\r\n'), line.replace(b'  ', b' '), line.upper()):
                (work / gate.CHECKSUM).write_bytes(bad)
                with self.assertRaisesRegex(ValueError, 'Local checksum differs'):
                    gate.verified_checksum(work)
            (work / gate.CHECKSUM).write_bytes(line)
            self.assertEqual(gate.verified_checksum(work), sha)


if __name__ == '__main__':
    unittest.main()



