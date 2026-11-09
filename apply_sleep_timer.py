#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Them tinh nang Hen gio dung phat cho NewPipe v0.29.1 (GPL-3.0-or-later).

FIX 2026-11-08: chon caption FrameLayout bang thuoc tinh thay vi khoang trang co dinh.
FIX: them @param minutes cho Javadoc de vuot qua NewPipe Checkstyle.

Dung: python apply_sleep_timer.py <duong_dan_thu_muc_NewPipe>
"""
import difflib
from pathlib import Path
import sys


def replace_once(text, old, new, file_label):
    count = text.count(old)
    if count != 1:
        raise ValueError(f"{file_label}: can 1 vi tri de sua, nhung tim thay {count}."
                         " Hay dam bao ma nguon dung phien ban v0.29.1.")
    return text.replace(old, new, 1)


def update_player(text, label):
    if 'setSleepTimerMinutes(final int minutes)' in text:
        raise ValueError(f"{label}: da co hen gio, khong chen trung lan thu hai.")
    text = replace_once(text,
        'import android.media.AudioManager;\n',
        'import android.media.AudioManager;\n'
        'import android.os.Handler;\n'
        'import android.os.Looper;\n'
        'import android.os.SystemClock;\n', label)

    text = replace_once(text,
        '    private final PlayerService service; //TODO try to remove and replace everything with context\n',
        '    private final PlayerService service; //TODO try to remove and replace everything with context\n'
        '\n'
        '    // The timer belongs to the playback service, so it survives UI changes.\n'
        '    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());\n'
        '    @Nullable\n'
        '    private Runnable sleepTimerRunnable;\n'
        '    private long sleepTimerDeadlineElapsedRealtime;\n', label)

    text = replace_once(text,
        '    public void destroy() {\n'
        '        if (DEBUG) {\n'
        '            Log.d(TAG, "destroy() called");\n'
        '        }\n',
        '    public void destroy() {\n'
        '        if (DEBUG) {\n'
        '            Log.d(TAG, "destroy() called");\n'
        '        }\n'
        '        cancelSleepTimer();\n', label)

    methods = """    /**
     * Automatically pauses playback after the given number of real-time minutes.
     * Calling this method again replaces the previous timer.
     *
     * @param minutes number of minutes before playback is paused
     */
    public void setSleepTimerMinutes(final int minutes) {
        cancelSleepTimer();
        if (minutes <= 0) {
            return;
        }
        final long delayMillis = minutes * 60_000L;
        sleepTimerDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + delayMillis;
        sleepTimerRunnable = () -> {
            sleepTimerRunnable = null;
            sleepTimerDeadlineElapsedRealtime = 0L;
            pause();
        };
        sleepTimerHandler.postDelayed(sleepTimerRunnable, delayMillis);
    }

    /** @return milliseconds remaining, or zero if the timer is off. */
    public long getSleepTimerRemainingMillis() {
        if (sleepTimerDeadlineElapsedRealtime == 0L) {
            return 0L;
        }
        return Math.max(0L,
                sleepTimerDeadlineElapsedRealtime - SystemClock.elapsedRealtime());
    }

    public void cancelSleepTimer() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        sleepTimerDeadlineElapsedRealtime = 0L;
    }

"""
    text = replace_once(text,
        '    public void playPause() {\n', methods + '    public void playPause() {\n', label)
    return text


def update_ui(text, label):
    if 'private void showSleepTimerDialog()' in text:
        raise ValueError(f"{label}: da co giao dien hen gio, khong chen trung.")
    text = replace_once(text,
        'import android.os.Looper;\n',
        'import android.os.Looper;\n'
        'import android.text.InputType;\n', label)
    text = replace_once(text,
        'import android.widget.FrameLayout;\n',
        'import android.widget.EditText;\n'
        'import android.widget.FrameLayout;\n', label)
    text = replace_once(text,
        'import android.widget.LinearLayout;\n',
        'import android.widget.LinearLayout;\n'
        'import android.widget.Toast;\n', label)
    text = replace_once(text,
        'import androidx.appcompat.app.AppCompatActivity;\n',
        'import androidx.appcompat.app.AlertDialog;\n'
        'import androidx.appcompat.app.AppCompatActivity;\n', label)
    text = replace_once(text,
        '    protected void initListeners() {\n'
        '        super.initListeners();\n',
        '    protected void initListeners() {\n'
        '        super.initListeners();\n'
        '        binding.sleepTimerTextView.setOnClickListener(\n'
        '                makeOnClickListener(this::showSleepTimerDialog));\n'
        '        updateSleepTimerLabel();\n', label)
    text = replace_once(text,
        '    protected void deinitListeners() {\n'
        '        super.deinitListeners();\n',
        '    protected void deinitListeners() {\n'
        '        super.deinitListeners();\n'
        '        binding.sleepTimerTextView.setOnClickListener(null);\n', label)
    # This one is unique in MainPlayerUi and refreshes the label while media is playing.
    text = replace_once(text,
        '        super.onUpdateProgress(currentProgress, duration, bufferPercent);\n',
        '        super.onUpdateProgress(currentProgress, duration, bufferPercent);\n'
        '        updateSleepTimerLabel();\n', label)
    methods = """    private void updateSleepTimerLabel() {
        final long remaining = player.getSleepTimerRemainingMillis();
        if (remaining > 0L) {
            final long minutes = (remaining + 59_999L) / 60_000L;
            binding.sleepTimerTextView.setText(
                    context.getString(R.string.sleep_timer_remaining, minutes));
        } else {
            binding.sleepTimerTextView.setText(R.string.sleep_timer_title);
        }
    }

    private void startSleepTimer(final int minutes) {
        player.setSleepTimerMinutes(minutes);
        updateSleepTimerLabel();
        Toast.makeText(context, context.getString(R.string.sleep_timer_started, minutes),
                Toast.LENGTH_SHORT).show();
    }

    private void showSleepTimerDialog() {
        getParentActivity().ifPresent(activity -> {
            final int[] presetMinutes = {5, 10, 15, 30, 45, 60, 90, 120};
            final String[] options = new String[presetMinutes.length + 2];
            for (int i = 0; i < presetMinutes.length; i++) {
                options[i] = activity.getString(
                        R.string.sleep_timer_minutes_option, presetMinutes[i]);
            }
            options[presetMinutes.length] = activity.getString(R.string.sleep_timer_custom);
            options[presetMinutes.length + 1] = activity.getString(
                    R.string.sleep_timer_cancel_option);
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.sleep_timer_title)
                    .setItems(options, (dialog, selected) -> {
                        if (selected < presetMinutes.length) {
                            startSleepTimer(presetMinutes[selected]);
                        } else if (selected == presetMinutes.length) {
                            showCustomSleepTimerDialog(activity);
                        } else {
                            player.cancelSleepTimer();
                            updateSleepTimerLabel();
                            Toast.makeText(activity, R.string.sleep_timer_cancelled,
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void showCustomSleepTimerDialog(final AppCompatActivity activity) {
        final EditText minutesInput = new EditText(activity);
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setSingleLine(true);
        minutesInput.setHint(R.string.sleep_timer_custom_hint);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.sleep_timer_custom)
                .setView(minutesInput)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    try {
                        final int minutes = Integer.parseInt(
                                minutesInput.getText().toString().trim());
                        if (minutes < 1 || minutes > 1440) {
                            throw new NumberFormatException("Out of range");
                        }
                        startSleepTimer(minutes);
                    } catch (final NumberFormatException e) {
                        Toast.makeText(activity, R.string.sleep_timer_invalid,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

"""
    # IMPORTANT: move the *whole* @Override + method declaration anchor. In v0.29.1,
    # @Override immediately precedes onPlaybackSpeedClicked(). Inserting before only
    # the method declaration makes @Override attach to private updateSleepTimerLabel()
    # and javac fails with: method does not override or implement a method from a supertype.
    text = replace_once(text,
        '    @Override\n'
        '    protected void onPlaybackSpeedClicked() {\n',
        methods
        + '    @Override\n'
        + '    protected void onPlaybackSpeedClicked() {\n', label)
    # A timer expiry pauses playback; the progress loop then stops, so refresh
    # the timer label directly when entering the paused state as well.
    text = replace_once(text,
        '    @Override\n'
        '    public void onPlaying() {\n',
        '    @Override\n'
        '    public void onPaused() {\n'
        '        super.onPaused();\n'
        '        updateSleepTimerLabel();\n'
        '    }\n'
        '\n'
        '    @Override\n'
        '    public void onPlaying() {\n', label)
    return text


def update_layout(text, label):
    """Insert the timer control in secondaryControls, just before the captions.

    The old patch assumed an exact indentation sequence after `tools:text="FIT"`.
    GitHub's actual v0.29.1 layout can differ in that whitespace, so match the
    uniquely identifiable captions FrameLayout instead of its preceding spaces.
    """
    import re

    if 'android:id="@+id/sleepTimerTextView"' in text:
        raise ValueError(f"{label}: nut hen gio da ton tai, khong chen lap.")

    resize_marker = 'android:id="@+id/resizeTextView"'
    if text.count(resize_marker) != 1:
        raise ValueError(f"{label}: khong tim thay duy nhat resizeTextView.")

    # This FrameLayout contains captionTextView and is the only one with weight 3
    # under secondaryControls in the original NewPipe v0.29.1 player.xml.
    pattern = re.compile(
        r'^(?P<indent>[ \t]*)<FrameLayout'
        r'(?=[ \t]*\r?\n[ \t]*android:layout_width="0dp"'
        r'[ \t]*\r?\n[ \t]*android:layout_height="wrap_content"'
        r'[ \t]*\r?\n[ \t]*android:layout_weight="3")',
        re.MULTILINE,
    )
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise ValueError(
            f"{label}: can 1 FrameLayout cua caption, nhung tim thay "
            f"{len(matches)}. Kiem tra XML tren tag v0.29.1."
        )
    match = matches[0]
    # Verify this is the captions container after the resize button.
    if match.start() <= text.index(resize_marker):
        raise ValueError(f"{label}: bo cuc resize/caption khong dung du kien.")
    near = text[match.start():match.start() + 800]
    if 'android:id="@+id/captionTextView"' not in near:
        raise ValueError(f"{label}: FrameLayout khong phai khung caption.")

    indent = match.group('indent')
    child = indent + '    '
    block = (
        indent + '<org.schabi.newpipe.views.NewPipeTextView\n'
        + child + 'android:id="@+id/sleepTimerTextView"\n'
        + child + 'android:layout_width="wrap_content"\n'
        + child + 'android:layout_height="35dp"\n'
        + child + 'android:layout_marginEnd="8dp"\n'
        + child + 'android:background="?attr/selectableItemBackground"\n'
        + child + 'android:clickable="true"\n'
        + child + 'android:focusable="true"\n'
        + child + 'android:contentDescription="@string/sleep_timer_title"\n'
        + child + 'android:gravity="center"\n'
        + child + 'android:minWidth="52dp"\n'
        + child + 'android:padding="@dimen/player_main_buttons_padding"\n'
        + child + 'android:text="@string/sleep_timer_title"\n'
        + child + 'android:textColor="@android:color/white"\n'
        + child + 'android:ellipsize="end"\n'
        + child + 'android:singleLine="true"\n'
        + child + 'android:textStyle="bold" />\n'
    )
    updated = text[:match.start()] + block + text[match.start():]

    import xml.etree.ElementTree as ET
    try:
        ET.fromstring(updated)
    except ET.ParseError as exc:
        raise ValueError(f"{label}: XML sau chinh sua khong hop le: {exc}") from exc
    return updated


ENGLISH_STRINGS = """    <string name="sleep_timer_title">Timer</string>
    <string name="sleep_timer_remaining">Timer %1$dm</string>
    <string name="sleep_timer_custom">Custom duration</string>
    <string name="sleep_timer_custom_hint">Minutes (1–1440)</string>
    <string name="sleep_timer_started">Playback stops in %1$d minutes</string>
    <string name="sleep_timer_cancelled">Sleep timer cancelled</string>
    <string name="sleep_timer_invalid">Please enter 1–1440 minutes</string>
    <string name="sleep_timer_cancel_option">Turn off timer</string>
    <string name="sleep_timer_minutes_option">%1$d minutes</string>
"""
VIETNAMESE_STRINGS = """    <string name="sleep_timer_title">Hẹn giờ</string>
    <string name="sleep_timer_remaining">Còn %1$d phút</string>
    <string name="sleep_timer_custom">Nhập số phút tùy chọn</string>
    <string name="sleep_timer_custom_hint">Số phút (1–1440)</string>
    <string name="sleep_timer_started">Sẽ dừng phát sau %1$d phút</string>
    <string name="sleep_timer_cancelled">Đã hủy hẹn giờ</string>
    <string name="sleep_timer_invalid">Vui lòng nhập từ 1 đến 1440 phút</string>
    <string name="sleep_timer_cancel_option">Hủy hẹn giờ</string>
    <string name="sleep_timer_minutes_option">%1$d phút</string>
"""


def update_strings(text, label, inserted):
    for key in ('sleep_timer_title', 'sleep_timer_remaining'):
        if f'name="{key}"' in text:
            raise ValueError(f'{label}: key {key} da ton tai. Khong ghi de.')
    return replace_once(text, '</resources>\n', inserted + '</resources>\n', label)


def main():
    if len(sys.argv) != 2:
        sys.exit('Cach dung: python apply_sleep_timer.py <thu_muc_ma_nguon_NewPipe_v0.29.1>')
    root = Path(sys.argv[1]).expanduser().resolve()
    files = {
        'app/src/main/java/org/schabi/newpipe/player/Player.java': update_player,
        'app/src/main/java/org/schabi/newpipe/player/ui/MainPlayerUi.java': update_ui,
        'app/src/main/res/layout/player.xml': update_layout,
        'app/src/main/res/values/strings.xml':
            lambda text, label: update_strings(text, label, ENGLISH_STRINGS),
        'app/src/main/res/values-vi/strings.xml':
            lambda text, label: update_strings(text, label, VIETNAMESE_STRINGS),
    }
    originals = {}
    modified = {}
    for rel, transform in files.items():
        path = root / rel
        if not path.is_file():
            sys.exit(f'Thieu tep: {path}')
        originals[rel] = path.read_text(encoding='utf-8')
        modified[rel] = transform(originals[rel], rel)
    # All changes were validated before writing any of them.
    changes = []
    for rel in files:
        changes += list(difflib.unified_diff(
            originals[rel].splitlines(keepends=True),
            modified[rel].splitlines(keepends=True),
            fromfile='a/' + rel,
            tofile='b/' + rel,
        ))
    for rel in files:
        (root / rel).write_text(modified[rel], encoding='utf-8')
    (root / 'sleep_timer_changes.diff').write_text(''.join(changes), encoding='utf-8')
    print('DA AP DUNG CHINH SUA:', len(files), 'tep; vao thu muc', root)
    print('Ban diff:', root / 'sleep_timer_changes.diff')
    print('BIEN DICH: ./gradlew assembleDebug (hoac gradlew.bat assembleDebug)')
    print('APK: app/build/outputs/apk/debug/app-debug.apk')
    print('LUU Y: Khong cai de ban NewPipe chinh thuc; ban Debug co ID rieng.')


if __name__ == '__main__':
    main()
