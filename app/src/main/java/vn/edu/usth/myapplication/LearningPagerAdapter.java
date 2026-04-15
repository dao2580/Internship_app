package vn.edu.usth.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LearningPagerAdapter extends FragmentStateAdapter {

    public LearningPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HistoryTabFragment();
            case 1: return new MyWordsTabFragment();
            case 2: return new QuizTabFragment();
            default: return new HistoryTabFragment();
        }
    }

    @Override
    public int getItemCount() { return 3; }
}