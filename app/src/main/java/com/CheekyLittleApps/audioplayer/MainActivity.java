package com.CheekyLittleApps.audioplayer;

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
import android.widget.Toast;

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



        Button btnFileSelect = findViewById(R.id.btnFileSelect);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);

        spinnerPlaybackSpeed = findViewById(R.id.spinnerPlaybackSpeed);

        btnPlay = findViewById(R.id.btnPlay);

        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvSongLength = findViewById(R.id.tvSongLength);

        ivCover = findViewById(R.id.ivCover);

        sbTime = findViewById(R.id.sbTime);

        btnFileSelect.setOnClickListener(v -> {
            showFileChooser();
        });

        btnPlay.setOnClickListener(v -> {
            handlePlayButton();
        });

        btnForward.setOnClickListener(v -> {
            handleButtonForward();
        });

        btnBack.setOnClickListener(v -> {
            handleButtonBack();
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedPostion = prefs.getInt(KEY_POSITION, 0);

        if(mediaPlayer != null)
        {
            mediaPlayer.seekTo(savedPostion);
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
                    tvCurrentTime.setText(formatDuration(currentPosition));
                    sbTime.setProgress(currentPosition);

                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - lastSaveTime > SAVE_INTERVAL)
                    {
                        try {
                            savePlaybackPosition(mediaUri);
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
                handlePlayBackSpeedChange(selectedSpeed);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void handleButtonBack()
    {
        if(mediaPlayer == null)
            return;

        handler.removeCallbacks(updatePositionRunnable);

        int currentPos = mediaPlayer.getCurrentPosition();

        if(currentAudioType.equals("audiobook"))
        {
            currentPos -= 15000;
            mediaPlayer.seekTo(currentPos);
        }
        else
        {
            mediaPlayer.seekTo(0);
        }

        handler.post(updatePositionRunnable);

        try {
            savePlaybackPosition(mediaUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleButtonForward()
    {
        if(mediaPlayer == null)
            return;

        handler.removeCallbacks(updatePositionRunnable);

        int currentPos = mediaPlayer.getCurrentPosition();

        if(currentAudioType.equals("audiobook"))
        {
            currentPos += 15000;
            mediaPlayer.seekTo(currentPos);
        }
        else
        {
            mediaPlayer.seekTo(mediaPlayer.getDuration());
            mediaPlayer.pause();
        }

        handler.post(updatePositionRunnable);

        try {
            savePlaybackPosition(mediaUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void savePlaybackPosition(Uri uri) throws IOException {
        if(mediaPlayer != null && "audiobook".equals(getAudioType(uri)))
        {
            int currentPosition = mediaPlayer.getCurrentPosition();
            String key = generateUniqueKey(uri);

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt(key, currentPosition);
            editor.apply();
        }
    }

    private String getAudioType(Uri mediaUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String audioType = "unknown";

        try
        {
            retriever.setDataSource(this, mediaUri);

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = 0;
            if(durationStr != null)
            {
                durationMs = Long.parseLong(durationStr);
            }

            //if the duration of audio is over one hour it is classified as an audiobook, there are quite
            //long songs for example, Dream Theater having 30+ minute long songs so we dont want to misclassify
            if(durationMs > 60 * 60 * 1000)
            {
                audioType = "audiobook";
            }
            else
            {
                audioType = "song";
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            retriever.release();
        }

        return audioType;
    }


    //Handles the playback speeds of the media player
    private void handlePlayBackSpeedChange(String selectedSpeed)
    {
        Toast.makeText(this, "Selected: " + selectedSpeed, Toast.LENGTH_SHORT).show();

        float playbackSpeed = 1.0f;
        switch(selectedSpeed)
        {
            case "0.50x":
                playbackSpeed = 0.5f;
                break;

            case "0.75x":
                playbackSpeed = 0.75f;
                break;

            case "1.00x":
                playbackSpeed = 1.0f;
                break;

            case "1.25x":
                playbackSpeed = 1.25f;
                break;

            case "1.50x":
                playbackSpeed = 1.5f;
                break;

            case "1.75x":
                playbackSpeed = 1.75f;
                break;

            case "2.00x":
                playbackSpeed = 2.0f;
                break;
        }

        if(mediaPlayer != null)
        {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        }
    }

    private void showFileChooser()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");

        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"));
    }

    private void handlePlayButton()
    {
        if(mediaPlayer == null)
            return;

        if(mediaPlayer.isPlaying())
        {
            currentTime = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            btnPlay.setText("Play");
            handler.removeCallbacks(updatePositionRunnable);
            try {
                savePlaybackPosition(mediaUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            if(mediaPlayer != null)
            {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            try
            {
                mediaPlayer.setDataSource(this, mediaUri);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(currentTime);
                mediaPlayer.start();
                btnPlay.setText("Pause");
                handler.post(updatePositionRunnable);
                savePlaybackPosition(mediaUri);
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
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

            int savedPostion = getSavedPlaybackPosition(uri);

            if(savedPostion > 0 && "audiobook".equals(getAudioType(mediaUri)))
            {
                currentTime = savedPostion;
            }

            mediaPlayer.seekTo(currentTime);
            mediaPlayer.start();
            handlePlayBackSpeedChange(playbackSpeed);
            sbTime.setMax(mediaPlayer.getDuration());
            btnPlay.setText("Pause");
            handler.post(updatePositionRunnable);

            currentAudioType = getAudioType(mediaUri);
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

    private String formatDuration(long durationInMillis)
    {
        long totalSeconds = durationInMillis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private int getSavedPlaybackPosition(Uri uri)
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = generateUniqueKey(uri);
        return prefs.getInt(key, 0);
    }

    private String generateUniqueKey(Uri uri)
    {
        return "audiobook_position_" + uri.toString().hashCode();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        try {
            savePlaybackPosition(mediaUri);
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