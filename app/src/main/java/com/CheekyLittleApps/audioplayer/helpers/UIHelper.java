package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import java.io.IOException;

public class UIHelper
{
    public static void showFileChooser(ActivityResultLauncher<Intent> filePickerLauncher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"));
    }

    public static void updateMediaMetadata(Context context, Uri uri, TextView tvTitle, TextView tvArtist, TextView tvSongLength, ImageView ivCover) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, uri);

        byte[] artBytes = retriever.getEmbeddedPicture();
        if (artBytes != null) {
            Bitmap albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            ivCover.setImageBitmap(albumArt);
        }

        tvTitle.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        tvArtist.setText(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration != null) {
            long durationInMillis = Long.parseLong(duration);
            long durationInSec = durationInMillis / 1000;
            long minutes = durationInSec / 60;
            long seconds = durationInSec % 60;
            String songLength = String.format("%02d:%02d", minutes, seconds);
            tvSongLength.setText(songLength);
        }

        retriever.release();
    }

    public static String formatDuration(long durationInMillis) {
        long durationInSec = durationInMillis / 1000;
        long hours = durationInSec / 3600;
        long minutes = (durationInSec % 3600) / 60;
        long seconds = durationInSec % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
