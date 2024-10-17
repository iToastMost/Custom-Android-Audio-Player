package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.CheekyLittleApps.audioplayer.MainActivity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayerHelper
{

    private static MediaPlayer mediaPlayer;
    private static Handler handler;
    private static Runnable updatePositionRunnable;
    private static Context context;
    private static int currentTime;
    private static String currentAudioType = "";
    private static Uri currentUri;
    private static MediaNotificationHelper notificationHelper;
    private static MediaSessionCompat mediaSession;
    private static String title;
    private static String artist;
    static Bitmap albumArt = null;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public MediaPlayerHelper(Context context, MediaSessionCompat mediaSession) {
        this.context = context;
        this.handler = new Handler();
        this.mediaPlayer = new MediaPlayer();
        this.currentUri = currentUri;
        notificationHelper = new MediaNotificationHelper(context, mediaSession);
    }

    public void startUpdatingCurrentTime(SeekBar seekBar, TextView tvCurrentTime, Uri mediaUri, MainActivity activity) {
        updatePositionRunnable = new Runnable() {
            private long lastSaveTime = 0;
            private static final long SAVE_INTERVAL = 10000;

            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    tvCurrentTime.setText(UIHelper.formatDuration(currentPosition));
                    seekBar.setProgress(currentPosition);

                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - lastSaveTime > SAVE_INTERVAL) {
                        try {
                            SharedPreferencesHelper.savePlaybackPosition(mediaUri, mediaPlayer, activity);
                            lastSaveTime = currentTimeMillis;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updatePositionRunnable);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {

                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(UIHelper.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                handler.removeCallbacks(updatePositionRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                handler.post(updatePositionRunnable);
            }
        });
    }

    public void stopUpdatingCurrentTime() {
        if (updatePositionRunnable != null) {
            handler.removeCallbacks(updatePositionRunnable);
        }
    }

    public static void handlePlayButton()
    {
        if(mediaPlayer == null)
            return;

        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            MainActivity.updatePlayButtonText("Play");
            mainHandler.post(() -> MainActivity.updatePlayButtonText("Play"));
            handler.removeCallbacks(updatePositionRunnable);
            notificationHelper.updateNotification(title, artist, albumArt, false);

        }
        else
        {
            mediaPlayer.start();
            MainActivity.updatePlayButtonText("Pause");
            handler.post(updatePositionRunnable);
            notificationHelper.updateNotification(title, artist, albumArt, true);

        }
    }

    public static void handleButtonForward(Uri mediaUri) throws IOException {

        executorService.execute(() ->
        {
            if(mediaPlayer == null)
                return;

            handler.removeCallbacks(updatePositionRunnable);

            int currentPos = mediaPlayer.getCurrentPosition();
            try {
                currentAudioType = SharedPreferencesHelper.getAudioType(context, mediaUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

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
            notificationHelper.updateNotification(title, artist, albumArt, isPlaying());
        });

    }

    public static void handleButtonBack(Uri mediaUri) throws IOException {
        if(mediaPlayer == null)
            return;

        handler.removeCallbacks(updatePositionRunnable);

        int currentPos = mediaPlayer.getCurrentPosition();
        currentAudioType = SharedPreferencesHelper.getAudioType(context, mediaUri);

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
        notificationHelper.updateNotification(title, artist, albumArt, isPlaying());
    }

    //Handles the playback speeds of the media player
    public static void handlePlayBackSpeedChange(String selectedSpeed, MediaPlayer mediaPlayer)
    {
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

        try {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        } catch (IllegalStateException e) {
            // Log or handle the error appropriately
            e.printStackTrace();
        }
    }


    public static void handleAudioFile(Uri uri, Spinner spinnerPlaybackSpeed, Button btnPlay, SeekBar sbTime, ImageView ivCover, TextView tvTitle, TextView tvArtist, TextView tvSongLength, Uri mediaUri, TextView tvCurrentTime)
    {
        try
        {
            currentUri = uri;
            mediaPlayer.reset();
            String playbackSpeed = spinnerPlaybackSpeed.getSelectedItem().toString();
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepare();

            currentTime = 0;

            int savedPosition = SharedPreferencesHelper.getSavedPlaybackPosition(context, uri);

            if(savedPosition > 0 && "audiobook".equals(SharedPreferencesHelper.getAudioType(context, mediaUri)))
            {
                currentTime = savedPosition;
            }


            tvCurrentTime.setText(UIHelper.formatDuration(currentTime));
            mediaPlayer.seekTo(currentTime);
            mediaPlayer.start();
            handlePlayBackSpeedChange(playbackSpeed, mediaPlayer);
            sbTime.setMax(mediaPlayer.getDuration());
            btnPlay.setText("Pause");
            handler.post(updatePositionRunnable);

            currentAudioType = SharedPreferencesHelper.getAudioType(context, mediaUri);
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
        retriever.setDataSource(context, uri);

        byte[] artBytes = retriever.getEmbeddedPicture();
        albumArt = null;
        if(artBytes != null)
        {
            albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            ivCover.setImageBitmap(albumArt);
        }

        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        tvTitle.setText(title);
        tvArtist.setText(artist);

        Bitmap resized = null;
        resized = resizeBitmap(albumArt, 256, 256);

        notificationHelper.showNotification(title, artist, resized, isPlaying());

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);


        if(duration != null )
        {
            long durationInMillis = Long.parseLong(duration);
            long durationInSec = durationInMillis / 1000;
            long minutes = (durationInSec % 3600) / 60;
            long seconds = durationInSec % 60;
            long hours = durationInSec / 3600;
            String songLength;
            if(hours > 0)
            {
                songLength = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }
            else
            {
                songLength = String.format("%02d:%02d", minutes, seconds);
            }
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

    public static MediaPlayer getMediaPlayer()
    {
        return mediaPlayer;
    }

    public static Uri getUri()
    {
        return currentUri;
    }

    public static String getTitle()
    {
        return title;
    }

    public static String getArtist()
    {
        return artist;
    }

    public static Bitmap getAlbumArt()
    {
        return albumArt;
    }

    private void setMediaPlayer(MediaPlayer mediaPlayer)
    {
        this.mediaPlayer = mediaPlayer;
    }

    public static boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public static Bitmap resizeBitmap(Bitmap originalImage, int targetWidth, int targetHeight) {
        if (originalImage == null) {
            return null;
        }

        // Calculate the aspect ratio
        float aspectRatio = originalImage.getWidth() / (float) originalImage.getHeight();

        int newWidth = targetWidth;
        int newHeight = targetHeight;

        // Adjust width/height to preserve aspect ratio
        if (aspectRatio > 1) { // Landscape
            newHeight = Math.round(targetWidth / aspectRatio);
        } else if (aspectRatio < 1) { // Portrait
            newWidth = Math.round(targetHeight * aspectRatio);
        }

        // Scale the bitmap to the new dimensions
        return Bitmap.createScaledBitmap(originalImage, newWidth, newHeight, false);
    }
}

