package fr.xgouchet.deezer.muzei.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import fr.xgouchet.deezer.muzei.R;
import fr.xgouchet.deezer.muzei.data.AlbumDao;
import fr.xgouchet.deezer.muzei.data.AlbumInfo;
import fr.xgouchet.deezer.muzei.util.Constants;


public class SearchAlbumActivity extends Activity {
    
    private DeezerConnect mConnect;
    private EditText mSearchInput;
    private AlbumDao mAlbumDao;
    private AdapterView<ListAdapter> mGridView;
    private AlbumAdapter mAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mConnect = new DeezerConnect(this, Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
    }
    
    
    private void search(String query, boolean force) {
        
        // progress
        setProgressBarVisibility(true);
        setProgressBarIndeterminate(true);
        
        
        // Launch the request 
        DeezerRequest request = DeezerRequestFactory.requestSearchAlbums(query);
        request.setId(Long.valueOf(force ? Long.MAX_VALUE : System.nanoTime()));
        mConnect.requestAsync(request, mSearchAlbumListener);
        
        // 
        if (force) {
            List<Album> empty = Collections.emptyList();
            mGridView.setAdapter(new AlbumAdapter(this, empty));
        }
        
    }
    
    private void hideProgress() {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                setProgressBarVisibility(false);
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // On Item Click listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnItemClickListener mAlbumSelectListener = new OnItemClickListener() {
        
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            Log.d("Editor Action", "Action " + actionId + " / Key Event " + event);
            
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = mSearchInput.getText().toString();
                Log.i("Search", "q=" + query);
                
                // TODO hide the keyboard ! 
                InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(mSearchInput.getWindowToken(), 0);
                
                search(query, true);
                return true;
            }
            
            return false;
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Search Request Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private long mLastRequestTimestamp;
    
    private JsonRequestListener mSearchAlbumListener = new JsonRequestListener() {
        
        @SuppressWarnings("unchecked")
        @Override
        public void onResult(Object result, Object requestId) {
            
            hideProgress();
            
            final PaginatedList<Album> albums = (PaginatedList<Album>) result;
            Log.i("Search Result", "Result : " + albums.size() + " albums found");
            
            Long requestTimestamp = (Long) requestId;
            
            if (requestTimestamp < mLastRequestTimestamp) {
                return;
            }
            
            mLastRequestTimestamp = requestTimestamp;
            
            runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    mAdapter = new AlbumAdapter(SearchAlbumActivity.this, albums);
                    mGridView.setAdapter(mAdapter);
                }
            });
            
        }
        
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            hideProgress();
            Log.e("Search", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            hideProgress();
            Log.e("Search", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            hideProgress();
            Log.e("Search", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            hideProgress();
            Log.e("Search", "onDeezerError", e);
        }
        
        public void onJSONParseException(JSONException e, Object requestId) {
            hideProgress();
            Log.e("Search", "onJSONParseException", e);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Info Adapter
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class AlbumAdapter extends ArrayAdapter<Album> {
        
        private final LayoutInflater mLayoutInflater;
        
        public AlbumAdapter(Context context, List<Album> list) {
            super(context, R.layout.item_album, list);
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
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
            holder.albumId = album.getId();
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
                        .into(new AlbumTarget(album, holder, cacheFile));
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
        public long albumId;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Target (for Picasso)
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class AlbumTarget implements Target {
        
        private final Album mAlbum;
        private final AlbumViewHolder mHolder;
        private final File mFile;
        
        public AlbumTarget(Album info, AlbumViewHolder holder, File file) {
            mAlbum = info;
            mHolder = holder;
            mFile = file;
        }
        
        
        @Override
        public void onPrepareLoad(Drawable arg0) {
            
        }
        
        @Override
        public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
            
            if (mHolder.albumId == mAlbum.getId()) {
                mHolder.cover.setImageBitmap(bitmap);
            }
            
            FileOutputStream output = null;
            
            try {
                output = new FileOutputStream(mFile);
                
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                output.flush();
            }
            catch (Exception e) {
                mFile.delete();
            }
            finally {
                try {
                    output.close();
                }
                catch (Exception e) {
                    // 
                }
            }
        }
        
        @Override
        public void onBitmapFailed(Drawable drawable) {
            Picasso.with(SearchAlbumActivity.this).load(mAlbum.getCoverUrl()).into(this);
        }
    }
    
}
