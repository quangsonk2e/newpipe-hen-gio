/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.localmedia;

import android.Manifest;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import org.schabi.newpipe.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Library content displayed *inside* NewPipe's main horizontal tab bar. */
public final class LocalMediaFragment extends Fragment {
    private static final int PERMISSION_REQUEST = 682;
    private final Handler miniHandler = new Handler(Looper.getMainLooper());
    private LocalMediaPlaybackService miniService;
    private View miniBar;
    private TextView miniTitle;
    private TextView miniPause;
    private boolean miniBound;
    private final Runnable miniRefresh = new Runnable() {
        @Override public void run() {
            if (miniService == null || miniBar == null) { return; }
            miniBar.setVisibility(miniService.hasMedia() ? View.VISIBLE : View.GONE);
            miniTitle.setText(miniService.getCurrentTitle());
            miniPause.setText(miniService.isPlaying()
                    ? R.string.local_media_pause : R.string.local_media_play);
            miniHandler.postDelayed(this, 1000L);
        }
    };
    private final ServiceConnection miniConnection = new ServiceConnection() {
        @Override public void onServiceConnected(final ComponentName name,
                                                 final IBinder binder) {
            miniService = ((LocalMediaPlaybackService.LocalBinder) binder).getService();
            miniHandler.removeCallbacks(miniRefresh);
            miniRefresh.run();
        }
        @Override public void onServiceDisconnected(final ComponentName name) {
            miniService = null;
            miniHandler.removeCallbacks(miniRefresh);
            if (miniBar != null) { miniBar.setVisibility(View.GONE); }
        }
    };
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<LocalMediaItem> allItems = new ArrayList<>();
    private final List<LocalMediaItem> visibleItems = new ArrayList<>();
    private final List<String> folderChoices = new ArrayList<>();
    private final ActivityResultLauncher<String[]> openFile = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || getContext() == null) {
                    return;
                }
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (final SecurityException ignored) {
                    // Some providers only grant temporary access.
                }
                final String type = requireContext().getContentResolver().getType(uri);
                final boolean video = type != null && type.startsWith("video/");
                play(uri, video);
            });

    private ArrayAdapter<LocalMediaItem> listAdapter;
    private Spinner foldersSpinner;
    private Spinner sortSpinner;
    private EditText search;
    private ProgressBar progress;
    private TextView status;
    private TextView empty;
    private ListView list;
    private boolean videoMode;
    private String selectedFolder = "";
    private int generation;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_local_media, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.localMediaProgress);
        status = view.findViewById(R.id.localMediaStatus);
        empty = view.findViewById(R.id.localMediaEmpty);
        search = view.findViewById(R.id.localMediaSearch);
        foldersSpinner = view.findViewById(R.id.localMediaFolders);
        sortSpinner = view.findViewById(R.id.localMediaSort);
        list = view.findViewById(R.id.localMediaList);
        miniBar = view.findViewById(R.id.localMediaMiniPlayer);
        miniTitle = view.findViewById(R.id.localMiniTitle);
        miniPause = view.findViewById(R.id.localMiniPause);
        view.findViewById(R.id.localMiniPause).setOnClickListener(v -> {
            if (miniService != null) { miniService.togglePlayback(); }
        });
        view.findViewById(R.id.localMiniOpen).setOnClickListener(v -> {
            if (miniService != null) {
                startActivity(new Intent(requireContext(), LocalMediaPlayerActivity.class));
            }
        });

        listAdapter = new ArrayAdapter<LocalMediaItem>(requireContext(),
                R.layout.row_local_media, visibleItems) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable final View recycled,
                                @NonNull final ViewGroup parent) {
                final View row = recycled == null ? LayoutInflater.from(getContext())
                        .inflate(R.layout.row_local_media, parent, false) : recycled;
                final LocalMediaItem item = getItem(position);
                final ImageView icon = row.findViewById(R.id.localMediaRowIcon);
                icon.setImageResource(item.video ? android.R.drawable.ic_media_play
                        : R.drawable.ic_music_note);
                ((TextView) row.findViewById(R.id.localMediaRowTitle)).setText(item.title);
                final String meta = item.subtitle.isEmpty() ? item.folder : item.subtitle;
                ((TextView) row.findViewById(R.id.localMediaRowSubtitle))
                        .setText(meta.isEmpty() ? getString(R.string.local_media_unknown) : meta);
                ((TextView) row.findViewById(R.id.localMediaRowDuration))
                        .setText(formatDuration(item.duration));
                return row;
            }
        };
        list.setAdapter(listAdapter);
        list.setOnItemClickListener((parent, clicked, position, id) -> {
            final LocalMediaItem item = visibleItems.get(position);
            play(item.uri, item.video);
        });
        view.findViewById(R.id.localMediaRefresh).setOnClickListener(v -> loadFiles());
        view.findViewById(R.id.localMediaOpenFile).setOnClickListener(
                v -> openFile.launch(new String[]{"audio/*", "video/*"}));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start,
                                          final int count, final int after) { }
            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                filterItems();
            }
            @Override
            public void afterTextChanged(final Editable s) { }
        });

        final ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{
                getString(R.string.local_media_sort_recent),
                getString(R.string.local_media_sort_name),
                getString(R.string.local_media_sort_duration)
        });
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View selected,
                                       final int position, final long id) {
                filterItems();
            }
            @Override
            public void onNothingSelected(final AdapterView<?> parent) { }
        });
        foldersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View selected,
                                       final int position, final long id) {
                selectedFolder = position == 0 || position >= folderChoices.size()
                        ? "" : folderChoices.get(position);
                filterItems();
            }
            @Override
            public void onNothingSelected(final AdapterView<?> parent) { }
        });

        final TabLayout types = view.findViewById(R.id.localTypeTabs);
        types.addTab(types.newTab().setText(R.string.local_media_audio));
        types.addTab(types.newTab().setText(R.string.local_media_video));
        types.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
                videoMode = tab.getPosition() == 1;
                selectedFolder = "";
                loadFiles();
            }
            @Override
            public void onTabUnselected(final TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(final TabLayout.Tab tab) { }
        });
        videoMode = savedInstanceState != null && savedInstanceState.getBoolean("local_video");
        if (videoMode) {
            final TabLayout.Tab videoTab = types.getTabAt(1);
            if (videoTab != null) {
                videoTab.select();
            }
        } else {
            loadFiles();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind to an ALREADY running service; do not launch an idle service.
        miniBound = requireContext().bindService(
                new Intent(requireContext(), LocalMediaPlaybackService.class),
                miniConnection, 0);
    }

    @Override
    public void onStop() {
        miniHandler.removeCallbacks(miniRefresh);
        if (miniBound) {
            requireContext().unbindService(miniConnection);
            miniBound = false;
        }
        miniService = null;
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putBoolean("local_video", videoMode);
        super.onSaveInstanceState(outState);
    }

    private void play(@NonNull final Uri uri, final boolean video) {
        if (getContext() == null) {
            return;
        }
        final Intent intent = new Intent(requireContext(), LocalMediaPlayerActivity.class);
        intent.putExtra(LocalMediaPlaybackService.EXTRA_URI, uri.toString());
        intent.putExtra(LocalMediaPlaybackService.EXTRA_VIDEO, video);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private boolean hasPermission() {
        if (getContext() == null) {
            return false;
        }
        final String permission = Build.VERSION.SDK_INT >= 33
                ? (videoMode ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_MEDIA_AUDIO)
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void loadFiles() {
        if (!isAdded() || getView() == null) {
            return;
        }
        final int requested = ++generation;
        allItems.clear();
        visibleItems.clear();
        listAdapter.notifyDataSetChanged();
        empty.setVisibility(View.GONE);
        if (!hasPermission()) {
            progress.setVisibility(View.GONE);
            status.setText(R.string.local_media_permission_needed);
            final String permission = Build.VERSION.SDK_INT >= 33
                    ? (videoMode ? Manifest.permission.READ_MEDIA_VIDEO
                    : Manifest.permission.READ_MEDIA_AUDIO)
                    : Manifest.permission.READ_EXTERNAL_STORAGE;
            requestPermissions(new String[]{permission}, PERMISSION_REQUEST);
            return;
        }
        status.setText(R.string.local_media_scanning);
        progress.setVisibility(View.VISIBLE);
        final boolean scanVideos = videoMode;
        final android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<LocalMediaItem> items;
            try {
                items = LocalMediaRepository.read(appContext, scanVideos);
            } catch (final SecurityException ex) {
                items = new ArrayList<>();
            }
            final List<LocalMediaItem> result = items;
            if (getActivity() == null) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null || requested != generation) {
                    return;
                }
                progress.setVisibility(View.GONE);
                allItems.clear();
                allItems.addAll(result);
                updateFolders();
                filterItems();
            });
        });
    }

    private void updateFolders() {
        final Set<String> unique = new LinkedHashSet<>();
        for (final LocalMediaItem item : allItems) {
            if (!item.folder.isEmpty()) {
                unique.add(item.folder);
            }
        }
        final List<String> sorted = new ArrayList<>(unique);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        folderChoices.clear();
        folderChoices.add(getString(R.string.local_media_all_folders));
        folderChoices.addAll(sorted);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, folderChoices);
        foldersSpinner.setAdapter(adapter);
        selectedFolder = "";
    }

    private void filterItems() {
        if (!isAdded() || search == null || listAdapter == null || sortSpinner == null) {
            return;
        }
        final String phrase = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();
        for (final LocalMediaItem item : allItems) {
            if (!selectedFolder.isEmpty() && !selectedFolder.equals(item.folder)) {
                continue;
            }
            if (item.title.toLowerCase(Locale.ROOT).contains(phrase)
                    || item.subtitle.toLowerCase(Locale.ROOT).contains(phrase)
                    || item.folder.toLowerCase(Locale.ROOT).contains(phrase)) {
                visibleItems.add(item);
            }
        }
        final int sort = sortSpinner.getSelectedItemPosition();
        final Comparator<LocalMediaItem> compare;
        if (sort == 1) {
            compare = Comparator.comparing(item -> item.title.toLowerCase(Locale.ROOT));
        } else if (sort == 2) {
            compare = (first, second) -> Long.compare(second.duration, first.duration);
        } else {
            compare = (first, second) -> Long.compare(second.dateAdded, first.dateAdded);
        }
        Collections.sort(visibleItems, compare);
        listAdapter.notifyDataSetChanged();
        status.setText(getString(R.string.local_media_count, visibleItems.size()));
        empty.setVisibility(visibleItems.isEmpty() && progress.getVisibility() != View.VISIBLE
                ? View.VISIBLE : View.GONE);
    }

    private static String formatDuration(final long millis) {
        final long total = Math.max(0L, millis / 1000L);
        if (total == 0L) {
            return "";
        }
        if (total >= 3600) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", total / 3600,
                    total % 3600 / 60, total % 60);
        }
        return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (hasPermission()) {
                loadFiles();
            } else if (status != null) {
                status.setText(R.string.local_media_permission_needed);
                Toast.makeText(requireContext(), R.string.local_media_permission_needed,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        ++generation;
        search = null;
        list = null;
        listAdapter = null;
        foldersSpinner = null;
        sortSpinner = null;
        progress = null;
        status = null;
        empty = null;
        miniBar = null;
        miniTitle = null;
        miniPause = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
