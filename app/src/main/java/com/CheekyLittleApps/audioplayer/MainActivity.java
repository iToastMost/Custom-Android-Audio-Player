package com.CheekyLittleApps.audioplayer;

import static com.CheekyLittleApps.audioplayer.helpers.UIHelper.showFileChooser;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.CheekyLittleApps.audioplayer.helpers.MediaPlayerHelper;
import com.CheekyLittleApps.audioplayer.helpers.SharedPreferencesHelper;
import com.CheekyLittleApps.audioplayer.helpers.UIHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    private static final String PREFS_NAME = "AudioPlayerPrefs";
    private static final String KEY_POSITION = "audiobook_position";

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updatePositionRunnable;
    private Uri mediaUri;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ImageView ivCover;
    private int currentTime;

    private String currentAudioType = "";

    private Spinner spinnerPlaybackSpeed;

    Button btnPlay;

    TextView tvTitle;
    TextView tvArtist;
    TextView tvCurrentTime;
    TextView tvSongLength;

    SeekBar sbTime;

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

        setupUIComponents();
    }

    private void handleAudioFile(Uri uri)
    {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();

        try
        {
            String playbackSpeed = spinnerPlaybackSpeed.getSelectedItem().toString();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();

            currentTime = 0;

            int savedPosition = SharedPreferencesHelper.getSavedPlaybackPosition(this, uri);

            if(savedPosition > 0 && "audiobook".equals(SharedPreferencesHelper.getAudioType(this, mediaUri)))
            {
                currentTime = savedPosition;
            }

            mediaPlayer.seekTo(currentTime);
            mediaPlayer.start();
            MediaPlayerHelper.handlePlayBackSpeedChange(playbackSpeed, mediaPlayer);
            sbTime.setMax(mediaPlayer.getDuration());
            btnPlay.setText("Pause");
            handler.post(updatePositionRunnable);

            currentAudioType = SharedPreferencesHelper.getAudioType(this, mediaUri);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        //releases media player for next song if I decide to implement this feature
        /*
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
            handler.removeCallbacks(updatePositionRunnable);
        });
         */

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, uri);

        byte[] artBytes = retriever.getEmbeddedPicture();
        if(artBytes != null)
        {
            Bitmap albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            ivCover.setImageBitmap(albumArt);
        }
        else
        {

        }

        tvTitle.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        tvArtist.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        if(duration != null)
        {
            long durationInMillis = Long.parseLong(duration);
            long durationInSec = durationInMillis / 1000;
            long minutes = durationInSec / 60;
            long seconds = durationInSec % 60;
            String songLength = String.format("%02d:%02d", minutes, seconds);
            tvSongLength.setText(songLength);
        }

        if(retriever != null)
        {
            try
            {
                retriever.release();
                retriever = null;
            }catch (IOException e)
            {
                e.printStackTrace();
            }

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
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnPlay.setOnClickListener(v -> {
            MediaPlayerHelper.handlePlayButton(btnPlay, mediaPlayer, handler, updatePositionRunnable);

            try {
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, MainActivity.this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnForward.setOnClickListener(v -> {
            MediaPlayerHelper.handleButtonForward(mediaPlayer, handler, updatePositionRunnable, currentAudioType);

            try {
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnBack.setOnClickListener(v -> {
            MediaPlayerHelper.handleButtonBack(mediaPlayer, handler, updatePositionRunnable, currentAudioType);

            try {
                SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedPosition = prefs.getInt(KEY_POSITION, 0);

        if(mediaPlayer != null)
        {
            mediaPlayer.seekTo(savedPosition);
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
                            handleAudioFile(uri);
                        }
                    }
                });

        //Seekbar change listener, picks correct spot of audio based on user input
        sbTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updatePositionRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mediaPlayer != null)
                {
                    int newPosition = sbTime.getProgress();
                    mediaPlayer.seekTo(newPosition);
                    mediaPlayer.start();
                    handler.post(updatePositionRunnable);
                }
                else
                {
                    sbTime.setProgress(0);
                }

            }
        });

        //Handler to constantly update seekbar progress
        handler = new Handler(Looper.getMainLooper());
        updatePositionRunnable = new Runnable() {
            private long lastSaveTime = 0;
            private static final long SAVE_INTERVAL = 10000;
            @Override
            public void run() {
                if(mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    tvCurrentTime.setText(UIHelper.formatDuration(currentPosition));
                    sbTime.setProgress(currentPosition);

                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - lastSaveTime > SAVE_INTERVAL)
                    {
                        try {
                            SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, MainActivity.this);
                            lastSaveTime = currentTimeMillis;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    handler.postDelayed(this, 1000);
                }
            }
        };


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
                MediaPlayerHelper.handlePlayBackSpeedChange(selectedSpeed, mediaPlayer);
                UIHelper.showToast(MainActivity.this, "Selected: " + selectedSpeed);
                try {
                    SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, MainActivity.this);
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
            SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updatePositionRunnable);
    }

}