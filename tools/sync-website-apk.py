"""Copy the latest stable GitHub APK into the public static site. Requires gh auth on the publisher only."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPO = 'surakiatforwork-art/Full_Lock_lala'

def main():
    result = subprocess.run(['gh', 'api', f'repos/{REPO}/releases/latest'], check=True, capture_output=True, text=True, encoding='utf-8')
    release = json.loads(result.stdout)
    if release['draft'] or release['prerelease'] or not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+', release['tag_name']):
        raise ValueError('Expected a published stable release')
    name = 'FullLock-' + release['tag_name'] + '.apk'
    asset = next((a for a in release['assets'] if a['name'] == name), None)
    if asset is None:
        raise ValueError('Release APK missing')
    with tempfile.TemporaryDirectory(prefix='full-lock-download-') as directory:
        subprocess.run(['gh', 'release', 'download', release['tag_name'], '--repo', REPO, '--pattern', name, '--pattern', 'SHA256SUMS.txt', '--dir', directory], check=True)
        apk = Path(directory) / name
        checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
        expected = [line.split() for line in (Path(directory) / 'SHA256SUMS.txt').read_text(encoding='ascii').splitlines()]
        if not any(len(parts) == 2 and parts[0].lower() == checksum and parts[1].lstrip('*') == name for parts in expected):
            raise ValueError('APK checksum mismatch')
        if asset.get('digest') and asset['digest'] != 'sha256:' + checksum:
            raise ValueError('Release asset digest mismatch')
        with zipfile.ZipFile(apk) as archive:
            if 'AndroidManifest.xml' not in archive.namelist():
                raise ValueError('Not an APK')
        output = ROOT / 'web/downloads'
        output.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(apk, output / 'FullLock.apk')
        (output / 'SHA256SUMS.txt').write_text(checksum + '  FullLock.apk\n', encoding='ascii')
        (output / 'version.json').write_text(json.dumps({'version': release['tag_name'], 'sha256': checksum, 'bytes': apk.stat().st_size}, indent=2) + '\n', encoding='utf-8')
        print('Website APK synchronized:', release['tag_name'], checksum)

if __name__ == '__main__':
    main()
