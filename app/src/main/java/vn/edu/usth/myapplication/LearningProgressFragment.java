package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LearningProgressFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_learning_progress, container, false);

        TabLayout tabLayout = v.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = v.findViewById(R.id.view_pager);

        LearningPagerAdapter adapter = new LearningPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Lịch sử");
                    break;
                case 1:
                    tab.setText("My Words");
                    break;
                case 2:
                    tab.setText("Quiz");
                    break;
            }
        }).attach();

        int selectedTab = 0;
        if (getArguments() != null) {
            selectedTab = getArguments().getInt("selected_tab", 0);
        }

        if (selectedTab < 0 || selectedTab > 2) {
            selectedTab = 0;
        }

        viewPager.setCurrentItem(selectedTab, false);

        return v;
    }
}