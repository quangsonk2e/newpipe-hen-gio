/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.localmedia;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

/** Compatibility entry point for the drawer; the main screen uses the same fragment. */
public final class LocalMediaActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        final FrameLayout container = new FrameLayout(this);
        container.setId(R.id.localMediaHost);
        setContentView(container);
        setTitle(R.string.local_media_title);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.localMediaHost, new LocalMediaFragment()).commit();
        }
    }
}
