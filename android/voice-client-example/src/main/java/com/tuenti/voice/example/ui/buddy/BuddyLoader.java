package com.tuenti.voice.example.ui.buddy;

import android.content.Context;
import com.github.kevinsawicki.wishlist.AsyncLoader;
import com.tuenti.voice.core.data.Buddy;

import java.util.List;

public class BuddyLoader
    extends AsyncLoader<List<Buddy>>
{
// ------------------------------ FIELDS ------------------------------

    private List<Buddy> mBuddies;

// --------------------------- CONSTRUCTORS ---------------------------

    public BuddyLoader( final Context context, final List<Buddy> buddies )
    {
        super( context );
        mBuddies = buddies;
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public List<Buddy> loadInBackground()
    {
        return mBuddies;
    }
}