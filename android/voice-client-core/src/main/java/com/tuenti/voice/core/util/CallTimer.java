package com.tuenti.voice.core.util;

import android.os.Handler;
import android.os.SystemClock;
import com.tuenti.voice.core.data.Call;

public class CallTimer
    extends Handler
{
// ------------------------------ FIELDS ------------------------------

    private Call mCall;

    private long mInterval;

    private long mLastReportedTime;

    private OnTickListener mListener;

    private PeriodicTimerCallback mTimerCallback;

    private boolean mTimerRunning;

// --------------------------- CONSTRUCTORS ---------------------------

    public CallTimer( OnTickListener listener )
    {
        mListener = listener;
        mTimerCallback = new PeriodicTimerCallback();
    }

// -------------------------- OTHER METHODS --------------------------

    public void cancelTimer()
    {
        removeCallbacks( mTimerCallback );
        mTimerRunning = false;
    }

    public void startTimer( Call call )
    {
        cancelTimer();

        mCall = call;
        mCall.setCallStartTime( SystemClock.uptimeMillis() );
        mInterval = 1000;
        mLastReportedTime = mCall.getCallStartTime() - mInterval;

        periodicUpdateTimer();
    }

    private void periodicUpdateTimer()
    {
        if ( !mTimerRunning )
        {
            mTimerRunning = true;

            long now = SystemClock.uptimeMillis();
            long nextReport = mLastReportedTime + mInterval;

            while ( now >= nextReport )
            {
                nextReport += mInterval;
            }

            postAtTime( mTimerCallback, nextReport );
            mLastReportedTime = nextReport;

            if ( mCall != null )
            {
                updateElapsedTime();
            }
        }
    }

    private void updateElapsedTime()
    {
        if ( mListener != null )
        {
            long duration = SystemClock.uptimeMillis() - mCall.getCallStartTime();
            mListener.onTickForCallTimeElapsed( duration / 1000 );
        }
    }

// -------------------------- INNER CLASSES --------------------------

    public interface OnTickListener
    {
        void onTickForCallTimeElapsed( long timeElapsed );
    }

    private class PeriodicTimerCallback
        implements Runnable
    {
        @Override
        public void run()
        {
            mTimerRunning = false;
            periodicUpdateTimer();
        }
    }
}
