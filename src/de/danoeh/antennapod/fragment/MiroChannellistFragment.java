package de.danoeh.antennapod.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import com.actionbarsherlock.app.SherlockListFragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.MiroChannellistAdapter;
import de.danoeh.antennapod.miroguide.con.MiroException;
import de.danoeh.antennapod.miroguide.con.MiroService;
import de.danoeh.antennapod.miroguide.model.MiroChannel;

public class MiroChannellistFragment extends SherlockListFragment {
	private static final String TAG = "MiroChannellistFragment";

	private static final String ARG_FILTER = "filter";
	private static final String ARG_FILTER_VALUE = "filter_value";
	private static final String ARG_SORT = "sort";

	private ArrayList<MiroChannel> channels;
	private MiroChannellistAdapter listAdapter;
	private int offset;

	private boolean isLoadingChannels;

	private View footer;

	private String filter;
	private String filterValue;
	private String sort;

	/**
	 * Creates a new instance of Channellist fragment.
	 * 
	 * @throws IllegalArgumentException
	 *             if filter, filterValue or sort is null
	 * */
	public static MiroChannellistFragment newInstance(String filter,
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
		MiroChannellistFragment cf = new MiroChannellistFragment();
		Bundle args = new Bundle();
		args.putString(ARG_FILTER, filter);
		args.putString(ARG_FILTER_VALUE, filterValue);
		args.putString(ARG_SORT, sort);
		cf.setArguments(args);
		return cf;
	}

	private MiroChannellistFragment() {
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
		listAdapter = new MiroChannellistAdapter(getActivity(), 0, channels);
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
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().addFooterView(footer);
		getListView().setAdapter(listAdapter);
		getListView().removeFooterView(footer);

		getListView().setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				int lastVisibleItem = firstVisibleItem + visibleItemCount;
				if (lastVisibleItem == totalItemCount) {
					loadChannels();
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
		});
	}

	@SuppressLint("NewApi")
	private void loadChannels() {
		if (!isLoadingChannels) {
			isLoadingChannels = true;
			AsyncTask<Void, Void, List<MiroChannel>> channelLoader = new AsyncTask<Void, Void, List<MiroChannel>>() {
				private MiroException exception;

				@Override
				protected void onPostExecute(List<MiroChannel> result) {
					if (exception == null) {
						getListView().removeFooterView(footer);
						for (MiroChannel channel : result) {
							channels.add(channel);
						}
						listAdapter.notifyDataSetChanged();
						offset += MiroService.DEFAULT_CHANNEL_LIMIT;
						setListShown(true);
					} else {
						AlertDialog.Builder dialog = new AlertDialog.Builder(
								getActivity());
						dialog.setTitle(R.string.error_label);
						dialog.setMessage(exception.getMessage());
						dialog.setNeutralButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										getActivity().finish();
									}
								});
						dialog.create().show();
					}
				}

				@Override
				protected void onPreExecute() {
					getListView().addFooterView(footer);
				}

				@Override
				protected List<MiroChannel> doInBackground(Void... params) {
					MiroService service = new MiroService();
					try {
						return service
								.getChannelList(filter, filterValue, sort,
										MiroService.DEFAULT_CHANNEL_LIMIT,
										offset);
					} catch (MiroException e) {
						exception = e;
						e.printStackTrace();
					}
					return null;
				}
			};

			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				channelLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				channelLoader.execute();
			}

		}
	}
}
