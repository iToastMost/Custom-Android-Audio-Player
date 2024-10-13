package com.CheekyLittleApps.audioplayer;

import static com.CheekyLittleApps.audioplayer.helpers.UIHelper.showFileChooser;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
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

public class MainActivity extends AppCompatActivity
{
    private static final String PREFS_NAME = "AudioPlayerPrefs";
    private static final String KEY_POSITION = "audiobook_position";


    private Handler handler;
    private Runnable updatePositionRunnable;
    private Uri mediaUri;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ImageView ivCover;
    private MediaPlayerHelper mediaPlayerHelper;

    private Spinner spinnerPlaybackSpeed;

    Button btnPlay;

    TextView tvTitle;
    TextView tvArtist;
    TextView tvCurrentTime;
    TextView tvSongLength;

    SeekBar sbTime;

    private MediaSessionCompat mediaSession;
    private MediaNotificationHelper notificationHelper;
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


        mediaSession = new MediaSessionCompat(this, "MediaSessionTag");
        mediaPlayerHelper = new MediaPlayerHelper(this, mediaSession);
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        startService(serviceIntent);

        mediaSession.setActive(true);

        setupUIComponents();

        mediaPlayerHelper.startUpdatingCurrentTime(sbTime, tvCurrentTime, mediaUri, this);
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
            MediaPlayerHelper.handlePlayButton(btnPlay);

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
                            MediaPlayerHelper.handleAudioFile(uri, spinnerPlaybackSpeed, btnPlay, sbTime, ivCover, tvTitle, tvArtist, tvSongLength, mediaUri, tvCurrentTime);
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
                        SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), MainActivity.this);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }


            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        try {
            SharedPreferencesHelper.savePlaybackPosition(mediaUri, MediaPlayerHelper.getMediaPlayer(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mediaPlayerHelper.stopUpdatingCurrentTime();

        if(MediaPlayerHelper.getMediaPlayer()  != null) {
            MediaPlayerHelper.getMediaPlayer().release();
        }
        handler.removeCallbacks(updatePositionRunnable);
    }

}