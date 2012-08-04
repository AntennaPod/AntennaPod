package de.danoeh.antennapod.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MiroGuideChannelViewActivity;
import de.danoeh.antennapod.adapter.MiroGuideChannelListAdapter;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.miroguide.con.MiroGuideException;
import de.danoeh.antennapod.miroguide.con.MiroGuideService;
import de.danoeh.antennapod.miroguide.model.MiroChannel;

/**
 * Displays a list of MiroChannel objects that were results of a certain
 * MiroGuideService query. If the user reaches the bottom of the list, more
 * entries will be loaded until all entries have been loaded or the maximum
 * number of channels has been reached.
 * */
public class MiroGuideChannellistFragment extends SherlockListFragment {
	private static final String TAG = "MiroGuideChannellistFragment";

	private static final String ARG_FILTER = "filter";
	private static final String ARG_FILTER_VALUE = "filter_value";
	private static final String ARG_SORT = "sort";

	private static final int MAX_CHANNELS = 200;
	private static final int CHANNELS_PER_QUERY = MiroGuideService.DEFAULT_CHANNEL_LIMIT;

	private ArrayList<MiroChannel> channels;
	private MiroGuideChannelListAdapter listAdapter;
	private int offset;

	private boolean isLoadingChannels;
	/**
	 * True if there are no more entries to load or if the maximum number of
	 * channels in the channellist has been reached
	 */
	private boolean stopLoading;

	private View footer;

	private String filter;
	private String filterValue;
	private String sort;

	private AsyncTask<Void, Void, List<MiroChannel>> channelLoader;

	/**
	 * Creates a new instance of Channellist fragment.
	 * 
	 * @throws IllegalArgumentException
	 *             if filter, filterValue or sort is null
	 * */
	public static MiroGuideChannellistFragment newInstance(String filter,
			String filterValue, String sort) {
		if (filter == null) {
			throw new IllegalArgumentException("filter cannot be null");
		}
		if (filterValue == null) {
			throw new IllegalArgumentException("filter value cannot be null");
		}
		if (sort == null) {
			throw new IllegalArgumentException("sort cannot be null");
		}
		MiroGuideChannellistFragment cf = new MiroGuideChannellistFragment();
		Bundle args = new Bundle();
		args.putString(ARG_FILTER, filter);
		args.putString(ARG_FILTER_VALUE, filterValue);
		args.putString(ARG_SORT, sort);
		cf.setArguments(args);
		return cf;
	}

	private MiroGuideChannellistFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		offset = 0;
		channels = new ArrayList<MiroChannel>();

		Bundle args = getArguments();
		filter = args.getString(ARG_FILTER);
		filterValue = args.getString(ARG_FILTER_VALUE);
		sort = args.getString(ARG_SORT);

		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		footer = inflater.inflate(R.layout.loading_footer, null);
		listAdapter = new MiroGuideChannelListAdapter(getActivity(), 0,
				channels);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (channels.isEmpty()) {
			setListShown(false);
			loadChannels();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (channelLoader != null) {
			channelLoader.cancel(true);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().addFooterView(footer); // footer has to be added before
												// the adapter has been set
		getListView().setAdapter(listAdapter);
		getListView().removeFooterView(footer);

		getListView().setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				int lastVisibleItem = firstVisibleItem + visibleItemCount;
				if (lastVisibleItem == totalItemCount) {
					if (AppConfig.DEBUG)
						loadChannels();
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
		});
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (listAdapter != null) {
			MiroChannel selection = listAdapter.getItem(position);
			Intent launchIntent = new Intent(getActivity(),
					MiroGuideChannelViewActivity.class);
			launchIntent.putExtra(MiroGuideChannelViewActivity.EXTRA_CHANNEL_ID,
					selection.getId());
			launchIntent.putExtra(MiroGuideChannelViewActivity.EXTRA_CHANNEL_URL,
					selection.getDownloadUrl());
			startActivity(launchIntent);
		}
	}

	@SuppressLint("NewApi")
	private void loadChannels() {
		if (!isLoadingChannels) {
			if (!stopLoading) {
				isLoadingChannels = true;
				channelLoader = new AsyncTask<Void, Void, List<MiroChannel>>() {
					private MiroGuideException exception;

					@Override
					protected void onCancelled() {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Channel loader was cancelled");
					}

					@Override
					protected void onPostExecute(List<MiroChannel> result) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Channel loading finished");
						if (exception == null) {
							getListView().removeFooterView(footer);
							for (MiroChannel channel : result) {
								channels.add(channel);
							}
							listAdapter.notifyDataSetChanged();
							offset += CHANNELS_PER_QUERY;
							// check if fragment should not send any more
							// queries
							if (result.size() < CHANNELS_PER_QUERY) {
								if (AppConfig.DEBUG)
									Log.d(TAG,
											"Query result was less than requested number of channels. Stopping to send any more queries");
								stopLoading = true;
							}
							if (offset >= MAX_CHANNELS) {
								if (AppConfig.DEBUG)
									Log.d(TAG,
											"Maximum number of feeds has been reached. Stopping to send any more queries");
								stopLoading = true;
							}

							setListShown(true);
						} else {
							AlertDialog.Builder dialog = new AlertDialog.Builder(
									getActivity());
							dialog.setTitle(R.string.error_label);
							dialog.setMessage(exception.getMessage());
							dialog.setNeutralButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
							dialog.create().show();
						}
						isLoadingChannels = false;
					}

					@Override
					protected void onPreExecute() {
						getListView().addFooterView(footer);
					}

					@Override
					protected List<MiroChannel> doInBackground(Void... params) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Background channel loader started");
						MiroGuideService service = new MiroGuideService();
						try {
							return service.getChannelList(filter, filterValue,
									sort, CHANNELS_PER_QUERY, offset);
						} catch (MiroGuideException e) {
							exception = e;
							e.printStackTrace();
						} finally {
							// service.close();
						}
						return null;
					}
				};

				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					channelLoader
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					channelLoader.execute();
				}
			}
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Channels are already being loaded");
		}
	}
}
