package com.cc.navigation;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private ArrayList<Fragment> mFragments = new ArrayList<>();
    private String[] mTitles = {"第一", "第二", "第三"};

    PagerSlidingTabStrip mPagerSlidingTabStrip;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPagerSlidingTabStrip = (PagerSlidingTabStrip) findViewById(R.id.main_tabs);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        BlankFragment blankFragment = new BlankFragment();
        BlankFragment blankFragment1 = new BlankFragment();
        BlankFragment blankFragment3 = new BlankFragment();

        mFragments.add(blankFragment);
        mFragments.add(blankFragment1);
        mFragments.add(blankFragment3);

        mViewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));

        mPagerSlidingTabStrip.setViewPager(mViewPager);

    }

    class MainPagerAdapter extends FragmentPagerAdapter {

        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }

}
