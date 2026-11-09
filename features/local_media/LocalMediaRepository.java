/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.localmedia;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Read locally indexed music and videos without requiring all-files access. */
public final class LocalMediaRepository {
    private static final String TAG = "NewPipeLocalMedia";

    private LocalMediaRepository() { }

    @NonNull
    public static List<LocalMediaItem> read(@NonNull final Context context,
                                            final boolean video) {
        final List<LocalMediaItem> result = new ArrayList<>();
        final ContentResolver resolver = context.getContentResolver();
        final Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String pathKey = Build.VERSION.SDK_INT >= 29
                ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA;
        final String subtitleKey = video ? MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                : MediaStore.Audio.Media.ARTIST;
        final String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DURATION,
                subtitleKey,
                pathKey,
                MediaStore.MediaColumns.DATE_ADDED
        };
        try (Cursor cursor = resolver.query(collection, projection, null, null,
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) {
                return result;
            }
            final int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            final int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            final int lengthIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION);
            final int subtitleIndex = cursor.getColumnIndexOrThrow(subtitleKey);
            final int folderIndex = cursor.getColumnIndexOrThrow(pathKey);
            final int dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            while (cursor.moveToNext()) {
                final String title = cursor.getString(nameIndex);
                final String artist = cursor.getString(subtitleIndex);
                final String path = cursor.getString(folderIndex);
                final String folder;
                if (path == null || path.isEmpty()) {
                    folder = "";
                } else if (Build.VERSION.SDK_INT >= 29) {
                    final String trimmed = path.endsWith("/")
                            ? path.substring(0, path.length() - 1) : path;
                    final int slash = trimmed.lastIndexOf('/');
                    folder = trimmed.substring(slash + 1);
                } else {
                    final int last = path.lastIndexOf('/');
                    final int parent = last > 0 ? path.lastIndexOf('/', last - 1) : -1;
                    folder = path.substring(parent + 1, Math.max(parent + 1, last));
                }
                result.add(new LocalMediaItem(
                        ContentUris.withAppendedId(collection, cursor.getLong(idIndex)),
                        title == null ? "" : title,
                        artist == null || "<unknown>".equalsIgnoreCase(artist) ? "" : artist,
                        folder,
                        cursor.isNull(lengthIndex) ? 0L : cursor.getLong(lengthIndex),
                        cursor.isNull(dateIndex) ? 0L : cursor.getLong(dateIndex), video));
            }
        } catch (final SecurityException e) {
            Log.w(TAG, "MediaStore read permission revoked", e);
            throw e;
        }
        return result;
    }
}
