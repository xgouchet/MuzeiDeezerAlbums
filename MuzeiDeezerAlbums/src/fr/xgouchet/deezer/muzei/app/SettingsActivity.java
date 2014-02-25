package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.deezer.sdk.model.User;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.connect.event.DialogError;
import com.deezer.sdk.network.connect.event.DialogListener;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.JsonRequestListener;
import com.deezer.sdk.network.request.event.OAuthException;

import fr.xgouchet.deezer.muzei.R;
import fr.xgouchet.deezer.muzei.data.Preferences;
import fr.xgouchet.deezer.muzei.task.FetchUserThumbnailTask;
import fr.xgouchet.deezer.muzei.task.FetchUserThumbnailTask.UserThumbnailTaskListener;
import fr.xgouchet.deezer.muzei.util.Constants;


/**
 * 
 * @author Xavier Gouchet
 * 
 */
public class SettingsActivity extends Activity {
    
    
    private boolean mIsLoggedIn = false;
    private DeezerConnect mConnect;
    
    
    private ImageView mUserAvatar;
    private TextView mUserName;
    
    private RadioButton mSourceFavs, mSourceEdito, mSourceCustom, mSourcePlaying;
    private TextView mTipText;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Transitions
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        // UI
        setContentView(R.layout.activity_settings);
        
        
        // WIDGETS
        mUserName = (TextView) findViewById(R.id.user_name);
        mUserAvatar = (ImageView) findViewById(R.id.user_avatar);
        
        mSourceFavs = (RadioButton) findViewById(R.id.radio_fav_albums);
        mSourceEdito = (RadioButton) findViewById(R.id.radio_edito_albums);
        mSourceCustom = (RadioButton) findViewById(R.id.radio_custom_albums);
        mSourcePlaying = (RadioButton) findViewById(R.id.radio_playing_albums);
        
        ((RadioGroup) findViewById(R.id.group_source))
                .setOnCheckedChangeListener(mRadioButtonChangedListener);
        
        mTipText = (TextView) findViewById(R.id.text_tip);
        
    }
    
    
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Preferences.loadPreferences(this);
        
        // Restore all 
        switch (Preferences.getSourceType()) {
            case Preferences.SOURCE_EDITO:
                mSourceEdito.setChecked(true);
                break;
            case Preferences.SOURCE_FAVS:
                mSourceFavs.setChecked(true);
                break;
            case Preferences.SOURCE_CUSTOM:
                mSourceCustom.setChecked(true);
                break;
            case Preferences.SOURCE_LAST_PLAYED:
                mSourcePlaying.setChecked(true);
                break;
        }
        
        updateTip();
        
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
        
        setUserLogged(mConnect.isSessionValid());
        
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        Intent intent = new Intent(this, ArtSourceService.class);
        intent.setAction(Constants.ACTION_UPDATE);
        startService(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        
        menu.findItem(R.id.action_login).setVisible(!mIsLoggedIn);
        menu.findItem(R.id.action_logout).setVisible(mIsLoggedIn);
        
        switch (Preferences.getSourceType()) {
            case Preferences.SOURCE_CUSTOM:
            case Preferences.SOURCE_EDITO:
                menu.findItem(R.id.action_edit).setEnabled(true);
                break;
            case Preferences.SOURCE_LAST_PLAYED:
            case Preferences.SOURCE_FAVS:
            default:
                menu.findItem(R.id.action_edit).setEnabled(false);
                break;
        }
        
        switch (Preferences.getScheduleHourDelay()) {
            case 1:
                menu.findItem(R.id.schedule_1_hour).setEnabled(false);
                break;
            case 3:
                menu.findItem(R.id.schedule_3_hour).setEnabled(false);
                break;
            case 6:
                menu.findItem(R.id.schedule_6_hour).setEnabled(false);
                break;
            case 12:
                menu.findItem(R.id.schedule_12_hour).setEnabled(false);
                break;
            case 24:
                menu.findItem(R.id.schedule_24_hour).setEnabled(false);
                break;
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        
        boolean res = true;
        
        switch (item.getItemId()) {
            case R.id.action_login:
                connectToDeezer();
                break;
            case R.id.action_logout:
                disconnectFromDeezer();
                break;
            case R.id.action_edit:
                editSourceOptions();
                break;
            case R.id.action_schedule:
                break;
            case R.id.schedule_1_hour:
                Preferences.saveScheduleHourDelay(this, 1);
                break;
            case R.id.schedule_3_hour:
                Preferences.saveScheduleHourDelay(this, 3);
                break;
            case R.id.schedule_6_hour:
                Preferences.saveScheduleHourDelay(this, 6);
                break;
            case R.id.schedule_12_hour:
                Preferences.saveScheduleHourDelay(this, 12);
                break;
            case R.id.schedule_24_hour:
                Preferences.saveScheduleHourDelay(this, 24);
                break;
            default:
                res = super.onOptionsItemSelected(item);
                break;
        }
        
        return res;
    }
    /**
     * 
     */
    private final void connectToDeezer() {
        mConnect.authorize(this, Constants.APP_PERMISSIONS, mDeezerDialogListener);
    }
    
    /**
     * 
     */
    private final void disconnectFromDeezer() {
        // if deezerConnect is still valid, clear all auth info
        if (mConnect != null) {
            mConnect.logout(this);
        }
        
        // also clear the session store
        new SessionStore().clear(this);
        
        setUserLogged(false);
    }
    
    /**
     * Launch another activity for additional options
     */
    private void editSourceOptions() {
        switch (Preferences.getSourceType()) {
            case Preferences.SOURCE_EDITO:
                Intent editoIntent = new Intent(this, SelectEditoActivity.class);
                startActivity(editoIntent);
                break;
            case Preferences.SOURCE_CUSTOM:
                Intent customIntent = new Intent(this, SelectCustomActivity.class);
                startActivity(customIntent);
                break;
        }
    }
    
    /**
     * @param logged
     *            is the user logged
     */
    private void setUserLogged(final boolean logged) {
        
        boolean change = logged != mIsLoggedIn;
        
        mSourceFavs.setEnabled(logged);
        
        if (change) {
            mIsLoggedIn = logged;
            invalidateOptionsMenu();
            loadUserInfo();
        }
    }
    
    /**
     * Loads the user info to use in the
     */
    private void loadUserInfo() {
        if (mIsLoggedIn) {
            mUserName.setText(R.string.loading);
            mUserAvatar.setImageResource(R.drawable.user_default);
            
            DeezerRequest userInfo = DeezerRequestFactory.requestCurrentUser();
            mConnect.requestAsync(userInfo, mUserInfoRequestListener);
        } else {
            mUserName.setText(R.string.anonymous);
            mUserAvatar.setImageResource(R.drawable.user_anonymous);
            
        }
        
        
    }
    
    /**
     * Updates the displayed tip under the list of sources
     */
    private void updateTip() {
        
        int text = 0;
        
        switch (Preferences.getSourceType()) {
            case Preferences.SOURCE_EDITO:
                text = R.string.desc_edito;
                break;
            case Preferences.SOURCE_FAVS:
                text = R.string.desc_favorites;
                break;
            case Preferences.SOURCE_LAST_PLAYED:
                text = R.string.desc_last_played;
                break;
            case Preferences.SOURCE_CUSTOM:
                text = R.string.desc_custom;
                break;
        }
        
        if (text == 0) {
            mTipText.setText("");
        } else {
            mTipText.setText(text);
        }
    }
    
    private void showToast(final int resource) {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                Toast.makeText(SettingsActivity.this, resource, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // User Info Request Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private JsonRequestListener mUserInfoRequestListener = new JsonRequestListener() {
        
        @Override
        public void onResult(final Object result, final Object requestId) {
            final User user = (User) result;
            
            runOnUiThread(new Runnable() {
                
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    Preferences.saveCurrentUser(SettingsActivity.this, user);
                    
                    // Update the view 
                    mUserName.setText(user.getName());
                    
                    // Add icons if birthday !
                    Date birthday = user.getBirthday();
                    Date today = new Date();
                    if ((birthday.getMonth() == today.getMonth())
                            && (birthday.getDate() == today.getDate())) {
                        mUserName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_birthday,
                                0, R.drawable.ic_birthday, 0);
                    } else {
                        mUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    }
                    
                    // get the user icon
                    new FetchUserThumbnailTask(
                            getApplicationContext(), mUserThumbnailTaskListener).execute(user);
                }
            });
            
        }
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            showToast(R.string.error_user_info);
//            Log.e("Settings", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            showToast(R.string.error_user_info);
//            Log.e("Settings", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            showToast(R.string.error_user_info);
//            Log.e("Settings", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            showToast(R.string.error_user_info);
//            Log.e("Settings", "onDeezerError", e);
        }
        
        
        @Override
        public void onJSONParseException(final JSONException e, final Object arg1) {
            showToast(R.string.error_user_info);
//            Log.e("Settings", "onJSONParseException", e);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // User Thumbnail Download Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private UserThumbnailTaskListener mUserThumbnailTaskListener = new UserThumbnailTaskListener() {
        
        @Override
        public void thumbnailLoaded(final User user, final Bitmap bitmap) {
            mUserAvatar.setImageBitmap(bitmap);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // OAuth Dialog Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private DialogListener mDeezerDialogListener = new DialogListener() {
        
        @Override
        public void onComplete(final Bundle arg0) {
            // store the current authentication info 
            SessionStore sessionStore = new SessionStore();
            sessionStore.save(mConnect, SettingsActivity.this);
            
            // Update the info 
            setUserLogged(true);
        }
        
        @Override
        public void onDeezerError(final DeezerError deezerError) {
            Toast.makeText(SettingsActivity.this,
                    R.string.deezer_error_during_login,
                    Toast.LENGTH_LONG).show();
        }
        
        @Override
        public void onError(final DialogError dialogError) {
            Toast.makeText(SettingsActivity.this,
                    R.string.deezer_error_during_login,
                    Toast.LENGTH_LONG).show();
        }
        
        @Override
        public void onCancel() {
        }
        
        @Override
        public void onOAuthException(final OAuthException oAuthException) {
            Toast.makeText(SettingsActivity.this, R.string.invalid_credentials,
                    Toast.LENGTH_LONG)
                    .show();
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Radio Buttons Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    
    private OnCheckedChangeListener mRadioButtonChangedListener = new OnCheckedChangeListener() {
        
        @Override
        public void onCheckedChanged(final RadioGroup group, final int checkedId) {
            Log.i("Radio", "Checked " + checkedId);
            int source;
            switch (checkedId) {
            
                case R.id.radio_fav_albums:
                    source = Preferences.SOURCE_FAVS;
                    break;
                case R.id.radio_custom_albums:
                    source = Preferences.SOURCE_CUSTOM;
                    break;
                case R.id.radio_playing_albums:
                    source = Preferences.SOURCE_LAST_PLAYED;
                    break;
                case R.id.radio_edito_albums:
                default:
                    source = Preferences.SOURCE_EDITO;
                    break;
            }
            
            Preferences.saveSourceType(SettingsActivity.this, source);
            
            invalidateOptionsMenu();
            
            updateTip();
        }
    };
}
