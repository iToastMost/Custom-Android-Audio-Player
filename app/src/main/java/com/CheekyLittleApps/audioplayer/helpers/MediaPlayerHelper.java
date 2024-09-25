//package com.CheekyLittleApps.audioplayer.helpers;
//
//import android.content.Context;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.Handler;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.SeekBar;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import java.io.IOException;
//
//public class MediaPlayerHelper
//{
//    private MediaPlayer mediaPlayer;
//    private Handler handler;
//    private Runnable updatePositionRunnable;
//    private Context context;
//
//    public MediaPlayerHelper(Context context, Handler handler, Runnable updatePositionRunnable) {
//        this.context = context;
//        this.handler = handler;
//        this.updatePositionRunnable = updatePositionRunnable;
//    }
//
//    public void handleButtonBack(String currentAudioType) {
//        if (mediaPlayer == null) return;
//
//        handler.removeCallbacks(updatePositionRunnable);
//
//        int currentPos = mediaPlayer.getCurrentPosition();
//
//        if ("audiobook".equals(currentAudioType)) {
//            currentPos -= 15000;
//            mediaPlayer.seekTo(currentPos);
//        } else {
//            mediaPlayer.seekTo(0);
//        }
//
//        handler.post(updatePositionRunnable);
//    }
//
//    public void handleButtonForward(String currentAudioType) {
//        if (mediaPlayer == null) return;
//
//        handler.removeCallbacks(updatePositionRunnable);
//
//        int currentPos = mediaPlayer.getCurrentPosition();
//
//        if ("audiobook".equals(currentAudioType)) {
//            currentPos += 15000;
//            mediaPlayer.seekTo(currentPos);
//        } else {
//            mediaPlayer.seekTo(mediaPlayer.getDuration());
//            mediaPlayer.pause();
//        }
//
//        handler.post(updatePositionRunnable);
//    }
//
//    public void handlePlayBackSpeedChange(String selectedSpeed) {
//        float playbackSpeed = 1.0f;
//        switch (selectedSpeed) {
//            case "0.50x":
//                playbackSpeed = 0.5f;
//                break;
//            case "0.75x":
//                playbackSpeed = 0.75f;
//                break;
//            case "1.00x":
//                playbackSpeed = 1.0f;
//                break;
//            case "1.25x":
//                playbackSpeed = 1.25f;
//                break;
//            case "1.50x":
//                playbackSpeed = 1.5f;
//                break;
//            case "1.75x":
//                playbackSpeed = 1.75f;
//                break;
//            case "2.00x":
//                playbackSpeed = 2.0f;
//                break;
//        }
//
//        if (mediaPlayer != null) {
//            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
//        }
//    }
//
//    public void handlePlayButton(Button btnPlay, int currentTime, Uri mediaUri) throws IOException {
//        if (mediaPlayer == null) {
//            mediaPlayer = new MediaPlayer();
//            mediaPlayer.setDataSource(context, mediaUri);
//            mediaPlayer.prepare();
//        }
//
//        if (mediaPlayer.isPlaying()) {
//            // Pause and save current time
//            mediaPlayer.pause();
//            btnPlay.setText("Play");
//            handler.removeCallbacks(updatePositionRunnable);
//        } else {
//            // Resume playing from the saved current time
//            mediaPlayer.seekTo(currentTime);
//            mediaPlayer.start();
//            btnPlay.setText("Pause");
//            handler.post(updatePositionRunnable);
//        }
//    }
//
//    public void handleAudioFile(Uri uri, Spinner spinnerPlaybackSpeed, Button btnPlay, SeekBar sbTime, TextView tvTitle, TextView tvArtist, TextView tvSongLength, ImageView ivCover) throws IOException {
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//
//        mediaPlayer = new MediaPlayer();
//
//        mediaPlayer.setDataSource(context, uri);
//        mediaPlayer.prepare();
//
//        String playbackSpeed = spinnerPlaybackSpeed.getSelectedItem().toString();
//
//        int savedPosition = SharedPreferencesHelper.getSavedPlaybackPosition(context, uri);
//
//        String audioType = SharedPreferencesHelper.getAudioType(context, uri);
//
//        if (savedPosition > 0 && "audiobook".equals(audioType)) {
//            mediaPlayer.seekTo(savedPosition);
//        }
//
//        mediaPlayer.start();
//        handlePlayBackSpeedChange(playbackSpeed);
//        sbTime.setMax(mediaPlayer.getDuration());
//        btnPlay.setText("Pause");
//        handler.post(updatePositionRunnable);
//
//        UIHelper.updateMediaMetadata(context, uri, tvTitle, tvArtist, tvSongLength, ivCover);
//    }
//
//    public void release() {
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//        handler.removeCallbacks(updatePositionRunnable);
//    }
//
//    public int getCurrentPosition() {
//        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
//    }
//
//    public int getDuration() {
//        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
//    }
//
//    public void seekTo(int position) {
//        if (mediaPlayer != null) {
//            mediaPlayer.seekTo(position);
//        }
//    }
//}
