package com.tuenti.voice.example.ui;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.example.R;

import java.util.Collections;
import java.util.List;

import static android.support.v4.app.LoaderManager.LoaderCallbacks;

public abstract class ItemListActivity<E>
    extends SherlockFragmentActivity
    implements LoaderCallbacks<List<E>>
{
// ------------------------------ FIELDS ------------------------------

    /**
     * List items provided to {@link #onLoadFinished(android.support.v4.content.Loader, List)}
     */
    protected List<E> mItems = Collections.emptyList();

    /**
     * List view
     */
    protected ListView mListView;

    /**
     * Is the list currently shown?
     */
    protected boolean mListShown;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public abstract Loader<List<E>> onCreateLoader( int id, Bundle args );

    @Override
    public void onLoadFinished( Loader<List<E>> loader, List<E> items )
    {
        mItems = items;
        mListView.setAdapter( createAdapter( mItems ) );
    }

    @Override
    public void onLoaderReset( Loader<List<E>> loader )
    {
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Callback when a list view item is clicked
     */
    public void onListItemClick( ListView l, View v, int position, long id )
    {
    }

    /**
     * Create adapter to display items
     *
     * @param items
     * @return adapter
     */
    protected abstract SingleTypeAdapter<E> createAdapter( final List<E> items );

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.item_list_activity );

        mListView = (ListView) findViewById( R.id.list );
        mListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                onListItemClick( (ListView) parent, view, position, id );
            }
        } );
        mListView.setAdapter( createAdapter( mItems ) );

        getSupportLoaderManager().initLoader( 0, null, this );
    }

    protected void refresh( final Bundle args )
    {
        getSupportLoaderManager().restartLoader( 0, args, this );
    }
}
