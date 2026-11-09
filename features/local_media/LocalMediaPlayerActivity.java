/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.localmedia;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.Locale;

/** Polished offline player with seek, repeat, shuffle, sleep timer and PiP. */
public final class LocalMediaPlayerActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocalMediaPlaybackService service;
    private PlayerView playerView;
    private TextView title;
    private TextView elapsed;
    private TextView total;
    private Button timerButton;
    private Button pipButton;
    private Button playPauseButton;
    private Button shuffleButton;
    private Button repeatButton;
    private SeekBar seekBar;
    private View audioCover;
    private View controls;
    private boolean bound;
    private boolean scrubbing;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (!bound || service == null) {
                return;
            }
            title.setText(service.getCurrentTitle());
            playPauseButton.setText(service.isPlaying()
                    ? R.string.local_media_pause : R.string.local_media_play);
            shuffleButton.setText(service.isShuffleEnabled()
                    ? R.string.local_media_shuffle_on : R.string.local_media_shuffle_off);
            final int repeat = service.getRepeatMode();
            repeatButton.setText(repeat == Player.REPEAT_MODE_ONE
                    ? R.string.local_media_repeat_one : repeat == Player.REPEAT_MODE_ALL
                    ? R.string.local_media_repeat_all : R.string.local_media_repeat_off);
            final boolean video = service.isVideo();
            audioCover.setVisibility(video ? View.GONE : View.VISIBLE);
            playerView.setVisibility(video ? View.VISIBLE : View.GONE);
            pipButton.setVisibility(video && Build.VERSION.SDK_INT >= 26
                    ? View.VISIBLE : View.GONE);

            final long duration = service.getDuration();
            final long position = service.getCurrentPosition();
            elapsed.setText(clock(position));
            total.setText(clock(duration));
            if (!scrubbing) {
                seekBar.setProgress(duration <= 0L ? 0 :
                        (int) Math.min(1000L, position * 1000L / duration));
            }
            final long remaining = service.getSleepTimerRemainingMillis();
            if (remaining <= 0L) {
                timerButton.setText(R.string.local_media_timer);
            } else {
                final long minutes = (remaining + 59999L) / 60000L;
                timerButton.setText(getString(R.string.local_media_timer_remaining, minutes));
            }
            handler.postDelayed(this, 1000L);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            service = ((LocalMediaPlaybackService.LocalBinder) binder).getService();
            bound = true;
            playerView.setPlayer(service.getPlayer());
            handler.removeCallbacks(refresh);
            refresh.run();
        }
        @Override
        public void onServiceDisconnected(final ComponentName name) {
            playerView.setPlayer(null);
            service = null;
            bound = false;
            handler.removeCallbacks(refresh);
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_media_player);
        setTitle(R.string.local_media_title);
        playerView = findViewById(R.id.localPlayerView);
        title = findViewById(R.id.localPlayerTitle);
        audioCover = findViewById(R.id.localPlayerAudioCover);
        elapsed = findViewById(R.id.localPlayerTimeElapsed);
        total = findViewById(R.id.localPlayerTimeTotal);
        seekBar = findViewById(R.id.localPlayerSeek);
        timerButton = findViewById(R.id.localPlayerTimer);
        pipButton = findViewById(R.id.localPlayerPip);
        playPauseButton = findViewById(R.id.localPlayerPlayPause);
        shuffleButton = findViewById(R.id.localPlayerShuffle);
        repeatButton = findViewById(R.id.localPlayerRepeat);
        controls = findViewById(R.id.localPlayerControls);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar bar, final int value,
                                          final boolean user) {
                if (user && service != null) {
                    elapsed.setText(clock(service.getDuration() * value / 1000L));
                }
            }
            @Override
            public void onStartTrackingTouch(final SeekBar bar) {
                scrubbing = true;
            }
            @Override
            public void onStopTrackingTouch(final SeekBar bar) {
                if (service != null) {
                    service.seekTo(service.getDuration() * bar.getProgress() / 1000L);
                }
                scrubbing = false;
            }
        });
        findViewById(R.id.localPlayerPrevious).setOnClickListener(v -> {
            if (service != null) { service.previous(); }
        });
        findViewById(R.id.localPlayerNext).setOnClickListener(v -> {
            if (service != null) { service.next(); }
        });
        playPauseButton.setOnClickListener(v -> {
            if (service != null) { service.togglePlayback(); }
        });
        shuffleButton.setOnClickListener(v -> {
            if (service != null) { service.toggleShuffle(); handler.removeCallbacks(refresh); refresh.run(); }
        });
        repeatButton.setOnClickListener(v -> {
            if (service != null) { service.cycleRepeat(); handler.removeCallbacks(refresh); refresh.run(); }
        });
        findViewById(R.id.localPlayerStop).setOnClickListener(v -> {
            if (service != null) { service.stopPlayback(); }
            finish();
        });
        timerButton.setOnClickListener(v -> showSleepTimer());
        pipButton.setOnClickListener(v -> enterPip());
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent launch = new Intent(this, LocalMediaPlaybackService.class);
        final String uri = getIntent().getStringExtra(LocalMediaPlaybackService.EXTRA_URI);
        if (uri != null && !uri.isEmpty()) {
            launch.setAction(LocalMediaPlaybackService.ACTION_OPEN);
            launch.putExtra(LocalMediaPlaybackService.EXTRA_URI, uri);
            launch.putExtra(LocalMediaPlaybackService.EXTRA_VIDEO,
                    getIntent().getBooleanExtra(LocalMediaPlaybackService.EXTRA_VIDEO, false));
            ContextCompat.startForegroundService(this, launch);
            getIntent().removeExtra(LocalMediaPlaybackService.EXTRA_URI);
        }
        bound = bindService(new Intent(this, LocalMediaPlaybackService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(refresh);
        if (Build.VERSION.SDK_INT < 26 || !isInPictureInPictureMode()) {
            playerView.setPlayer(null);
        }
        if (bound) {
            unbindService(connection);
            bound = false;
            service = null;
        }
        super.onStop();
    }

    private static String clock(final long milliseconds) {
        final long seconds = Math.max(0L, milliseconds / 1000L);
        if (seconds >= 3600L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d",
                    seconds / 3600L, seconds / 60L % 60L, seconds % 60L);
        }
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private void showSleepTimer() {
        if (service == null) { return; }
        final int[] minutes = {5, 10, 15, 30, 45, 60, 90, 120};
        final String[] choices = new String[minutes.length + 2];
        for (int i = 0; i < minutes.length; i++) {
            choices[i] = getString(R.string.local_media_minutes, minutes[i]);
        }
        choices[minutes.length] = getString(R.string.local_media_timer_custom);
        choices[minutes.length + 1] = getString(R.string.local_media_timer_cancel);
        new AlertDialog.Builder(this).setTitle(R.string.local_media_timer)
                .setItems(choices, (dialog, position) -> {
                    if (service == null) { return; }
                    if (position < minutes.length) {
                        service.setSleepTimerMinutes(minutes[position]);
                    } else if (position == minutes.length) {
                        showCustomTimer();
                    } else {
                        service.cancelSleepTimer();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showCustomTimer() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.local_media_timer_hint);
        new AlertDialog.Builder(this).setTitle(R.string.local_media_timer_custom)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try {
                        final int minutes = Integer.parseInt(input.getText().toString().trim());
                        if (minutes < 1 || minutes > 1440) {
                            throw new NumberFormatException("Invalid timer");
                        }
                        if (service != null) { service.setSleepTimerMinutes(minutes); }
                    } catch (final NumberFormatException e) {
                        Toast.makeText(this, R.string.local_media_timer_invalid,
                                Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT >= 26 && service != null && service.isVideo()) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(final boolean pip,
                 @NonNull final android.content.res.Configuration configuration) {
        super.onPictureInPictureModeChanged(pip, configuration);
        controls.setVisibility(pip ? View.GONE : View.VISIBLE);
        title.setVisibility(pip ? View.GONE : View.VISIBLE);
    }
}
