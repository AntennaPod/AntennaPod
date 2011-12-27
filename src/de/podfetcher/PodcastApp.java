package de.podfetcher;

import de.podfetcher.feed.FeedManager;
import de.podfetcher.FeedlistActivity;
import android.app.Application;
import greendroid.app.GDApplication;

public class PodcastApp extends GDApplication {

	private static PodcastApp singleton;
	
	public static PodcastApp getInstance() {
		return singleton;
	}

	public Class<?> getHomeActivityClass() {
		return PodfetcherActivity.class;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		
		//FeedManager manager = FeedManager.getInstance();
		//manager.loadDBData(getApplicationContext());
	}
	
	

}
