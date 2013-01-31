package com.tuenti.voice.example.ui;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragment;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;

import java.util.Collections;
import java.util.List;

public abstract class ItemListFragment<E>
    extends SherlockFragment
    implements LoaderManager.LoaderCallbacks<List<E>>
{
// ------------------------------ FIELDS ------------------------------

    /**
     * List items provided to {@link #onLoadFinished(Loader, List)}
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
    public Loader<List<E>> onCreateLoader( int id, Bundle args )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onLoadFinished( Loader<List<E>> loader, List<E> data )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onLoaderReset( Loader<List<E>> loader )
    {
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onActivityCreated( Bundle savedInstanceState )
    {
        super.onActivityCreated( savedInstanceState );
        getLoaderManager().initLoader( 0, null, this );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        return null;
    }

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView()
    {
        mListShown = false;
        mListView = null;
        super.onDestroyView();
    }

    /**
     * Callback when a list view item is clicked
     */
    public void onListItemClick( ListView l, View v, int position, long id )
    {
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState )
    {
        super.onViewCreated( view, savedInstanceState );

        mListView = (ListView) view.findViewById( android.R.id.list );
        mListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                onListItemClick( (ListView) parent, view, position, id );
            }
        } );
        mListView.setAdapter( createAdapter( mItems ) );
    }

    /**
     * Create adapter to display items
     *
     * @param items
     * @return adapter
     */
    protected abstract SingleTypeAdapter<E> createAdapter( final List<E> items );

    /**
     * Set list adapter to use on list view
     *
     * @param adapter
     * @return this fragment
     */
    protected ItemListFragment<E> setListAdapter( final ListAdapter adapter )
    {
        if ( mListView != null )
        {
            mListView.setAdapter( adapter );
        }
        return this;
    }
}
