package com.tuenti.voice.core.util;

import android.os.Build;

public final class Compatibility
{
// -------------------------- STATIC METHODS --------------------------

    public static boolean isCompatible( int apiLevel )
    {
        return Build.VERSION.SDK_INT >= apiLevel;
    }
}
