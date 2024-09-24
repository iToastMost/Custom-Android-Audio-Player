package com.CheekyLittleApps.audioplayer.helpers;

import android.content.Context;

public class UIHelper
{
    private Context context;
    private MediaPlayerHelper mediaPlayerHelper;

    public UIHelper(Context context, MediaPlayerHelper mediaPlayerHelper)
    {
        this.context = context;
        this.mediaPlayerHelper = mediaPlayerHelper;
    }
}
