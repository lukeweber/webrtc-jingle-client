package com.tuenti.voice.example.ui.buddy;

import android.view.LayoutInflater;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.example.R;

public class BuddyListAdapter
    extends SingleTypeAdapter<Buddy>
{
// --------------------------- CONSTRUCTORS ---------------------------

    public BuddyListAdapter( final LayoutInflater inflater, final Buddy[] buddies )
    {
        super( inflater, R.layout.buddy_item );
        setItems( buddies );
    }

    @Override
    protected int[] getChildViewIds()
    {
        return new int[]{R.id.buddy_name};
    }

    @Override
    protected void update( final int position, final Buddy buddy )
    {
        setText( 0, buddy.getRemoteJid() );
    }
}
