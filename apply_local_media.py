#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Install the horizontal Local tab and offline player into NewPipe v0.29.1.

Usage: python3 apply_local_media.py NewPipe
The script works alongside the existing apply_sleep_timer.py and does not
replace any of its changes. All modifications are validated before writing.
"""
from __future__ import annotations

import difflib
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT_ASSETS = Path(__file__).resolve().parent / 'features' / 'local_media'
JAVA_BASE = 'app/src/main/java/org/schabi/newpipe/localmedia/'
XML_BASE = 'app/src/main/res/layout/'


def replace_once(text: str, before: str, after: str, name: str) -> str:
    n = text.count(before)
    if n != 1:
        raise ValueError(f'{name}: expected 1 anchor; found {n}: {before[:85]!r}. '
                         'Source must match NewPipe v0.29.1.')
    return text.replace(before, after, 1)


def patch_main_activity(s: str, name: str) -> str:
    if 'ITEM_ID_LOCAL_MEDIA' in s:
        raise ValueError(f'{name}: already contains local media support')
    s = replace_once(s,
                     '    private static final int ITEM_ID_HISTORY = -5;',
                     '    private static final int ITEM_ID_HISTORY = -5;\n'
                     '    private static final int ITEM_ID_LOCAL_MEDIA = -6;', name)
    s = replace_once(s,
                     '                .setIcon(R.drawable.ic_history);',
                     '                .setIcon(R.drawable.ic_history);\n'
                     '        drawerLayoutBinding.navigation.getMenu()\n'
                     '                .add(R.id.menu_tabs_group, ITEM_ID_LOCAL_MEDIA, ORDER,\n'
                     '                        R.string.local_media_title)\n'
                     '                .setIcon(R.drawable.ic_music_note);', name)
    s = replace_once(s,
                     '            case ITEM_ID_HISTORY:\n'
                     '                NavigationHelper.openStatisticFragment(getSupportFragmentManager());\n'
                     '                break;',
                     '            case ITEM_ID_HISTORY:\n'
                     '                NavigationHelper.openStatisticFragment(getSupportFragmentManager());\n'
                     '                break;\n'
                     '            case ITEM_ID_LOCAL_MEDIA:\n'
                     '                startActivity(new Intent(this,\n'
                     '                        org.schabi.newpipe.localmedia.LocalMediaActivity.class));\n'
                     '                break;', name)
    return s


def patch_tab_class(s: str, name: str) -> str:
    s = replace_once(s,
                     'import org.schabi.newpipe.local.playlist.LocalPlaylistFragment;',
                     'import org.schabi.newpipe.local.playlist.LocalPlaylistFragment;\n'
                     'import org.schabi.newpipe.localmedia.LocalMediaFragment;', name)
    # New tab always appears in the main ViewPager, without mutating saved tabs.
    anchor = '    public static class HistoryTab extends Tab {'
    s = replace_once(s, anchor,
                     '    /** Device media tab; reserved id outside regular saved-tab ids. */\n'
                     '    public static class LocalMediaTab extends Tab {\n'
                     '        public static final int ID = 80;\n'
                     '        @Override public int getTabId() { return ID; }\n'
                     '        @Override public String getTabName(final Context context) {\n'
                     '            return context.getString(R.string.local_media_tab_title);\n'
                     '        }\n'
                     '        @DrawableRes @Override public int getTabIconRes(final Context context) {\n'
                     '            return R.drawable.ic_music_note;\n'
                     '        }\n'
                     '        @Override public Fragment getFragment(final Context context) {\n'
                     '            return new LocalMediaFragment();\n'
                     '        }\n'
                     '    }\n\n' + anchor, name)
    return s


def patch_main_fragment(s: str, name: str) -> str:
    s = replace_once(s,
                     '        tabsList.addAll(tabsManager.getTabs());',
                     '        tabsList.addAll(tabsManager.getTabs());\n'
                     '        // Keep Local on the horizontal bar even after users reorder tabs.\n'
                     '        tabsList.add(Math.min(2, tabsList.size()), new Tab.LocalMediaTab());',
                     name)
    s = replace_once(s,
                     '                tabToSet.setContentDescription(tab.getTabName(requireContext()));',
                     '                tabToSet.setContentDescription(tab.getTabName(requireContext()));\n'
                     '                if (tab instanceof Tab.LocalMediaTab) {\n'
                     '                    tabToSet.setText(R.string.local_media_tab_title);\n'
                     '                }', name)
    return s


def patch_manifest(s: str, name: str) -> str:
    if '.localmedia.LocalMediaActivity' in s:
        raise ValueError(f'{name}: already contains local media support')
    perm = '''    <!-- Read indexed music and videos only: no all-files access needed. -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
'''
    s = replace_once(s, '    <uses-permission android:name="android.permission.INTERNET" />',
                     perm + '    <uses-permission android:name="android.permission.INTERNET" />',
                     name)
    activities = '''        <activity
            android:name=".localmedia.LocalMediaActivity"
            android:exported="false"
            android:label="@string/local_media_title" />
        <activity
            android:name=".localmedia.LocalMediaPlayerActivity"
            android:exported="false"
            android:resizeableActivity="true"
            android:supportsPictureInPicture="true"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
            android:label="@string/local_media_title" />
        <service
            android:name=".localmedia.LocalMediaPlaybackService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />

'''
    s = replace_once(s, '        <activity\n            android:name=".MainActivity"',
                     activities + '        <activity\n            android:name=".MainActivity"', name)
    ET.fromstring(s)
    return s


def patch_strings(s: str, name: str, fragment: str) -> str:
    if 'name="local_media_title"' in s:
        raise ValueError(f'{name}: local media strings already installed')
    insert = (ROOT_ASSETS / fragment).read_text(encoding='utf-8')
    result = replace_once(s, '</resources>', insert + '</resources>', name)
    ET.fromstring(result)
    return result


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit('Usage: python3 apply_local_media.py <NewPipe source root>')
    root = Path(sys.argv[1]).expanduser().resolve()
    files = {
        'app/src/main/java/org/schabi/newpipe/MainActivity.java': patch_main_activity,
        'app/src/main/java/org/schabi/newpipe/fragments/MainFragment.java': patch_main_fragment,
        'app/src/main/java/org/schabi/newpipe/settings/tabs/Tab.java': patch_tab_class,
        'app/src/main/AndroidManifest.xml': patch_manifest,
        'app/src/main/res/values/strings.xml':
            lambda s, name: patch_strings(s, name, 'strings-en.xml.part'),
        'app/src/main/res/values-vi/strings.xml':
            lambda s, name: patch_strings(s, name, 'strings-vi.xml.part'),
    }
    originals = {}
    updates = {}
    for rel, transform in files.items():
        path = root / rel
        if not path.is_file():
            raise SystemExit(f'Missing file in NewPipe: {path}')
        originals[rel] = path.read_text(encoding='utf-8')
        updates[rel] = transform(originals[rel], rel)
    for filename in ('LocalMediaItem.java', 'LocalMediaRepository.java',
                     'LocalMediaActivity.java', 'LocalMediaFragment.java',
                     'LocalMediaPlayerActivity.java',
                     'LocalMediaPlaybackService.java'):
        rel = JAVA_BASE + filename
        if (root / rel).exists():
            raise SystemExit(f'Refusing to overwrite: {root / rel}')
        updates[rel] = (ROOT_ASSETS / filename).read_text(encoding='utf-8')
    for filename in ('activity_local_media.xml', 'activity_local_media_host.xml',
                     'row_local_media.xml', 'activity_local_media_player.xml'):
        rel = XML_BASE + filename
        if (root / rel).exists():
            raise SystemExit(f'Refusing to overwrite: {root / rel}')
        updates[rel] = (ROOT_ASSETS / filename).read_text(encoding='utf-8')
        ET.fromstring(updates[rel])
    for filename in ('local_media_icon_bg.xml', 'local_media_audio_bg.xml'):
        rel = 'app/src/main/res/drawable/' + filename
        if (root / rel).exists():
            raise SystemExit(f'Refusing to overwrite: {root / rel}')
        updates[rel] = (ROOT_ASSETS / filename).read_text(encoding='utf-8')
        ET.fromstring(updates[rel])
    # Commit only after all expected anchors and XML files pass validation.
    changes = []
    for rel, content in updates.items():
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        changes.extend(difflib.unified_diff(
            originals.get(rel, '').splitlines(keepends=True),
            content.splitlines(keepends=True),
            fromfile='a/' + rel if rel in originals else '/dev/null',
            tofile='b/' + rel,
        ))
    (root / 'local_media_changes.diff').write_text(''.join(changes), encoding='utf-8')
    print(f'Installed local audio/video playback: {len(updates)} files updated/added.')
    print('Preserved existing sleep timer files and changes.')


if __name__ == '__main__':
    main()
