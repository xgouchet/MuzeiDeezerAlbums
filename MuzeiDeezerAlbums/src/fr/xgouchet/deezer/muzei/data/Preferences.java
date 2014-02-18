package fr.xgouchet.deezer.muzei.data;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.deezer.sdk.model.Album;
import com.deezer.sdk.model.User;

import fr.xgouchet.deezer.muzei.app.ArtSourceService;
import fr.xgouchet.deezer.muzei.util.Constants;


/**
 * Handle the preferences
 * 
 * @author Xavier Gouchet
 * 
 */
public class Preferences {
    
    //////////////////////////////////////////////////////////////////////////////////////
    // The prefs keys
    //////////////////////////////////////////////////////////////////////////////////////
    private static final String PREF_USER_ID = "deezer_user_id";
    private static final String PREF_SOURCE = "deezer_source";
    private static final String PREF_LAST_TRACK_TIME = "deezer_last_timestamp";
    private static final String PREF_LAST_ALBUM_ID = "deezer_last_album_id";
    
    //////////////////////////////////////////////////////////////////////////////////////
    // The different values possible for source
    //////////////////////////////////////////////////////////////////////////////////////
    public static final int SOURCE_EDITO = 0;
    public static final int SOURCE_FAVS = 1;
    public static final int SOURCE_CUSTOM = 2;
    public static final int SOURCE_LAST_PLAYED = 3;
    
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Quickly accessible values for each prefs
    //////////////////////////////////////////////////////////////////////////////////////
    
    private static long sUserId;
    @Deprecated
    private static List<Long> sEditoIdList;
    private static int sSourceType;
    private static long sLastTrackTilestamp;
    private static long sLastTrackAlbumId;
    
    
    public static long getUserId() {
        return sUserId;
    }
    
    
    public static int getSourceType() {
        return sSourceType;
    }
    
    
    public static long getLastTrackAlbumId() {
        return sLastTrackAlbumId;
    }
    
    
    public static long getLastTrackTilestamp() {
        return sLastTrackTilestamp;
    }
    
    
    @Deprecated
    public static List<Long> getEditoIdList() {
        return sEditoIdList;
    }
    
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Utils methods to load and save preferences
    //////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Loads the shared preferences
     * 
     * @param context
     */
    public static void loadPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        sUserId = prefs.getLong(PREF_USER_ID, 0L);
        sLastTrackAlbumId = prefs.getLong(PREF_LAST_ALBUM_ID, 0L);
        sLastTrackTilestamp = prefs.getLong(PREF_LAST_TRACK_TIME, 0L);
        sSourceType = prefs.getInt(PREF_SOURCE, SOURCE_EDITO);
        
    }
    /**
     * Saves the current user id
     * 
     * @param context
     * @param user
     */
    public static void saveCurrentUser(Context context, User user) {
        // Save the user id
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(PREF_USER_ID, user.getId());
        editor.apply();
        
        sUserId = user.getId();
    }
    
    
    public static void saveSourceType(Context context, int source) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(PREF_SOURCE, source);
        editor.apply();
        
        sSourceType = source;
    }
    
    
    /**
     * Saves the last played track info
     * 
     * @param context
     * @param album
     * @param timestamp
     */
    public static void saveLastTrackInfo(final Context context, final Album album, long timestamp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        long lastSaved = prefs.getLong(Preferences.PREF_LAST_TRACK_TIME, 0L);
        
        // another track was played after this one
        if (lastSaved > timestamp) {
            return;
        }
        
        // Save data
        Editor editor = prefs.edit();
        editor.putLong(Preferences.PREF_LAST_TRACK_TIME, timestamp);
        editor.putLong(Preferences.PREF_LAST_ALBUM_ID, album.getId());
        editor.apply();
        
        sLastTrackAlbumId = album.getId();
        sLastTrackTilestamp = timestamp;
        
        // Request an update
        if (prefs.getInt(Preferences.PREF_SOURCE, Preferences.SOURCE_EDITO) == Preferences.SOURCE_LAST_PLAYED) {
            Intent intent = new Intent(context, ArtSourceService.class);
            intent.setAction(Constants.ACTION_UPDATE);
            context.startService(intent);
        }
        
        
    }
}
