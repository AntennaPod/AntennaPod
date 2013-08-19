package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
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
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.util.ChapterUtils;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.playback.Playable;

public class ChapterListAdapter extends ArrayAdapter<Chapter> {

	private static final String TAG = "ChapterListAdapter";

	private List<Chapter> chapters;
	private Playable media;
	
	private int defaultTextColor;

	public ChapterListAdapter(Context context, int textViewResourceId,
			List<Chapter> objects, Playable media) {
		super(context, textViewResourceId, objects);
		this.chapters = objects;
		this.media = media;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;

		Chapter sc = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.simplechapter_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			defaultTextColor = holder.title.getTextColors().getDefaultColor();
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
				// from
				// http://stackoverflow.com/questions/7236840/android-textview-linkify-intercepts-with-parent-view-gestures
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
		Chapter current = ChapterUtils.getCurrentChapter(media);
		if (current != null) {
			if (current == sc) {
				holder.title.setTextColor(convertView.getResources().getColor(
						R.color.bright_blue));
			} else {
				holder.title.setTextColor(defaultTextColor);
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

	@Override
	public int getCount() {
		// ignore invalid chapters
		int counter = 0;
		for (Chapter chapter : chapters) {
			if (!ignoreChapter(chapter)) {
				counter++;
			}
		}
		return counter;
	}

	private boolean ignoreChapter(Chapter c) {
		return media.getDuration() > 0 && media.getDuration() < c.getStart();
	}

	@Override
	public Chapter getItem(int position) {
		int i = 0;
		for (Chapter chapter : chapters) {
			if (!ignoreChapter(chapter)) {
				if (i == position) {
					return chapter;
				} else {
					i++;
				}
			}
		}
		return super.getItem(position);
	}

}
