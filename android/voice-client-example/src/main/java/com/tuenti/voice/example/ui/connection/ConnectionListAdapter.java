package com.tuenti.voice.example.ui.connection;

import android.view.LayoutInflater;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;

public class ConnectionListAdapter
    extends SingleTypeAdapter<Connection>
{
// --------------------------- CONSTRUCTORS ---------------------------

    public ConnectionListAdapter( final LayoutInflater inflater, final Connection[] connections )
    {
        super( inflater, R.layout.account_item );
        setItems( connections );
    }

    @Override
    protected int[] getChildViewIds()
    {
        return new int[]{R.id.account_name, R.id.presence, R.id.status};
    }

    @Override
    protected void update( final int position, final Connection connection )
    {
        setText( 0, connection.getUsername() );
        imageView( 1 ).setImageResource( getImageResource( connection ) );
        setText( 2, connection.getPresenceId() );
    }

    private int getImageResource( final Connection connection )
    {
        if ( connection.getPresenceId() == R.string.presence_available )
        {
            return R.drawable.presence_online;
        }
        return R.drawable.presence_offline;
    }
}
