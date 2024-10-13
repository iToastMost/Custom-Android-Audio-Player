package com.CheekyLittleApps.audioplayer.helpers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.CheekyLittleApps.audioplayer.MainActivity;
import com.CheekyLittleApps.audioplayer.R;

import java.io.IOException;

public class MediaPlayerService extends Service
{

    private MediaPlayerHelper mediaPlayerHelper;
    private MediaNotificationHelper mediaNotificationHelper;
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "MediaSessionTag");
        mediaPlayerHelper = new MediaPlayerHelper(this, mediaSession);
        mediaNotificationHelper = new MediaNotificationHelper(this, mediaSession);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case "ACTION_PLAY_PAUSE":
                    handlePlayPause();
                    break;
                case "ACTION_NEXT":
                    try {
                        MediaPlayerHelper.handleButtonForward(MediaPlayerHelper.getUri());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "ACTION_PREVIOUS":
                    try {
                        MediaPlayerHelper.handleButtonBack(MediaPlayerHelper.getUri());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private void handlePlayPause() {
        if (mediaPlayerHelper.isPlaying()) {
            mediaPlayerHelper.pause();
        } else {
            mediaPlayerHelper.play();
        }
        updateNotification();
    }

    // Update the media notification to reflect play/pause state
    private void updateNotification() {
        mediaNotificationHelper.showNotification(mediaPlayerHelper.getTitle(), mediaPlayerHelper.getArtist(), mediaPlayerHelper.getAlbumArt(), mediaPlayerHelper.isPlaying());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding this service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayerHelper.release();
        mediaSession.release();
    }
}
