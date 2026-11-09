#!/usr/bin/env python3
"""Static checks only: not a Gradle compile or Android runtime test."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'NewPipe')
main = root / 'app/src/main'
res = main / 'res'
source = main / 'java/org/schabi/newpipe'

for xml in res.rglob('*.xml'):
    ET.parse(xml)
ET.parse(main / 'AndroidManifest.xml')

java = list((source / 'localmedia').glob('*.java'))
assert len(java) == 6, f'Expected 6 local Java source files, found {len(java)}'
assert 'tabsList.add(Math.min(2, tabsList.size()), new Tab.LocalMediaTab())' in (
    source / 'fragments/MainFragment.java').read_text(encoding='utf-8')
assert 'class LocalMediaTab extends Tab' in (
    source / 'settings/tabs/Tab.java').read_text(encoding='utf-8')

layout_defs = set()
for xml in (res / 'layout').glob('*.xml'):
    text = xml.read_text(encoding='utf-8')
    layout_defs.update(re.findall(r'android:id="@\+id/(\w+)"', text))

langs = {}
for lang in ['values', 'values-vi']:
    tree = ET.parse(res / lang / 'strings.xml')
    names = [item.get('name') for item in tree.getroot() if item.get('name')]
    assert len(names) == len(set(names)), f'Duplicate name in {lang}'
    langs[lang] = set(names)

for src in java:
    text = src.read_text(encoding='utf-8')
    resources = set(re.findall(r'R\.string\.(\w+)', text))
    ids = set(re.findall(r'R\.id\.(\w+)', text))
    assert resources <= langs['values'], f'Missing EN strings in {src}: {resources - langs["values"]}'
    assert resources <= langs['values-vi'], f'Missing VI strings in {src}: {resources - langs["values-vi"]}'
    assert ids <= layout_defs, f'Missing layout IDs in {src}: {ids - layout_defs}'

for name in ['local_media_tab_title', 'local_media_audio', 'local_media_video',
             'local_media_open_file', 'local_media_timer',
             'local_media_shuffle_on', 'local_media_repeat_all']:
    assert all(name in group for group in langs.values()), name

player = (source / 'localmedia/LocalMediaPlayerActivity.java').read_text()
service = (source / 'localmedia/LocalMediaPlaybackService.java').read_text()
for method in ('togglePlayback(', 'toggleShuffle(', 'cycleRepeat(', 'seekTo(',
               'setSleepTimerMinutes('):
    assert method in service, f'Missing playback method {method}'
assert 'enterPictureInPictureMode(' in player
print(f'PASS: {len(java)} Java source files, EN/VI resources, local tab, layouts and manifest.')
print('Static analysis only; compile using GitHub Actions to confirm toolchain compatibility.')
