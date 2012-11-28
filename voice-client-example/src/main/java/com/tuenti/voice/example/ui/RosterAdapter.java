package com.tuenti.voice.example.ui;

import android.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.tuenti.voice.core.data.Buddy;

public class RosterAdapter
    extends ArrayAdapter<Buddy>
{
// ------------------------------ FIELDS ------------------------------

    private final Activity mActivity;

    private final Buddy[] mBuddies;

// --------------------------- CONSTRUCTORS ---------------------------

    public RosterAdapter( Activity activity, final Buddy[] buddies )
    {
        super( activity, R.layout.simple_list_item_1, buddies );
        mActivity = activity;
        mBuddies = buddies;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Adapter ---------------------

    @Override
    public View getView( int position, View convertView, ViewGroup parent )
    {
        View rowView = convertView;
        if ( rowView == null )
        {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            rowView = inflater.inflate( R.layout.simple_list_item_1, null );
            ViewHolder holder = new ViewHolder();
            holder.text = (TextView) rowView.findViewById( R.id.text1 );
            rowView.setTag( holder );
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.text.setText( mBuddies[position].getRemoteJid() );
        return rowView;
    }

// -------------------------- INNER CLASSES --------------------------

    static class ViewHolder
    {
        TextView text;
    }
}
