package com.CheekyLittleApps.audioplayer;

import static com.CheekyLittleApps.audioplayer.helpers.UIHelper.showFileChooser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;

import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.CheekyLittleApps.audioplayer.helpers.MediaNotificationHelper;
import com.CheekyLittleApps.audioplayer.helpers.MediaPlayerHelper;
import com.CheekyLittleApps.audioplayer.helpers.MediaPlayerService;
import com.CheekyLittleApps.audioplayer.helpers.SharedPreferencesHelper;
import com.CheekyLittleApps.audioplayer.helpers.UIHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AudioPlayerPrefs";
    private static final String KEY_POSITION = "audiobook_position";

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private static final String PREFS_APP = "app_prefs";
    private static final String KEY_NEVER_SHOW_AGAIN = "never_show_again";
    boolean neverShowAgain;

    private Handler handler;
    private Runnable updatePositionRunnable;
    private Uri mediaUri;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ImageView ivCover;
    private MediaPlayerHelper mediaPlayerHelper;

    private Spinner spinnerPlaybackSpeed;

    static Button btnPlay;

    TextView tvTitle;
    TextView tvArtist;
    TextView tvCurrentTime;
    TextView tvSongLength;

    SeekBar sbTime;

    private MediaSessionCompat mediaSession;
    private MediaNotificationHelper notificationHelper;

    private AudioManager audioManager;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        SharedPreferences prefs = getSharedPreferences(PREFS_APP, MODE_PRIVATE);
        neverShowAgain = prefs.getBoolean(KEY_NEVER_SHOW_AGAIN, false);


        mediaSession = new MediaSessionCompat(this, "MediaSessionTag");
        mediaPlayerHelper = new MediaPlayerHelper(this, mediaSession);
        notificationHelper = mediaPlayerHelper.getNotificationHelper();

        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        startService(serviceIntent);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = keyEvent.getKeyCode();
                    try {
                        SharedPreferencesHelper.savePlaybackPosition(MediaPlayerHelper.getUri(), MediaPlayerHelper.getMediaPlayer(), MainActivity.this);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            MediaPlayerHelper.handlePlayButton();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            MediaPlayerHelper.handlePlayButton();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            MediaPlayerHelper.handlePlayButton();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            try {
                                MediaPlayerHelper.handleButtonForward(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            try {
                                MediaPlayerHelper.handleButtonBack(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                            try {
                                MediaPlayerHelper.handleButtonForward(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_REWIND:
                            try {
                                MediaPlayerHelper.handleButtonBack(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                            try {
                                MediaPlayerHelper.handleButtonBack(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                            try {
                                MediaPlayerHelper.handleButtonForward(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
                            try {
                                MediaPlayerHelper.handleButtonBack(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            break;
                        case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
                            try {
                                MediaPlayerHelper.handleButtonForward(MediaPlayerHelper.getUri());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                    }
                }
                return true;
            }
        });

        mediaSession.setActive(true);
        setupUIComponents();

        mediaPlayerHelper.startUpdatingCurrentTime(sbTime, tvCurrentTime, mediaUri, this);

        requestNotificationPermission();
    }


    public static void updatePlayButtonText(String text) {
        if (btnPlay != null) {
            btnPlay.setText(text);
        }
    }


    private void setupUIComponents()
    {
        Button btnFileSelect = findViewById(R.id.btnFileSelect);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);

        spinnerPlaybackSpeed = findViewById(R.id.spinnerPlaybackSpeed);

        btnPlay = findViewById(R.id.btnWidgetPlay);

        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvSongLength = findViewById(R.id.tvSongLength);

        ivCover = findViewById(R.id.ivCover);

        sbTime = findViewById(R.id.sbTime);

        btnFileSelect.setOnClickListener(v -> {
            showFileChooser(filePickerLauncher);

            try {
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnPlay.setOnClickListener(v -> {
            MediaPlayerHelper.handlePlayButton();


            try {
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), MainActivity.this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnForward.setOnClickListener(v -> {
            try {
                MediaPlayerHelper.handleButtonForward(mediaUri);
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnBack.setOnClickListener(v -> {
            try {
                MediaPlayerHelper.handleButtonBack(mediaUri);
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedPosition = prefs.getInt(KEY_POSITION, 0);

        if(MediaPlayerHelper.getMediaPlayer()  != null)
        {
            MediaPlayerHelper.getMediaPlayer().seekTo(savedPosition);
        }

        //picks file for playback
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();

                        if(data != null)
                        {
                            Uri uri = data.getData();
                            mediaUri = uri;
                            MediaPlayerHelper.handleAudioFile(MainActivity.this, uri, spinnerPlaybackSpeed, btnPlay, sbTime, ivCover, tvTitle, tvArtist, tvSongLength, mediaUri, tvCurrentTime);
                        }
                    }
                });

        //Spinner options to pick playback speed
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.playback_speeds, android.R.layout.simple_spinner_item);

        spinnerPlaybackSpeed.setAdapter(adapter);

        //sets default to 1.00x speed
        spinnerPlaybackSpeed.setSelection(2);

        spinnerPlaybackSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedSpeed = adapterView.getItemAtPosition(i).toString();

                    MediaPlayerHelper.handlePlayBackSpeedChange(selectedSpeed, MediaPlayerHelper.getMediaPlayer());
                    UIHelper.showToast(MainActivity.this, "Selected: " + selectedSpeed);
                    try {
                        SharedPreferencesHelper.savePlaybackPosition(MediaPlayerHelper.getUri(), MediaPlayerHelper.getMediaPlayer(), MainActivity.this);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }


            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void showPermissionDeniedAlert() {
        SharedPreferences prefs = getSharedPreferences(PREFS_APP, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app requires notification permissions to display controls on the lock screen. The app will send you no other notifications. If you wish to have this functionality" +
                        " please enable notification permissions in the app settings.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Never Show Again", (dialog, which) -> {
                    editor.putBoolean(KEY_NEVER_SHOW_AGAIN, true);  // Set "Never Show Again" flag
                    editor.apply();
                })
                .show();
    }

    private void requestNotificationPermission() {
        if (!neverShowAgain && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) or higher
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show notification
                //showNotification();
            } else {
                // Permission denied, handle accordingly
                showPermissionDeniedAlert();
            }
        }
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d("d", "On destroy called in main");
        try {
            SharedPreferencesHelper.savePlaybackPosition(MediaPlayerHelper.getUri(), MediaPlayerHelper.getMediaPlayer(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mediaPlayerHelper.stopUpdatingCurrentTime();
        //audioManager.abandonAudioFocus(this);
        if(MediaPlayerHelper.getMediaPlayer()  != null) {
            MediaPlayerHelper.getMediaPlayer().release();
        }
        if(handler != null)
        {
            handler.removeCallbacks(updatePositionRunnable);
        }

        MediaPlayerHelper.release();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
