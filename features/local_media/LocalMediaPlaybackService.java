/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Local playback extension for NewPipe.
 */
package org.schabi.newpipe.localmedia;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.Player;

import org.schabi.newpipe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground ExoPlayer service: offline files keep playing after the UI is closed. */
public final class LocalMediaPlaybackService extends Service {
    public static final String EXTRA_URI = "org.schabi.newpipe.localmedia.URI";
    public static final String EXTRA_VIDEO = "org.schabi.newpipe.localmedia.VIDEO";
    public static final String ACTION_OPEN = "org.schabi.newpipe.localmedia.OPEN";
    public static final String ACTION_TOGGLE = "org.schabi.newpipe.localmedia.TOGGLE";
    public static final String ACTION_NEXT = "org.schabi.newpipe.localmedia.NEXT";
    public static final String ACTION_PREV = "org.schabi.newpipe.localmedia.PREV";
    public static final String ACTION_STOP = "org.schabi.newpipe.localmedia.STOP";
    private static final String CHANNEL_ID = "newpipe_local_media_playback";
    private static final int NOTIFICATION_ID = 22491;

    private final IBinder binder = new LocalBinder();
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<LocalMediaItem> currentQueue = new ArrayList<>();
    private ExoPlayer player;
    private MediaSessionCompat session;
    private long timerDeadline;
    private int generation;
    private boolean currentVideo;
    private final Runnable timerAction = this::stopPlayback;

    /** Binder for UI components connecting to local media playback. */
    public final class LocalBinder extends Binder {
        public LocalMediaPlaybackService getService() {
            return LocalMediaPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    getString(R.string.local_media_channel), NotificationManager.IMPORTANCE_LOW));
        }
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        session = new MediaSessionCompat(this, "NewPipeLocalMedia");
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                player.play();
            }
            @Override
            public void onPause() {
                player.pause();
            }
            @Override
            public void onSkipToNext() {
                next();
            }
            @Override
            public void onSkipToPrevious() {
                previous();
            }
            @Override
            public void onStop() {
                stopPlayback();
            }
            @Override
            public void onSeekTo(final long pos) {
                player.seekTo(pos);
            }
        });
        session.setActive(true);
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(final boolean playing) {
                updateNotification();
            }
            @Override
            public void onMediaItemTransition(@Nullable final MediaItem item, final int reason) {
                updateMetadata();
                updateNotification();
            }
            @Override
            public void onPlaybackStateChanged(final int state) {
                updateMetadata();
                updateNotification();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(@Nullable final Intent intent, final int flags,
                              final int startId) {
        // Immediately enter foreground: required after startForegroundService() on Android 8+.
        startForeground(NOTIFICATION_ID, createNotification());
        if (intent == null) {
            return START_NOT_STICKY;
        }
        final String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
        } else if (ACTION_TOGGLE.equals(action)) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        } else if (ACTION_NEXT.equals(action)) {
            next();
        } else if (ACTION_PREV.equals(action)) {
            previous();
        } else if (ACTION_OPEN.equals(action)) {
            final String uri = intent.getStringExtra(EXTRA_URI);
            if (uri != null && !uri.isEmpty()) {
                openMedia(Uri.parse(uri), intent.getBooleanExtra(EXTRA_VIDEO, false));
            }
        }
        return START_NOT_STICKY;
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public boolean isVideo() {
        return currentVideo;
    }

    public boolean hasMedia() {
        return !currentQueue.isEmpty();
    }

    private String displayNameFor(@NonNull final Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final String display = cursor.getString(0);
                if (display != null && !display.isEmpty()) {
                    return display;
                }
            }
        } catch (final SecurityException | IllegalArgumentException ignored) {
            // The selected document provider does not expose a display name.
        }
        return uri.getLastPathSegment() == null ? getString(R.string.local_media_title)
                : uri.getLastPathSegment();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void togglePlayback() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public boolean isShuffleEnabled() {
        return player.getShuffleModeEnabled();
    }

    public void toggleShuffle() {
        player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
        updateNotification();
    }

    public int getRepeatMode() {
        return player.getRepeatMode();
    }

    public void cycleRepeat() {
        final int oldMode = player.getRepeatMode();
        final int nextMode = oldMode == Player.REPEAT_MODE_OFF ? Player.REPEAT_MODE_ALL
                : oldMode == Player.REPEAT_MODE_ALL ? Player.REPEAT_MODE_ONE
                : Player.REPEAT_MODE_OFF;
        player.setRepeatMode(nextMode);
        updateNotification();
    }

    public long getCurrentPosition() {
        return Math.max(0L, player.getCurrentPosition());
    }

    public long getDuration() {
        return Math.max(0L, player.getDuration());
    }

    public void seekTo(final long milliseconds) {
        player.seekTo(Math.max(0L, milliseconds));
    }

    public String getCurrentTitle() {
        final int index = player.getCurrentMediaItemIndex();
        return index >= 0 && index < currentQueue.size()
                ? currentQueue.get(index).title : getString(R.string.local_media_title);
    }

    /**
     * Build an offline playlist in the background and play the selected item.
     * @param selected the requested MediaStore URI
     * @param video whether the media comes from the video collection
     */
    private void openMedia(@NonNull final Uri selected, final boolean video) {
        final int request = ++generation;
        currentVideo = video;
        player.stop();
        currentQueue.clear();
        updateNotification();
        libraryExecutor.execute(() -> {
            List<LocalMediaItem> loaded;
            try {
                loaded = LocalMediaRepository.read(getApplicationContext(), video);
            } catch (final SecurityException e) {
                loaded = new ArrayList<>();
            }
            final List<LocalMediaItem> result = loaded;
            handler.post(() -> {
                if (request != generation) {
                    return;
                }
                int startIndex = -1;
                for (int i = 0; i < result.size(); i++) {
                    if (selected.equals(result.get(i).uri)) {
                        startIndex = i;
                        break;
                    }
                }
                if (startIndex < 0) {
                    // The document might have disappeared from MediaStore meanwhile.
                    result.clear();
                    result.add(new LocalMediaItem(selected, displayNameFor(selected),
                            "", 0L, video));
                    startIndex = 0;
                }
                currentQueue.clear();
                currentQueue.addAll(result);
                final List<MediaItem> sources = new ArrayList<>(result.size());
                for (final LocalMediaItem item : result) {
                    sources.add(MediaItem.fromUri(item.uri));
                }
                player.setMediaItems(sources, startIndex, 0L);
                player.prepare();
                player.play();
                updateMetadata();
                updateNotification();
            });
        });
    }

    public void next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
            player.play();
        }
    }

    public void previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem();
            player.play();
        } else {
            player.seekTo(0L);
        }
    }

    /**
     * Sleep timer shared by audio and local video playback.
     * @param minutes number of minutes until local playback is stopped
     */
    public void setSleepTimerMinutes(final int minutes) {
        cancelSleepTimer();
        if (minutes < 1 || minutes > 1440) {
            return;
        }
        final long delay = minutes * 60_000L;
        timerDeadline = SystemClock.elapsedRealtime() + delay;
        handler.postDelayed(timerAction, delay);
    }

    public long getSleepTimerRemainingMillis() {
        return timerDeadline == 0L ? 0L
                : Math.max(0L, timerDeadline - SystemClock.elapsedRealtime());
    }

    public void cancelSleepTimer() {
        handler.removeCallbacks(timerAction);
        timerDeadline = 0L;
    }

    private void updateMetadata() {
        if (session == null || player == null) {
            return;
        }
        session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getCurrentTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        getString(R.string.local_media_title))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        Math.max(0L, player.getDuration()))
                .build());
        final int state = player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        session.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_STOP)
                .setState(state, Math.max(0L, player.getCurrentPosition()),
                        player.isPlaying() ? 1.0f : 0.0f).build());
    }

    private PendingIntent serviceAction(final String action, final int requestCode) {
        final Intent intent = new Intent(this, LocalMediaPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification createNotification() {
        final Intent open = new Intent(this, LocalMediaPlayerActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 5, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(getCurrentTitle())
                .setContentText(getString(R.string.local_media_playing))
                .setContentIntent(contentIntent)
                .setOngoing(player != null && player.isPlaying())
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous,
                        getString(R.string.local_media_previous),
                        serviceAction(ACTION_PREV, 1))
                .addAction(player != null && player.isPlaying()
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        getString(R.string.local_media_play_pause),
                        serviceAction(ACTION_TOGGLE, 2))
                .addAction(android.R.drawable.ic_media_next,
                        getString(R.string.local_media_next),
                        serviceAction(ACTION_NEXT, 3))
                .addAction(R.drawable.ic_close, getString(R.string.local_media_stop),
                        serviceAction(ACTION_STOP, 4))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(session.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private void updateNotification() {
        if (player == null || session == null) {
            return;
        }
        updateMetadata();
        final NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    public void stopPlayback() {
        ++generation;
        cancelSleepTimer();
        player.stop();
        player.clearMediaItems();
        currentQueue.clear();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        ++generation;
        cancelSleepTimer();
        libraryExecutor.shutdownNow();
        session.setActive(false);
        session.release();
        player.release();
        super.onDestroy();
    }
}
