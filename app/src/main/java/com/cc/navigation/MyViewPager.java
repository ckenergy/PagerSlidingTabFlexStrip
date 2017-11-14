package com.cc.navigation;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

/**
 * Created by chengkai on 2017/11/10 0010.
 */

public class MyViewPager extends ViewPager {

    private static final String TAG = MyViewPager.class.getSimpleName();

    public MyViewPager(Context context) {
        this(context, null);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            FixedSpeedScroller scroller = new FixedSpeedScroller(getContext(),
                    new AccelerateInterpolator());
            field.set(this, scroller);
            scroller.setmDuration(500);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    public class FixedSpeedScroller extends Scroller {
        private int mDuration = 1500;

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        public void setmDuration(int time) {
            mDuration = time;
        }

        public int getmDuration() {
            return mDuration;
        }
    }

}
