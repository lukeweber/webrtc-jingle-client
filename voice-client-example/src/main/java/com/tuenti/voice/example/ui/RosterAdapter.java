package com.tuenti.voice.example.ui;

import android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.tuenti.voice.example.data.Buddy;

public class RosterAdapter
    extends BaseAdapter
{
// ------------------------------ FIELDS ------------------------------

    private final Buddy[] mBuddies;

    private final LayoutInflater mInflater;

// --------------------------- CONSTRUCTORS ---------------------------

    public RosterAdapter( final LayoutInflater inflater, final Buddy[] buddies )
    {
        mInflater = inflater;
        mBuddies = buddies;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Adapter ---------------------

    @Override
    public int getCount()
    {
        return mBuddies.length;
    }

    @Override
    public Buddy getItem( int position )
    {
        return mBuddies[position];
    }

    @Override
    public long getItemId( int position )
    {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent )
    {
        View rowView = mInflater.inflate( R.layout.simple_list_item_1, null );
        TextView textView = (TextView) rowView.findViewById( R.id.text1 );
        textView.setText( getItem( position ).getRemoteJid() );
        return rowView;
    }
}
