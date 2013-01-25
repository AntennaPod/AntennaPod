package de.danoeh.antennapod.adapter;

import de.danoeh.antennapod.R;
import android.content.res.TypedArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

/**
 * OnClickListener for the itemlist headers of feeditem lists. This class takes
 * care of changing the appearance of the arrow on the left side of the header
 * view. An instance of this class should be set as the OnClickListener of the
 * header view.
 */
public class OnItemlistHeaderClicked implements OnClickListener {
	private ImageView arrow;
	private View header;

	private boolean isExpanded;

	/**
	 * Constructor
	 * 
	 * @param header
	 *            Reference to the header View of the itemlist.
	 * @param isExpanded
	 *            true if the itemlist is currently expanded.
	 * */
	public OnItemlistHeaderClicked(View header, boolean isExpanded) {
		if (header == null)
			throw new IllegalArgumentException("Header view must not be null");
		this.header = header;
		arrow = (ImageView) header.findViewById(R.id.imgvHeaderArrow);
		this.isExpanded = isExpanded;
		refreshArrowState();
	}

	private void refreshArrowState() {
		TypedArray typeDrawables = header.getContext().obtainStyledAttributes(
				new int[] { R.attr.navigation_collapse,
						R.attr.navigation_expand });
		if (isExpanded) {
			arrow.setImageDrawable(typeDrawables.getDrawable(0));
		} else {
			arrow.setImageDrawable(typeDrawables.getDrawable(1));
		}
	}

	@Override
	public void onClick(View v) {
		isExpanded = !isExpanded;
		refreshArrowState();
	}

}
