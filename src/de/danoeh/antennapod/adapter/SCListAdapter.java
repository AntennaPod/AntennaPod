package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.FeedlistAdapter.Holder;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SCListAdapter extends ArrayAdapter<SimpleChapter> {

	private static final String TAG = "SCListAdapter";

	public SCListAdapter(Context context, int textViewResourceId,
			List<SimpleChapter> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;

		SimpleChapter sc = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.simplechapter_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.start = (TextView) convertView.findViewById(R.id.txtvStart);
			holder.link = (TextView) convertView.findViewById(R.id.txtvLink);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();

		}

		holder.title.setText(sc.getTitle());
		holder.start.setText(Converter.getDurationStringLong((int) sc
				.getStart()));
		if (sc.getLink() != null) {
			holder.link.setVisibility(View.VISIBLE);
			holder.link.setText(sc.getLink());
			Linkify.addLinks(holder.link, Linkify.WEB_URLS);
		} else {
			holder.link.setVisibility(View.GONE);
		}
		holder.link.setMovementMethod(null);
		holder.link.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				TextView widget = (TextView) v;
				Object text = widget.getText();
				if (text instanceof Spanned) {
					Spannable buffer = (Spannable) text;

					int action = event.getAction();

					if (action == MotionEvent.ACTION_UP
							|| action == MotionEvent.ACTION_DOWN) {
						int x = (int) event.getX();
						int y = (int) event.getY();

						x -= widget.getTotalPaddingLeft();
						y -= widget.getTotalPaddingTop();

						x += widget.getScrollX();
						y += widget.getScrollY();

						Layout layout = widget.getLayout();
						int line = layout.getLineForVertical(y);
						int off = layout.getOffsetForHorizontal(line, x);

						ClickableSpan[] link = buffer.getSpans(off, off,
								ClickableSpan.class);

						if (link.length != 0) {
							if (action == MotionEvent.ACTION_UP) {
								link[0].onClick(widget);
							} else if (action == MotionEvent.ACTION_DOWN) {
								Selection.setSelection(buffer,
										buffer.getSpanStart(link[0]),
										buffer.getSpanEnd(link[0]));
							}
							return true;
						}
					}

				}

				return false;

			}
		});
		SimpleChapter current = sc.getItem().getCurrentChapter();
		if (current != null) {
			if (current == sc) {
				holder.title.setTextColor(convertView.getResources().getColor(
						R.color.bright_blue));
			} else {
				holder.title.setTextColor(Color.parseColor("black"));
			}
		} else {
			Log.w(TAG, "Could not find out what the current chapter is.");
		}

		return convertView;
	}

	static class Holder {
		TextView title;
		TextView start;
		TextView link;
	}

	private LinkMovementMethod linkMovementMethod = new LinkMovementMethod() {

		@Override
		public boolean onTouchEvent(TextView widget, Spannable buffer,
				MotionEvent event) {
			Object text = widget.getText();
			if (text instanceof Spanned) {
				int action = event.getAction();

				if (action == MotionEvent.ACTION_UP
						|| action == MotionEvent.ACTION_DOWN) {
					int x = (int) event.getX();
					int y = (int) event.getY();

					x -= widget.getTotalPaddingLeft();
					y -= widget.getTotalPaddingTop();

					x += widget.getScrollX();
					y += widget.getScrollY();

					Layout layout = widget.getLayout();
					int line = layout.getLineForVertical(y);
					int off = layout.getOffsetForHorizontal(line, x);

					ClickableSpan[] link = buffer.getSpans(off, off,
							ClickableSpan.class);

					if (link.length != 0) {
						if (action == MotionEvent.ACTION_UP) {
							link[0].onClick(widget);
						} else if (action == MotionEvent.ACTION_DOWN) {
							Selection.setSelection(buffer,
									buffer.getSpanStart(link[0]),
									buffer.getSpanEnd(link[0]));
						}
						return true;
					}
				}

			}

			return false;

		}

	};
}
