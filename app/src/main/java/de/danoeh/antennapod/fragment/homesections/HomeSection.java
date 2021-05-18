package de.danoeh.antennapod.fragment.homesections;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection {

    Fragment context;

    View section;
    TextView tvTitle;
    TextView tvNavigate;
    RecyclerView recyclerView;
    //FragmentContainerView fragmentContainer;

    protected String sectionTitle = "";
    protected String sectionNavigateTitle = "";
    protected Fragment sectionFragment = null;

    public HomeSection(Fragment context) {
        this.context = context;
        section = View.inflate(context.requireActivity(), R.layout.home_section, null);
        tvTitle = section.findViewById(R.id.sectionTitle);
        tvNavigate = section.findViewById(R.id.sectionNavigate);
        recyclerView = section.findViewById(R.id.sectionRecyclerView);
        //fragmentContainer = section.findViewById(R.id.sectionFragmentContainer);
    }

    public void addSectionTo(LinearLayout parent) {
        if (sectionFragment != null) {
            /*context.requireActivity().getSupportFragmentManager()
                    .beginTransaction().add(R.id.sectionFragmentContainer, sectionFragment)
                    .commit();
            fragmentContainer.setVisibility(View.VISIBLE);*/
        } else {
            //TODO
            recyclerView.setVisibility(View.VISIBLE);
        }

        tvTitle.setText(sectionTitle);
        tvNavigate.setText(sectionNavigateTitle.toLowerCase()+" >>");
        tvNavigate.setOnClickListener(navigate());

        parent.addView(section);
    }

    @NonNull
    protected abstract View.OnClickListener navigate();

    @NonNull
    protected abstract List<FeedItem> loadItems();

    public int getItemSize(boolean large) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = 100;
        if (large) {
            dp = 130;
        }
        return (int) (displayMetrics.density * dp);
    }
}
