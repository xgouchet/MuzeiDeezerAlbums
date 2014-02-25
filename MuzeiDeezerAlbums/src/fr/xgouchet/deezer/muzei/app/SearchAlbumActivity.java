package fr.xgouchet.deezer.muzei.app;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.deezer.sdk.model.Album;
import com.deezer.sdk.model.PaginatedList;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.JsonRequestListener;
import com.deezer.sdk.network.request.event.OAuthException;
import com.squareup.picasso.Picasso;

import fr.xgouchet.deezer.muzei.R;
import fr.xgouchet.deezer.muzei.data.AlbumDao;
import fr.xgouchet.deezer.muzei.data.AlbumInfo;
import fr.xgouchet.deezer.muzei.util.Constants;


public class SearchAlbumActivity extends Activity {
    
    private DeezerConnect mConnect;
    private EditText mSearchInput;
    private TextView mEmptyText;
    private AlbumDao mAlbumDao;
    private AdapterView<ListAdapter> mGridView;
    private AlbumAdapter mAdapter;
    
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        setContentView(R.layout.activity_search);
        
        mAlbumDao = new AlbumDao(this);
        
        // setup gridview 
        mGridView = (GridView) findViewById(android.R.id.list);
        mGridView.setEmptyView(findViewById(android.R.id.empty));
        mGridView.setOnItemClickListener(mAlbumSelectListener);
        
        // setup search box
        mSearchInput = (EditText) findViewById(R.id.search_input);
        mSearchInput.setImeActionLabel(getString(android.R.string.search_go),
                EditorInfo.IME_ACTION_SEARCH);
        mSearchInput.setOnEditorActionListener(mEditorListener);
        
        // For now, let's make it empty 
        mEmptyText = (TextView) findViewById(R.id.text_empty);
        mEmptyText.setText("");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mConnect = new DeezerConnect(this, Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
    }
    
    
    private void search(final String query) {
        
        // progress
        setProgressBarVisibility(true);
        setProgressBarIndeterminate(true);
        mEmptyText.setText(R.string.loading);
        
        
        // Launch the request 
        DeezerRequest request = DeezerRequestFactory.requestSearchAlbums(query);
        mConnect.requestAsync(request, mSearchAlbumListener);
        
        // 
        List<Album> empty = Collections.emptyList();
        mGridView.setAdapter(new AlbumAdapter(this, empty));
        
        
    }
    
    private void hideProgress() {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                setProgressBarVisibility(false);
            }
        });
    }
    
    private void showToast(final int resource) {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                Toast.makeText(SearchAlbumActivity.this, resource, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // On Item Click listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnItemClickListener mAlbumSelectListener = new OnItemClickListener() {
        
        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                final long id) {
            Album album = mAdapter.getItem(position);
            
            AlbumInfo albumInfo = new AlbumInfo();
            albumInfo.id = album.getId();
            albumInfo.title = album.getTitle();
            albumInfo.artist = album.getArtist().getName();
            albumInfo.cover = album.getCoverUrl();
            
            mAlbumDao.addAlbum(albumInfo);
            
            
            
            finish();
        }
        
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Text Input listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnEditorActionListener mEditorListener = new OnEditorActionListener() {
        
        @Override
        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
            Log.d("Editor Action", "Action " + actionId + " / Key Event " + event);
            
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = mSearchInput.getText().toString();
                Log.i("Search", "q=" + query);
                
                // hide the keyboard ! 
                InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(mSearchInput.getWindowToken(), 0);
                
                search(query);
                return true;
            }
            
            return false;
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Search Request Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    
    private JsonRequestListener mSearchAlbumListener = new JsonRequestListener() {
        
        @SuppressWarnings("unchecked")
        @Override
        public void onResult(final Object result, final Object requestId) {
            
            hideProgress();
            
            final PaginatedList<Album> albums = (PaginatedList<Album>) result;
            Log.i("Search Result", "Result : " + albums.size() + " albums found");
            
            runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    mAdapter = new AlbumAdapter(SearchAlbumActivity.this, albums);
                    mGridView.setAdapter(mAdapter);
                    
                    if (albums.size() == 0) {
                        mEmptyText.setText(R.string.custom_album_no_result);
                    }
                }
            });
            
        }
        
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_search_albums);
//            Log.e("Search", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_search_albums);
//            Log.e("Search", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_search_albums);
//            Log.e("Search", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_search_albums);
//            Log.e("Search", "onDeezerError", e);
        }
        
        @Override
        public void onJSONParseException(final JSONException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_search_albums);
//            Log.e("Search", "onJSONParseException", e);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Info Adapter
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class AlbumAdapter extends ArrayAdapter<Album> {
        
        private final LayoutInflater mLayoutInflater;
        
        public AlbumAdapter(final Context context, final List<Album> list) {
            super(context, R.layout.item_album, list);
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            AlbumViewHolder holder;
            View view = convertView;
            
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.item_album, parent, false);
                
                holder = new AlbumViewHolder();
                holder.title = (TextView) view.findViewById(R.id.album_title);
                holder.cover = (ImageView) view.findViewById(R.id.album_cover);
                
                view.setTag(holder);
            } else {
                holder = (AlbumViewHolder) view.getTag();
            }
            
            Album album = getItem(position);
            holder.title.setText(album.getTitle());
            holder.cover.setImageResource(R.drawable.album_default);
            
            
            File cacheFolder = new File(getContext().getCacheDir(), "thumbs");
            cacheFolder.mkdirs();
            File cacheFile = new File(cacheFolder, Long.toString(album.getId()));
            
            if (cacheFile.exists()) {
                Picasso.with(getContext()).load(cacheFile).placeholder(R.drawable.album_default)
                        .into(holder.cover);
            } else {
                Picasso.with(getContext()).load(album.getCoverUrl())
                        .placeholder(R.drawable.album_default)
                        .into(holder.cover);
            }
            
            return view;
        }
    }
    
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Simple view holder
    //////////////////////////////////////////////////////////////////////////////////////
    private class AlbumViewHolder {
        
        public TextView title;
        public ImageView cover;
    }
    
    
    
}
