package com.tuenti.voice.example.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.tuenti.voice.example.R;

import java.util.Collections;
import java.util.List;

import static android.support.v4.app.LoaderManager.LoaderCallbacks;
import static android.widget.AdapterView.OnItemClickListener;

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
     * Progress bar
     */
    protected ProgressBar mProgressBar;

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
        displayProgressBar( false );
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
     * Refresh the fragment's list
     */
    public void refresh()
    {
        refresh( null );
    }

    /**
     * Create adapter to display items
     *
     * @param items
     * @return adapter
     */
    protected abstract SingleTypeAdapter<E> createAdapter( final List<E> items );

    /**
     * Display/hide the ProgressBar.
     *
     * @param display
     */
    protected void displayProgressBar( boolean display )
    {
        if ( mProgressBar != null )
        {
            setSupportProgressBarIndeterminateVisibility( display );
            ViewUtils.setGone( mProgressBar, !display );
        }
    }

    /**
     * Get serializable extra from activity's intent
     *
     * @param name
     * @return extra
     */
    @SuppressWarnings("unchecked")
    protected <V extends Parcelable> V getParcelableExtra( final String name )
    {
        return (V) getIntent().getParcelableExtra( name );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.item_list_activity );

        mListView = (ListView) findViewById( R.id.list );
        mListView.setOnItemClickListener( new OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                onListItemClick( (ListView) parent, view, position, id );
            }
        } );
        mListView.setAdapter( createAdapter( mItems ) );

        mProgressBar = (ProgressBar) findViewById( R.id.loading );

        getSupportLoaderManager().initLoader( 0, null, this );
    }

    @Override
    protected void onDestroy()
    {
        mListShown = false;
        mProgressBar = null;
        mListView = null;
        super.onDestroy();
    }

    private void refresh( final Bundle args )
    {
        displayProgressBar( true );
        getSupportLoaderManager().restartLoader( 0, args, this );
    }
}
