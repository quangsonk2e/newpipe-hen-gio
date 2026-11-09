/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.localmedia;

import android.net.Uri;
import androidx.annotation.NonNull;

/** MediaStore-backed item; its content URI remains private to the device. */
public final class LocalMediaItem {
    public final Uri uri;
    public final String title;
    public final String subtitle;
    public final String folder;
    public final long duration;
    public final long dateAdded;
    public final boolean video;

    public LocalMediaItem(@NonNull final Uri uri, @NonNull final String title,
                          @NonNull final String subtitle, final long duration,
                          final boolean video) {
        this(uri, title, subtitle, "", duration, 0L, video);
    }

    public LocalMediaItem(@NonNull final Uri uri, @NonNull final String title,
                          @NonNull final String subtitle, @NonNull final String folder,
                          final long duration, final long dateAdded, final boolean video) {
        this.uri = uri;
        this.title = title;
        this.subtitle = subtitle;
        this.folder = folder;
        this.duration = duration;
        this.dateAdded = dateAdded;
        this.video = video;
    }
}
