package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SharedPreferencesHelper
{
    private static final String PREFS_NAME = "your_prefs_name";
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void savePlaybackPosition(Uri uri, MediaPlayer mediaPlayer, Context context) throws IOException {

        executorService.execute(() -> {
            try {
                if(mediaPlayer != null && "audiobook".equals(getAudioType(context, uri)) && uri != null)
                {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    String key = generateUniqueKey(uri);

                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).edit();
                    editor.putInt(key, currentPosition);
                    editor.apply();
                }
            } catch (IllegalStateException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void getSavedPlaybackPosition(Context context, Uri uri, PlaybackPositionCallback callback) {
        executorService.execute(() ->
        {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String key = generateUniqueKey(uri);
            int position = prefs.getInt(key, 0);
            Log.d("SharedPreferences", "Generated key: " + key);
            callback.onResult(position);
        });
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
                audioType = "audiobook";
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

    // Callback interfaces
    public interface SaveCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface PlaybackPositionCallback {
        void onResult(int position);
    }

    public interface AudioTypeCallback {
        void onResult(String audioType);
    }
}
