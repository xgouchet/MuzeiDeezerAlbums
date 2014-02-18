package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.deezer.sdk.model.Album;
import com.deezer.sdk.model.PaginatedList;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.JsonUtils;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.OAuthException;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import fr.xgouchet.deezer.muzei.data.AlbumDao;
import fr.xgouchet.deezer.muzei.data.AlbumInfo;
import fr.xgouchet.deezer.muzei.data.EditoDao;
import fr.xgouchet.deezer.muzei.data.EditoInfo;
import fr.xgouchet.deezer.muzei.data.Preferences;
import fr.xgouchet.deezer.muzei.util.Constants;


public class ArtSourceService extends RemoteMuzeiArtSource {
    
    private static final String SOURCE_NAME = "DeezerAlbumCoverArtSource";
    
    private static final String USER_ALBUMS = "user/%d/albums";
    private static final String EDITO_ALBUMS = "editorial/%d/selection";
    
    private Random mRandom;
    
    private long mCurrentAlbumId;
    
    private DeezerConnect mConnect;
    private EditoDao mEditoDao;
    private AlbumDao mAlbumDao;
    
    public ArtSourceService() {
        super(SOURCE_NAME);
        mRandom = new Random(System.nanoTime());
    }
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
        
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, getBaseContext());
        
        mEditoDao = new EditoDao(this);
        mAlbumDao = new AlbumDao(this);
    }
    
    @Override
    protected void onHandleIntent(final Intent intent) {
        
        if (intent == null) {
            return;
        }
        
        String action = intent.getAction();
        Log.i("HandleIntent", "action = " + action);
        
        
        if (Constants.ACTION_UPDATE.equals(action)) {
            // schedule update in a second
            scheduleUpdate(System.currentTimeMillis() + 1000L);
        } else {
            super.onHandleIntent(intent);
        }
    }
    
    
    @Override
    protected void onTryUpdate(final int reason)
            throws RetryException {
        
        Preferences.loadPreferences(this);
        
        Artwork current = getCurrentArtwork();
        String token = current == null ? "" : current.getToken();
        
        try {
            mCurrentAlbumId = Long.valueOf(token);
        }
        catch (NumberFormatException e) {
            mCurrentAlbumId = 0L;
        }
        
        switch (Preferences.getSourceType()) {
            case Preferences.SOURCE_EDITO:
                Log.i("Source", "Edito");
                publishEdito();
                break;
            case Preferences.SOURCE_FAVS:
                if (Preferences.getUserId() == 0L) {
                    Log.i("Source", "No User, no Favs -> Edito");
                    publishEdito();
                } else {
                    Log.i("Source", "User Favs");
                    publishUserAlbum();
                }
                break;
            case Preferences.SOURCE_LAST_PLAYED:
                Log.i("Source", "Playing");
                publishLastPlayedTrack();
                break;
            case Preferences.SOURCE_CUSTOM:
                Log.i("Source", "Custom List");
                publishCustomAlbum();
                break;
        
        }
        
    }
    
    /**
     * @throws RetryException
     * 
     */
    private void publishEdito()
            throws RetryException {
        
        
        // select a random edito
        EditoInfo editoInfo = mEditoDao.getRandomEdito();
        long editoId;
        
        if (editoInfo == null) {
            editoId = 0;
        } else {
            editoId = editoInfo.id;
        }
        
        // Get all the albums from the edito
        List<Album> albums;
        try {
            albums = getAlbumsList(String.format(EDITO_ALBUMS, editoId));
        }
        catch (Exception e) {
            throw new RetryException();
        }
        
        publishRandomAlbum(albums);
    }
    
    private void publishCustomAlbum()
            throws RetryException {
        
        // select a random album
        AlbumInfo albumInfo = mAlbumDao.getRandomAlbum();
        if (albumInfo == null) {
            return;
        }
        
        Album album;
        try {
            album = getAlbum(albumInfo.id);
        }
        catch (Exception e) {
            throw new RetryException();
        }
        
        publishAlbum(album);
    }
    
    /**
     * 
     * @throws RetryException
     */
    private void publishUserAlbum()
            throws RetryException {
        // Get all the albums from the user
        List<Album> albums;
        try {
            albums = getAlbumsList(String.format(USER_ALBUMS, Preferences.getUserId()));
        }
        catch (Exception e) {
            throw new RetryException();
        }
        
        publishRandomAlbum(albums);
    }
    
    private void publishLastPlayedTrack()
            throws RetryException {
        
        long albumId = Preferences.getLastTrackAlbumId();
        // ignore if  no album detected 
        if (albumId == 0L) {
            return;
        }
        
        // get last played album info
        Album album;
        try {
            album = getAlbum(albumId);
        }
        catch (Exception e) {
            throw new RetryException();
        }
        
        publishAlbum(album);
    }
    /**
     * Publish a random album from the given list
     * 
     * @param albums
     */
    private void publishRandomAlbum(final List<Album> albums) {
        // Get a random album from the list
        Album randomAlbum = null;
        
        // we need a random album that is NOT the current one
        while ((randomAlbum == null) || (mCurrentAlbumId == randomAlbum.getId())) {
            int randomIndex = mRandom.nextInt(albums.size());
            randomAlbum = albums.get(randomIndex);
        }
        
        // publish
        publishAlbum(randomAlbum);
    }
    
    /**
     * Publish the artwork from an Album Cover
     * 
     * @param album
     */
    private void publishAlbum(final Album album) {
        
        Artwork.Builder builder = new Artwork.Builder();
        builder.title(album.getTitle());
        builder.byline(album.getArtist().getName());
        builder.imageUri(Uri.parse(album.getCoverUrl() + "?size=" + Constants.COVER_SIZE));
        builder.token(Long.toString(album.getId()));
        
        if (album.getLink() != null) {
            builder.viewIntent(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(album.getLink())));
        }
        
        // Publish the artwork
        publishArtwork(builder.build());
    }
    
    /**
     * 
     * @return
     * @throws MalformedURLException
     * @throws OAuthException
     * @throws IOException
     * @throws DeezerError
     * @throws JSONException
     */
    @SuppressWarnings("unchecked")
    private List<Album> getAlbumsList(final String firstPageUrl)
            throws MalformedURLException, OAuthException, IOException, DeezerError, JSONException {
        
        List<Album> albums = new LinkedList<Album>();
        String nextUrl = firstPageUrl;
        
        while (nextUrl != null) {
            // Get albums from the User ID
            DeezerRequest request = new DeezerRequest(nextUrl);
            String response = mConnect.requestSync(request);
            
            // parse the result into a valid entity (either JSONObject, JSONArray, or a primitive type)
            final Object json = new JSONTokener(response).nextValue();
            if (!(json instanceof JSONObject)) {
                throw new JSONException("Unexpected JSON response " + response);
            }
            
            // Get the result as a Paginated list 
            PaginatedList<Album> paginated = (PaginatedList<Album>) JsonUtils
                    .deserializeObject((JSONObject) json);
            nextUrl = paginated.getNextUrl();
            
            // store fetched albums 
            albums.addAll(paginated);
        }
        
        
        return albums;
    }
    
    private Album getAlbum(final long id)
            throws MalformedURLException, OAuthException, IOException, DeezerError, JSONException {
        DeezerRequest albumRequest = DeezerRequestFactory.requestAlbum(id);
        String response = mConnect.requestSync(albumRequest);
        
        
        // parse the result into a valid entity (either JSONObject, JSONArray, or a primitive type)
        final Object json = new JSONTokener(response).nextValue();
        if (!(json instanceof JSONObject)) {
            throw new JSONException("Unexpected JSON response " + response);
        }
        
        return (Album) JsonUtils.deserializeObject((JSONObject) json);
        
    }
    
    
}
