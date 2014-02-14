package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.deezer.sdk.model.Album;
import com.deezer.sdk.model.PaginatedList;
import com.deezer.sdk.model.Track;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.JsonRequestListener;
import com.deezer.sdk.network.request.event.OAuthException;

import fr.xgouchet.deezer.muzei.util.Constants;
import fr.xgouchet.deezer.muzei.util.Preferences;


public class MediaDataReceiver extends BroadcastReceiver {
    
    
    private DeezerConnect mConnect;
    
    private String mTrack, mArtist, mAlbum;
    
    private long mTimeStamp;
    
    private Context mContext;
    
    @Override
    public void onReceive(final Context context, final Intent intent) {
        
        mContext = context;
        
        // restore deezer connect
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, context);
        
        
        
        mTrack = intent.getStringExtra("track");
        mArtist = intent.getStringExtra("artist");
        mAlbum = intent.getStringExtra("album");
        mTimeStamp = System.currentTimeMillis();
        
        Log.i("Looking for ", "\"" + mTrack + "\" in " + mAlbum + " (by " + mArtist + ")");
        
        if (TextUtils.isEmpty(mAlbum)) {
            Log.w("No Album ?", "No Cover !");
            return;
        }
        
        try {
            searchMatchingAlbum();
        }
        catch (Exception e) {
            Log.e("Media", "Error ?", e);
        }
        
    }
    
    private void searchMatchingAlbum()
            throws MalformedURLException, OAuthException, IOException, DeezerError {
        DeezerRequest request = DeezerRequestFactory.requestSearchTracks(mTrack);
        mConnect.requestAsync(request, mSearchTracksRequestListener);
        
    }
    
    private void saveMatchingAlbum(final Album album) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        long lastSaved = prefs.getLong(Preferences.PREF_LAST_TRACK_TIME, 0L);
        
        // another track was played after
        if (lastSaved > mTimeStamp) {
            return;
        }
        
        // Save data
        Editor editor = prefs.edit();
        editor.putLong(Preferences.PREF_LAST_TRACK_TIME, mTimeStamp);
        editor.putLong(Preferences.PREF_LAST_ALBUM_ID, album.getId());
        
        editor.apply();
        
        
        if (prefs.getInt(Preferences.PREF_SOURCE, Preferences.SOURCE_EDITO) == Preferences.SOURCE_LAST_PLAYED) {
            // Request an update
            Intent intent = new Intent(mContext, ArtSourceService.class);
            intent.setAction(Constants.ACTION_UPDATE);
            
            mContext.startService(intent);
        }
    }
    
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
                saveMatchingAlbum(track.getAlbum());
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
        public void onOAuthException(final OAuthException e, final Object arg1) {
            Log.e("Search", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object arg1) {
            Log.e("Search", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object arg1) {
            Log.e("Search", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object arg1) {
            Log.e("Search", "onDeezerError", e);
        }
        
        @Override
        public void onJSONParseException(final JSONException e, final Object arg1) {
            Log.e("Search", "onJSONParseException", e);
        }
    };
    
    
}
