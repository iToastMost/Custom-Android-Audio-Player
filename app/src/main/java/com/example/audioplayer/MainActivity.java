package com.example.audioplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updatePositionRunnable;
    private Uri mediaUri;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ImageView ivCover;
    private int currentTime;

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

        });

        btnBack.setOnClickListener(v -> {
            if(mediaPlayer == null)
                return;

            mediaPlayer.seekTo(0);
        });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();

                        if(data != null)
                        {
                            Uri uri = data.getData();
                            handleAudioFile(uri);
                            mediaUri = uri;
                        }
                    }
                });

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

        handler = new Handler(Looper.getMainLooper());
        updatePositionRunnable = new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    tvCurrentTime.setText(formatDuration(currentPosition));
                    sbTime.setProgress(currentPosition);
                    handler.postDelayed(this, 1000);
                }
            }
        };
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
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            sbTime.setMax(mediaPlayer.getDuration());
            btnPlay.setText("Pause");
            handler.post(updatePositionRunnable);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updatePositionRunnable);
    }

}