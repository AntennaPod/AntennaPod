package de.podfetcher;

import de.podfetcher.activity.PodfetcherActivity;
import de.podfetcher.feed.FeedManager;
import android.app.Application;

public class PodcastApp extends Application {
    private static final String TAG = "PodcastApp";
    public static final String PREF_NAME = "PodfetcherPrefs";
    
    
	private static PodcastApp singleton;
	
	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		
		FeedManager manager = FeedManager.getInstance();
		manager.loadDBData(getApplicationContext());
	}
	
	
	

}
