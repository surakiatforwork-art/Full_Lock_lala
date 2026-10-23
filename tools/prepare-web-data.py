"""Refresh the static, attributed tambon map. Run with Python + requests."""
import json, pathlib, hashlib, requests
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'web/data'
OUT.mkdir(parents=True, exist_ok=True)
sources = []
def get(url):
    response = requests.get(url, timeout=120)
    response.raise_for_status()
    sources.append({'url': url, 'sha256': hashlib.sha256(response.content).hexdigest()})
    return response.content.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
def revision(repo, branch):
    return json.loads(get(f'https://api.github.com/repos/{repo}/commits/{branch}'))['sha']
geo = revision('GeoThai/data', 'main')
base = revision('piyayut-ch/mapthai', 'master')
prefix = f'https://raw.githubusercontent.com/GeoThai/data/{geo}/'
provinces, districts, areas = [json.loads(get(prefix + 'json/' + name + '.json')) for name in ['provinces', 'districts', 'subdistricts']]
features = json.loads(get(f'https://raw.githubusercontent.com/piyayut-ch/mapthai/{base}/data-raw/geojson/th_adm3.geojson'))['features']
bkk = json.loads(get('https://traffy.longdo.com/data/subdistricts.json'))['features']
geometry = {f['properties']['ADM3_PCODE'].removeprefix('TH'): f for f in features if not f['properties']['ADM3_PCODE'].startswith('TH10')}
geometry.update({str(f['properties']['id']): f for f in bkk})
# Tha Faek transferred district; retain its original polygon, verify unique name/province.
for area in areas:
    if area['code'] not in geometry:
        matches = ([f for f in bkk if f['properties']['name_t'] == area['name_th']] if area['province_code'] == '10' else [f for f in features if f['properties']['ADM3_TH'] == area['name_th'] and f['properties']['ADM1_PCODE'] == 'TH' + area['province_code']])
        assert len(matches) == 1, ('Unresolved boundary', area)
        geometry[area['code']] = matches[0]
def points(coords):
    if isinstance(coords[0], (float, int)):
        yield coords
    else:
        for item in coords:
            yield from points(item)
def rounded(value):
    return [rounded(v) for v in value] if isinstance(value, list) else round(value, 5)
catalog = {'provinces': [{'id': p['code'], 'name': p['name_th']} for p in provinces], 'districts': [{'id': d['code'], 'name': d['name_th'], 'province': d['province_code']} for d in districts], 'areas': []}
for province in provinces:
    group = []
    for area in areas:
        if area['province_code'] != province['code']:
            continue
        shape = geometry[area['code']]['geometry']
        xy = list(points(shape['coordinates']))
        bounds = [min(p[1] for p in xy), min(p[0] for p in xy), max(p[1] for p in xy), max(p[0] for p in xy)]
        catalog['areas'].append({'id': area['code'], 'name': area['name_th'], 'district': area['district_code'], 'province': area['province_code'], 'bounds': [round(v, 5) for v in bounds]})
        group.append({'type': 'Feature', 'properties': {'id': area['code']}, 'geometry': {'type': shape['type'], 'coordinates': rounded(shape['coordinates'])}})
    write(OUT / 'provinces' / (province['code'] + '.geojson'), {'type': 'FeatureCollection', 'features': group})
assert len(catalog['areas']) == 7436 and len({a['id'] for a in catalog['areas']}) == 7436
write(OUT / 'catalog.json', catalog)
for name, url in [('geothai-CC-BY-4.0.txt', prefix + 'LICENSE'), ('geothai-NOTICE.txt', prefix + 'NOTICE'), ('mapthai-MIT.txt', f'https://raw.githubusercontent.com/piyayut-ch/mapthai/{base}/LICENSE.md')]:
    (OUT / name).write_text(get(url).rstrip() + '\n', encoding='utf-8')
write(OUT / 'provenance.json', {'sources': sources, 'catalogCount': len(areas), 'note': 'Country reference boundaries: RTSD/OCHA 2019 via mapthai; Bangkok: Traffy Fondue, retrieved 2026-10-23. Tha Faek matched by same name and province after district transfer.', 'bangkokReuseTerms': 'https://traffy.longdo.com/about', 'bangkokCredit': 'https://bangkok.traffy.in.th'})
vendor = ROOT / 'web/vendor/leaflet'
vendor.mkdir(parents=True, exist_ok=True)
for name in ['leaflet.js', 'leaflet.css']:
    (vendor / name).write_text(get('https://unpkg.com/leaflet@1.9.4/dist/' + name), encoding='utf-8')
(vendor / 'LICENSE').write_text(get('https://unpkg.com/leaflet@1.9.4/LICENSE'), encoding='utf-8')
(vendor / 'images').mkdir(exist_ok=True)
for name in ['layers.png', 'layers-2x.png', 'marker-icon.png', 'marker-icon-2x.png', 'marker-shadow.png']:
    response = requests.get('https://unpkg.com/leaflet@1.9.4/dist/images/' + name, timeout=60)
    response.raise_for_status()
    (vendor / 'images' / name).write_bytes(response.content)
assets = ROOT / 'web/assets'
assets.mkdir(parents=True, exist_ok=True)
(assets / 'app-icon.png').write_bytes((ROOT / 'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png').read_bytes())
print('Prepared', len(areas), 'areas in', len(provinces), 'province files')
