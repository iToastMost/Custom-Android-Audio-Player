package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.CheekyLittleApps.audioplayer.MainActivity;

import java.io.IOException;

public class MediaPlayerHelper
{
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updatePositionRunnable;
    private Context context;

    public MediaPlayerHelper(Context context, Handler handler, Runnable updatePositionRunnable) {
        this.context = context;
        this.handler = handler;
        this.updatePositionRunnable = updatePositionRunnable;
    }

    public static void handlePlayButton(Button btnPlay, MediaPlayer mediaPlayer, Handler handler, Runnable updatePositionRunnable)
    {
        if(mediaPlayer == null)
            return;

        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            btnPlay.setText("Play");
            handler.removeCallbacks(updatePositionRunnable);
        }
        else
        {
                mediaPlayer.start();
                btnPlay.setText("Pause");
                handler.post(updatePositionRunnable);
        }
    }

    public static void handleButtonForward(MediaPlayer mediaPlayer, Handler handler, Runnable updatePositionRunnable, String currentAudioType)
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
    }

    public static void handleButtonBack(MediaPlayer mediaPlayer, Handler handler, Runnable updatePositionRunnable, String currentAudioType)
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

        if(mediaPlayer != null)
        {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        }
    }

//    private void handleAudioFile(Uri uri, Spinner spinnerPlaybackSpeed, Uri mediaUri, SeekBar sbTime, Button btnPlay, String currentAudioType, ImageView ivCover, TextView tvTitle, TextView tvArtist, TextView tvSongLength)
//    {
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//
//        mediaPlayer = new MediaPlayer();
//
//        try
//        {
//            String playbackSpeed = spinnerPlaybackSpeed.getSelectedItem().toString();
//            mediaPlayer.setDataSource(this, uri);
//            mediaPlayer.prepare();
//
//            int currentTime = 0;
//
//            int savedPosition = SharedPreferencesHelper.getSavedPlaybackPosition(this, uri);
//
//            if(savedPosition > 0 && "audiobook".equals(SharedPreferencesHelper.getAudioType(this, mediaUri)))
//            {
//                currentTime = savedPosition;
//            }
//
//            mediaPlayer.seekTo(currentTime);
//            mediaPlayer.start();
//            MediaPlayerHelper.handlePlayBackSpeedChange(playbackSpeed, mediaPlayer);
//            sbTime.setMax(mediaPlayer.getDuration());
//            btnPlay.setText("Pause");
//            handler.post(updatePositionRunnable);
//
//            currentAudioType = SharedPreferencesHelper.getAudioType(this, mediaUri);
//        }
//        catch(IOException e)
//        {
//            e.printStackTrace();
//        }
//
//        //releases media player for next song if I decide to implement this feature
//        /*
//        mediaPlayer.setOnCompletionListener(mp -> {
//            mp.release();
//            mediaPlayer = null;
//            handler.removeCallbacks(updatePositionRunnable);
//        });
//         */
//
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        retriever.setDataSource(MainActivity.this, uri);
//
//        byte[] artBytes = retriever.getEmbeddedPicture();
//        if(artBytes != null)
//        {
//            Bitmap albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
//            ivCover.setImageBitmap(albumArt);
//        }
//        else
//        {
//
//        }
//
//        tvTitle.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
//        tvArtist.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
//
//        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//
//        if(duration != null)
//        {
//            long durationInMillis = Long.parseLong(duration);
//            long durationInSec = durationInMillis / 1000;
//            long minutes = durationInSec / 60;
//            long seconds = durationInSec % 60;
//            String songLength = String.format("%02d:%02d", minutes, seconds);
//            tvSongLength.setText(songLength);
//        }
//
//        if(retriever != null)
//        {
//            try
//            {
//                retriever.release();
//                retriever = null;
//            }catch (IOException e)
//            {
//                e.printStackTrace();
//            }
//
//        }
//
//    }
}

