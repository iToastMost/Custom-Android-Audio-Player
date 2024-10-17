package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class SharedPreferencesHelper
{
    private static final String PREFS_NAME = "your_prefs_name";

    public static void savePlaybackPosition(Uri uri, MediaPlayer mediaPlayer, Context context) throws IOException {
        if(mediaPlayer != null && "audiobook".equals(getAudioType(context, uri)))
        {
            int currentPosition = mediaPlayer.getCurrentPosition();
            String key = generateUniqueKey(uri);

            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
            editor.putInt(key, currentPosition);
            editor.apply();
        }
    }

    public static int getSavedPlaybackPosition(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = generateUniqueKey(uri);
        return prefs.getInt(key, 0);
    }

    public static String getAudioType(Context context, Uri uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String audioType = "unknown";

        try {
            if (uri != null) {
                retriever.setDataSource(context, uri);
            } else {
                Log.e("MediaMetadataRetriever", "URI is null, cannot retrieve metadata");
            }


            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = 0;
            if (durationStr != null) {
                durationMs = Long.parseLong(durationStr);
            }

            if (durationMs > 60 * 60 * 1000) {
                audioType = "audiobook";
            } else {
                audioType = "song";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }

        return audioType;
    }

    private static String generateUniqueKey(Uri uri) {
        return "audiobook_position_" + uri.toString().hashCode();
    }
}
