package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.deezer.sdk.model.PaginatedList;
import com.deezer.sdk.model.Track;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.JsonRequestListener;
import com.deezer.sdk.network.request.event.OAuthException;

import fr.xgouchet.deezer.muzei.data.Preferences;
import fr.xgouchet.deezer.muzei.util.Constants;


public class MediaDataReceiver extends BroadcastReceiver {
    
    
    private DeezerConnect mConnect;
    
    private String mTrack, mArtist, mAlbum;
    
    private long mTimeStamp, mTrackId;
    
    private Context mContext;
    
    @Override
    public void onReceive(final Context context, final Intent intent) {
        
        mContext = context;
        
        // restore deezer connect
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, context);
        
        mTrackId = intent.getLongExtra("id", 0L);
        mTrack = intent.getStringExtra("track");
        mArtist = intent.getStringExtra("artist");
        mAlbum = intent.getStringExtra("album");
        mTimeStamp = System.currentTimeMillis();
        
        // Not a Deezer track 
        if (mTrackId == 0L) {
            
            Log.i("Looking for ", "\"" + mTrack + "\" in " + mAlbum + " (by " + mArtist + ")");
            try {
                searchMatchingAlbum();
            }
            catch (Exception e) {
                Log.e("Media", "Error ?", e);
            }
        } else {
            Log.i("Deezer Track", "\"" + mTrack + "\" #" + mTrackId);
            getTrackInfo();
        }
    }
    private void searchMatchingAlbum()
            throws MalformedURLException, OAuthException, IOException, DeezerError {
        DeezerRequest request = DeezerRequestFactory.requestSearchTracks(mTrack);
        mConnect.requestAsync(request, mSearchTracksRequestListener);
    }
    
    private void getTrackInfo() {
        DeezerRequest request = DeezerRequestFactory.requestTrack(mTrackId);
        mConnect.requestAsync(request, mTrackInfoRequestListener);
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Search track request listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private JsonRequestListener mSearchTracksRequestListener = new JsonRequestListener() {
        
        
        @SuppressWarnings("unchecked")
        @Override
        public void onResult(final Object result, final Object requestId) {
            
            PaginatedList<Track> tracks = (PaginatedList<Track>) result;
            Log.i("Search Result", "Result : " + tracks.size() + " tracks found");
            
            
            boolean found = false;
            for (Track track : tracks) {
                if (!TextUtils.equals(mTrack, track.getTitle())) {
                    Log.d("Track Title Mismatch ", mTrack + " =/= " + track.getTitle());
                    continue;
                }
                
                if (!TextUtils.equals(mArtist, track.getArtist().getName())) {
                    Log.d("Artist Name Mismatch ", mArtist + " =/= "
                            + track.getArtist().getName());
                    continue;
                }
                
                Log.i("Found it ", track.getAlbum().getTitle() + " #" + track.getAlbum().getId());
                Preferences.saveLastTrackInfo(mContext, track.getAlbum(), mTimeStamp);
                
                found = true;
                break;
            }
            
            if (!(found || TextUtils.isEmpty(tracks.getNextUrl()))) {
                String next = tracks.getNextUrl();
                
                Log.i("Search Result", "Not found, search next Page " + next);
                DeezerRequest request = new DeezerRequest(next);
                mConnect.requestAsync(request, mSearchTracksRequestListener);
            }
        }
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            Log.e("Search", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            Log.e("Search", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            Log.e("Search", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            Log.e("Search", "onDeezerError", e);
        }
        
        @Override
        public void onJSONParseException(final JSONException e, final Object requestId) {
            Log.e("Search", "onJSONParseException", e);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Track Info Request
    //////////////////////////////////////////////////////////////////////////////////////
    
    private JsonRequestListener mTrackInfoRequestListener = new JsonRequestListener() {
        
        @Override
        public void onResult(Object result, Object requestId) {
            if (result instanceof Track) {
                Track track = (Track) result;
                
                Log.i("Found it ", track.getAlbum().getTitle() + " #" + track.getAlbum().getId());
                Preferences.saveLastTrackInfo(mContext, track.getAlbum(), mTimeStamp);
            } else {
                
            }
        }
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            Log.e("Search", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            Log.e("Search", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            Log.e("Search", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            Log.e("Search", "onDeezerError", e);
        }
        
        @Override
        public void onJSONParseException(final JSONException e, final Object requestId) {
            Log.e("Search", "onJSONParseException", e);
        }
    };
    
}
