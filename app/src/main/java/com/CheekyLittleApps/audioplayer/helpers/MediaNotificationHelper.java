package com.CheekyLittleApps.audioplayer.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.Build;

import com.CheekyLittleApps.audioplayer.R;

public class MediaNotificationHelper
{
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID= "Media_Notification_Channel";
    private final NotificationManager notificationManager;
    private final Context context;
    private final MediaSessionCompat mediaSession;


    public MediaNotificationHelper(Context context, MediaSessionCompat mediaSession) {
        this.context = context;
        this.mediaSession = mediaSession;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "Music Playback";
//            String description = "Channel for music playback notifications";
//            int importance = NotificationManager.IMPORTANCE_LOW; // Adjust as needed
//            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
//            channel.setDescription(description);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(context, MediaPlayerService.class); // Replace with your service
        intent.setAction(action);

        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_MUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        return PendingIntent.getService(context, 0, intent, flag);
    }



    public Notification showNotification(String title, String artist, Bitmap albumArt, Boolean isPlaying)
    {
        PendingIntent playPauseIntent = createPendingIntent("ACTION_PLAY_PAUSE");
        PendingIntent nextIntent = createPendingIntent("ACTION_NEXT");
        PendingIntent previousIntent = createPendingIntent("ACTION_PREVIOUS");

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow;

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action.Builder(
                playPauseIcon,
                "Play",
                playPauseIntent
        ).build();

        NotificationCompat.Action nextAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                "Next",
                nextIntent
        ).build();

        NotificationCompat.Action previousAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                "Previous",
                previousIntent
        ).build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(albumArt)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1)
                        .setMediaSession(mediaSession.getSessionToken()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        return builder.build();
    }

    public void updatePlaybackState(boolean isPlaying) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "YOUR_CHANNEL_ID");

        if (isPlaying) {
            builder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_pause);
        } else {
            builder.setOngoing(false)
                    .setSmallIcon(R.drawable.ic_play_arrow);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateNotification(String title, String artist, Bitmap albumArt, boolean isPlaying)
    {
        Notification notification = showNotification(title, artist, albumArt, isPlaying);
        if(notification != null)
        {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void clearNotification()
    {
        notificationManager.cancel(NOTIFICATION_ID);
    }

}
