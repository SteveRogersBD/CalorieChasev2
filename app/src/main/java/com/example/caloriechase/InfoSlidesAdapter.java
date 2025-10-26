package com.example.caloriechase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class InfoSlidesAdapter extends FragmentStateAdapter {
    
    private static final int NUM_PAGES = 3;
    
    public InfoSlidesAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return InfoSlideFragment.newInstance(
                    "Run, walk more and burn calories",
                    "running"
                );
            case 1:
                return InfoSlideFragment.newInstance(
                    "Chase treasures while you run",
                    "treasure"
                );
            case 2:
                return InfoSlideFragment.newInstance(
                    "Create your own track and run on it",
                    "map"
                );
            default:
                return InfoSlideFragment.newInstance("", "");
        }
    }
    
    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}