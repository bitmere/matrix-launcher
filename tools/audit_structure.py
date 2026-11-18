"""Check independent repository/component boundaries without an Android device."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

def gradle_namespace(text):
    match = re.search(r"namespace\s*(?:=\s*)?['\"]([^'\"]+)['\"]", text)
    assert match, 'Missing Android namespace'
    return match[1]


def declared_classes(text):
    package = re.search(r'^package ([A-Za-z0-9_.]+)', text, re.M)
    assert package, 'Missing source package'
    return {package[1] + '.' + match[1] for match in re.finditer(
        r'^(?:(?:public|internal|private|open|abstract|data|sealed|final)\s+)*(?:class|object|interface)\s+(\w+)', text, re.M)}


def component_available(name, namespace, qualified):
    full_name = namespace + name if name.startswith('.') else name
    return full_name in qualified or full_name == 'androidx.core.content.FileProvider'


def audit(root):
    manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml')
    android = '{http://schemas.android.com/apk/res/android}'
    app = manifest.getroot().find('application')
    sources = [p for p in (root / 'app/src/main/java').rglob('*') if p.suffix in {'.java', '.kt'}]
    classes = {p.stem for p in sources}
    qualified = {}
    for source in sources:
        for name in declared_classes(source.read_text(encoding='utf-8')):
            qualified[name] = source
    namespace = gradle_namespace((root / 'app/build.gradle').read_text())
    assert namespace == 'com.matrix.launcher'
    def full(name):
        return namespace + name if name.startswith('.') else name
    components = {e.get(android + 'name'): e for e in app if e.tag in {'activity', 'service', 'receiver', 'provider'}}
    legacy_services = {'com.gathin.tvhome.' + name for name in ('HomeKeyService', 'InputKeyService', 'GuardRevival', 'ClockDream')}
    required = {'.ComposeHomeActivity', '.HomeRecovery', '.InputsRequestActivity', '.AppLaunchActivity', '.QuickSettingsActivity', '.ClockActivity', '.WallpaperImportActivity'} | legacy_services
    assert required <= components.keys(), 'Required component declaration is missing'
    for name in components:
        assert component_available(name, namespace, qualified), f'Manifest component has no implementation: {name}'
    assert full(app.get(android + 'name')) in qualified
    for name in legacy_services:
        bridge = qualified[name].read_text(encoding='utf-8')
        assert 'extends com.matrix.launcher.' + name.rsplit('.', 1)[-1] + ' { }' in bridge
    for name in ('HomeKeyService', 'InputKeyService'):
        element = components['com.gathin.tvhome.' + name]
        assert element.get(android + 'permission') == 'android.permission.BIND_ACCESSIBILITY_SERVICE'
        assert any(e.get(android + 'name') == 'android.accessibilityservice' for e in element.findall('meta-data'))
    aliases = {e.get(android + 'name'): e for e in app.findall('activity-alias')}
    for name, alias in aliases.items():
        assert alias.get(android + 'targetActivity') in components
    home = aliases['com.gathin.tvhome.ComposeHomeActivity']
    assert home.get(android + 'targetActivity') == '.ComposeHomeActivity'
    assert any(n.get(android + 'name') == 'android.intent.category.HOME' for f in home.findall('intent-filter') for n in f.findall('category'))
    assert sum(any(n.get(android + 'name') == 'android.intent.category.HOME' for f in e.findall('intent-filter') for n in f.findall('category')) for e in [*components.values(), *aliases.values()]) == 1
    wallpaper = components['.WallpaperImportActivity']
    assert wallpaper.get(android + 'exported') == 'true'
    assert all(d.get(android + 'mimeType') == 'image/*' for f in wallpaper.findall('intent-filter') for d in f.findall('data'))
    permissions = {p.get(android + 'name'): p for p in manifest.getroot().findall('uses-permission')}
    assert 'android.permission.MANAGE_EXTERNAL_STORAGE' not in permissions
    assert 'android.permission.READ_MEDIA_VIDEO' not in permissions
    assert permissions['android.permission.READ_EXTERNAL_STORAGE'].get(android + 'maxSdkVersion') == '32'
    assert 'android.permission.READ_MEDIA_VISUAL_USER_SELECTED' in permissions
    retired = {'MainActivity', 'PersonalHomeActivity', 'AppAdapter', 'LauncherSettingsActivity', 'GlassFocus', 'SkyView'}
    assert not classes & retired, 'A retired Java UI implementation was reintroduced'
    for source in (root / 'core/src/main').rglob('*.java'):
        assert not re.search(r'^import (?:android|androidx)\.', source.read_text(encoding='utf-8'), re.M), f'Android dependency in core: {source}'
    for source in (root / 'app/src/main/java').rglob('*.kt'):
        assert 'import androidx.compose.material3.' not in source.read_text(encoding='utf-8'), 'Use TV Material 3, not the mobile component library'
    # Raw button styling must not spread to feature screens again.
    for source in (root / 'app/src/main/java').rglob('*.kt'):
        body = source.read_text(encoding='utf-8')
        if source.name != 'MatrixButton.kt':
            assert not re.search(r'^import androidx\.tv\.material3\.(?:Button|ButtonDefaults|IconButton|IconButtonDefaults)$', body, re.M), f'Use shared MatrixButton: {source}'
    print(json.dumps({'status': 'passed', 'manifest_components': len(components), 'app_classes': len(classes), 'core_android_free': True}))


if __name__ == '__main__':
    audit(Path(__file__).resolve().parent.parent)
