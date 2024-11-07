package com.CheekyLittleApps.audioplayer.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Button;

import com.CheekyLittleApps.audioplayer.MainActivity;
import com.CheekyLittleApps.audioplayer.R;

import java.io.IOException;

public class MediaPlayerService extends Service
{
    private static final String CHANNEL_ID= "Media_Notification_Channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayerHelper mediaPlayerHelper;
    private MediaNotificationHelper mediaNotificationHelper;
    private MediaSessionCompat mediaSession;
    IBinder binder = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "MediaSessionTag");
        mediaPlayerHelper = new MediaPlayerHelper(this, mediaSession);
        mediaNotificationHelper = new MediaNotificationHelper(this, mediaSession);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = mediaNotificationHelper.showNotification(mediaPlayerHelper.getTitle(), mediaPlayerHelper.getArtist(), mediaPlayerHelper.getAlbumArt(), mediaPlayerHelper.isPlaying());

        if(notification != null)
        {
            startForeground(NOTIFICATION_ID, notification);
            Log.d("Notification", "Notification created");
        }

        if(intent != null && intent.getAction() != null)
        {
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "ACTION_PLAY_PAUSE":
                        MediaPlayerHelper.handlePlayButton();
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
        }


        return START_STICKY;
    }

    private void handlePlayPause() {
        if (mediaPlayerHelper.isPlaying()) {
            mediaPlayerHelper.pause();
        } else {
            mediaPlayerHelper.play();
        }
        //updateNotification();
    }

    // Update the media notification to reflect play/pause state
//    private void updateNotification() {
//        mediaNotificationHelper.showNotification(mediaPlayerHelper.getTitle(), mediaPlayerHelper.getArtist(), mediaPlayerHelper.getAlbumArt(), mediaPlayerHelper.isPlaying());
//    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder; // Not binding this service
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("d", "On destroy called in MPS");
        mediaPlayerHelper.release();
        mediaSession.release();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        super.onTaskRemoved(rootIntent);
        Log.d("d", "On task remove called MPS");
        if(mediaSession != null)
        {
            mediaSession.release();
        }

        if(mediaPlayerHelper != null)
        {
            mediaNotificationHelper.clearNotification();
        }
        if(mediaPlayerHelper != null)
        {
            MediaPlayerHelper.release();
        }

        stopSelf();
    }
}
