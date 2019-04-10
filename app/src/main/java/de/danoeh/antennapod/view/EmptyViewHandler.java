package de.danoeh.antennapod.view;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import de.danoeh.antennapod.R;

public class EmptyViewHandler extends View {
    private Activity activity;
    private int title;
    private int message;

    public EmptyViewHandler(Context context) {
        super(context);
        this.setActivity((Activity) context);
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(int title) {
        this.title = title;
    }

    public int getMessage() {
        return message;
    }

    public void setMessage(int message) {
        this.message = message;
    }

    public void attachToListView(ListView listView){

        View  emptyView = getActivity().getLayoutInflater().inflate(R.layout.empty_view_layout, null);
        ((ViewGroup) listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);

        TextView tvTitle = (TextView) emptyView.findViewById(R.id.emptyViewTitle);
        tvTitle.setText(title);

        TextView tvMessage = (TextView) emptyView.findViewById(R.id.emptyViewMessage);
        tvMessage.setText(message);

    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
