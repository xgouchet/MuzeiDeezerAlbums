package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.OAuthException;
import com.deezer.sdk.network.request.event.RequestListener;

import fr.xgouchet.deezer.muzei.util.Constants;
import fr.xgouchet.deezer.muzei.util.Preferences;


public class SelectEditoActivity extends ListActivity {
    
    private DeezerConnect mConnect;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        getListView().setOnItemClickListener(mItemSelectedListener);
        
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    @Override
    protected void onResume() {
        super.onResume();
        
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
        
        setResult(RESULT_CANCELED);
        
        listEditos();
    }
    
    
    private void listEditos() {
        
        setProgressBarVisibility(true);
        setProgressBarIndeterminate(true);
        
        
        
        DeezerRequest editoRequest = new DeezerRequest("editorial");
        mConnect.requestAsync(editoRequest, mEditoRequestListener);
    }
    
    private void hideProgress() {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                setProgressBarVisibility(false);
            }
        });
    }
    
    private void updateSelection() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // parse the preference list
        String editosList = prefs.getString(Preferences.PREF_EDITO_ID_LIST, "0");
        
        String[] editos = editosList.split(";");
        List<Long> editosIds = new ArrayList<Long>();
        
        for (String edito : editos) {
            try {
                long id = Long.valueOf(edito);
                editosIds.add(id);
            }
            catch (NumberFormatException e) {
                
            }
        }
        
        // check all the rows in the list
        int count = getListAdapter().getCount();
        for (int i = 0; i < count; ++i) {
            Edito edito = (Edito) getListAdapter().getItem(i);
            getListView().setItemChecked(i, (editosIds.contains(edito.id)));
        }
    }
    
    private void saveSelection() {
        Log.i("Select", "Saving Selection :)");
        
        StringBuilder builder = new StringBuilder();
        
        // check all the rows in the list
        int count = getListAdapter().getCount();
        for (int i = 0; i < count; ++i) {
            if (getListView().isItemChecked(i)) {
                Edito edito = (Edito) getListAdapter().getItem(i);
                builder.append(edito.id);
                builder.append(";");
            }
        }
        
        // save preference
        Log.i("Select", builder.toString());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor edit = prefs.edit();
        edit.putString(Preferences.PREF_EDITO_ID_LIST, builder.toString());
        edit.apply();
        
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // List View Item Selection listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnItemClickListener mItemSelectedListener = new OnItemClickListener() {
        
        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
            saveSelection();
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Editorial Request Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private RequestListener mEditoRequestListener = new RequestListener() {
        
        @Override
        public void onComplete(final String response, final Object requestId) {
            
            hideProgress();
            
            try {
                final List<Edito> editos = parseResponse(response);
                
                runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        setListAdapter(new ArrayAdapter<Edito>(SelectEditoActivity.this,
                                android.R.layout.simple_list_item_multiple_choice, editos));
                        
                        updateSelection();
                    }
                });
                
            }
            catch (Exception e) {
                Log.e("Select", "onComplete", e);
            }
            
        }
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            hideProgress();
            Log.e("Select", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            hideProgress();
            Log.e("Select", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            hideProgress();
            Log.e("Select", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            hideProgress();
            Log.e("Select", "onDeezerError", e);
        }
        
        private List<Edito> parseResponse(final String response)
                throws JSONException {
            List<Edito> editos = new LinkedList<Edito>();
            
            JSONObject jsonResponse = new JSONObject(response);
            
            // get edito array
            JSONArray jsonData = jsonResponse.optJSONArray("data");
            if (jsonData == null) {
                return editos;
            }
            
            int count = jsonData.length();
            for (int i = 0; i < count; ++i) {
                JSONObject jsonEdito = jsonData.getJSONObject(i);
                
                if ("editorial".equals(jsonEdito.optString("type"))) {
                    Edito edito = new Edito();
                    edito.id = jsonEdito.optLong("id");
                    edito.name = jsonEdito.optString("name");
                    editos.add(edito);
                }
                
            }
            
            return editos;
        }
        
        
    };
    
}
