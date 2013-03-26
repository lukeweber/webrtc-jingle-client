package com.tuenti.voice.example.ui.buddy;

import android.view.LayoutInflater;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.XmppPresenceAvailable;
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
        return new int[]{R.id.buddy_name, R.id.buddy_presence, R.id.buddy_status};
    }

    @Override
    protected void update( final int position, final Buddy buddy )
    {
        setText( 0, buddy.getName() );
        imageView( 1 ).setImageResource( getImageResource( buddy ) );
        setText( 2, getAvailable( buddy ) );
    }

    private int getAvailable( final Buddy buddy )
    {
        if ( buddy.getAvailable().equals( XmppPresenceAvailable.XMPP_PRESENCE_AVAILABLE ) )
        {
            return R.string.presence_available;
        }
        return R.string.presence_offline;
    }

    private int getImageResource( final Buddy buddy )
    {
        if ( buddy.getAvailable().equals( XmppPresenceAvailable.XMPP_PRESENCE_AVAILABLE ) )
        {
            return R.drawable.presence_online;
        }
        return R.drawable.presence_offline;
    }
}
