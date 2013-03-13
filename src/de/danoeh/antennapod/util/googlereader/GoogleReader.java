package de.danoeh.antennapod.util.googlereader;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.opml.OpmlElement;

public class GoogleReader{
    private static final String TAG = "GoogleReader";
    private Account[] accounts;
    private AccountManager am;
    private String token;
    private Account selectedAccount;
    private Activity activity;
        
    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(int index) {
        this.selectedAccount = accounts[index];
    }

    public GoogleReader(Activity activity) {
        this.activity = activity;
        this.am = AccountManager.get(activity); 
        this.accounts = am.getAccountsByType("com.google");
        Log.d(TAG, "accounts size:"+accounts.length);
    }
    
    public String[] getGoogleAccountNames() {
        String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accountNames.length; i++) {
            accountNames[i] = accounts[i].name;
        }
        return accountNames;
    }

    public void acquireToken() throws Exception {
        try {
            this.token = am.blockingGetAuthToken(
                    getSelectedAccount(),                     // Account retrieved using getAccountsByType()
                    GoogleConfig.OAUTH2_SCOPE,            // Auth scope
                    true);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        if(this.token == null) {
            throw new Exception(this.activity.getString(R.string.greader_acquire_token_failed));
        }
    }
    
    public ArrayList<OpmlElement> getListenSubscriptions() throws Exception{
        ArrayList<OpmlElement> elementList = new ArrayList<OpmlElement>();  
        final AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
        
        if(this.token==null) {
            this.acquireToken();
        }
                
        String apiUrl = GoogleConfig.API_ENDPOINT;
        final HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.addHeader("client_id", GoogleConfig.CLIENT_ID);
        httpGet.addHeader("client_secret", GoogleConfig.CLIENT_SECRET);
        httpGet.addHeader("Authorization", "OAuth " + token);
        if (AppConfig.DEBUG) Log.d(TAG, "use token:"+token);  
        
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        String content = IOUtils.toString(AndroidHttpClient.getUngzippedContent(httpEntity )) ;
        
        try {
            JSONObject subscriptions = new JSONObject(content);
            JSONArray feeds = subscriptions.getJSONArray("subscriptions");
            for(int i=0; i<feeds.length();i++) {
                JSONObject feed = feeds.getJSONObject(i);
                JSONArray categories = feed.getJSONArray("categories");
                for(int j=0;j<categories.length();j++) {
                    JSONObject categroy = categories.getJSONObject(j);
                    if(categroy.getString("id").endsWith("/label/Listen Subscriptions")) {
    
                        OpmlElement element = new OpmlElement();
                        element.setHtmlUrl(feed.getString("htmlUrl"));
                        element.setText(feed.getString("title"));
                        element.setType("RSS");
                        element.setXmlUrl(feed.getString("id").replaceFirst("^feed/", ""));
                        elementList.add(element);
                        Log.d(TAG,"element htmlurl:"+element.getHtmlUrl()+" text:"+element.getText()+" type:"+element.getType()+" xmlurl:"+element.getXmlUrl());
                        break;
                    }
                }
            }
        }finally {
            httpClient.close();
        }
        return elementList;
    }
}
