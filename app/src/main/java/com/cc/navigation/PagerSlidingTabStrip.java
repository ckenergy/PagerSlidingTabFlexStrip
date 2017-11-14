/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cc.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;


public class PagerSlidingTabStrip extends HorizontalScrollView {

    private static final String TAG = PagerSlidingTabStrip.class.getSimpleName();

    public interface IconTabProvider {
        public int getPageIconResId(int position);
    }

    // @formatter:off
    private static final int[] ATTRS = new int[]{android.R.attr.textSize,
            android.R.attr.textColor};
    // @formatter:on

    private LinearLayout.LayoutParams defaultTabLayoutParams;
    private LinearLayout.LayoutParams expandedTabLayoutParams;

    private final PageListener pageListener = new PageListener();
    public OnPageChangeListener delegatePageListener;

    private LinearLayout tabsContainer;
    private ViewPager pager;

    private int tabCount;

    private int currentPosition = 0;
    private float currentPositionOffset = 0f;

    private Paint rectPaint;
    private Paint dividerPaint;

    private int indicatorColor = 0xFFff5050;
    private int underlineColor = 0xFFff5050;
    private int dividerColor = 0xFFff5050;

    private boolean shouldExpand = false;
    private boolean textAllCaps = true;

    private int scrollOffset = 52;
    private int indicatorHeight = 8;
    private int underlineHeight = 2;
    private int dividerPadding = 12;
    private int tabPadding = 20;
    private int dividerWidth = 1;

    private float dividerFlexLength = 0;
    private boolean dividerNeedFlex = false;

    private int tabTextSize = 15;
    private int tabTextColor = 0xFF5b5b5b;
    private int tabTextSelectedColor = 0xFFff5050;
    private int tabTextNormalColor = tabTextColor;

    private boolean dividerExpand;
    private int dividerPaddingBottom;
    private int indicatorWidth;

    private float corner = 1;
    RectF rectF = new RectF();

    private Typeface tabTypeface = null;
    private int tabTypefaceStyle = Typeface.BOLD;

    private int lastScrollX = 0;

    private int tabBackgroundResId = R.color.transparent;

    private Locale locale;

    public PagerSlidingTabStrip(Context context) {
        this(context, null);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);

        setFillViewport(true);
        setWillNotDraw(false);

        tabsContainer = new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        tabsContainer.setGravity(Gravity.CENTER);
        addView(tabsContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        scrollOffset = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
        indicatorHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
        underlineHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
        dividerPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
        tabPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        tabTextSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);
        corner = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, corner, dm);

        // get system attrs (android:textSize and android:textColor)

        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);

        tabTextSize = a.getDimensionPixelSize(0, tabTextSize);
//        tabTextColor = a.getColor(1, tabTextColor);

        a.recycle();

        // get custom attrs

        a = context.obtainStyledAttributes(attrs,
                R.styleable.PagerSlidingTabStrip);

        indicatorColor = a.getColor(
                R.styleable.PagerSlidingTabStrip_pstsIndicatorColor,
                indicatorColor);
        underlineColor = a.getColor(
                R.styleable.PagerSlidingTabStrip_pstsUnderlineColor,
                underlineColor);
        dividerColor = a
                .getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor,
                        dividerColor);
        indicatorHeight = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight,
                indicatorHeight);
        underlineHeight = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight,
                underlineHeight);
        dividerPadding = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsDividerPadding,
                dividerPadding);
        tabPadding = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight,
                tabPadding);
        tabBackgroundResId = a.getResourceId(
                R.styleable.PagerSlidingTabStrip_pstsTabBackground,
                tabBackgroundResId);
        shouldExpand = a
                .getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand,
                        shouldExpand);
        scrollOffset = a
                .getDimensionPixelSize(
                        R.styleable.PagerSlidingTabStrip_pstsScrollOffset,
                        scrollOffset);
        textAllCaps = a.getBoolean(
                R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps);
        dividerExpand = a.getBoolean(
                R.styleable.PagerSlidingTabStrip_pstsDividerExpand, false);
        dividerPaddingBottom = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsDividerPaddingBottom, 0);
        indicatorWidth = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsDividerWidth, 0);
        dividerFlexLength = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsDividerFlexLength, 0);
        dividerNeedFlex = a.getBoolean(
                R.styleable.PagerSlidingTabStrip_pstsDividerNeedFlex, false);

        a.recycle();

        rectPaint = new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Style.FILL);

        dividerPaint = new Paint();
        dividerPaint.setAntiAlias(true);
        dividerPaint.setStrokeWidth(dividerWidth);

        defaultTabLayoutParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        expandedTabLayoutParams = new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1.0f);

        if (locale == null) {
            locale = getResources().getConfiguration().locale;
        }
    }

    public void setViewPager(ViewPager pager) {
        this.pager = pager;

        if (pager.getAdapter() == null) {
            throw new IllegalStateException(
                    "ViewPager does not have adapter instance.");
        }

        pager.addOnPageChangeListener(pageListener);

        notifyDataSetChanged();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.delegatePageListener = listener;
    }

    public void notifyDataSetChanged() {

        tabsContainer.removeAllViews();

        tabCount = pager.getAdapter().getCount();

        for (int i = 0; i < tabCount; i++) {

            if (pager.getAdapter() instanceof IconTabProvider) {
                addIconTab(i,
                        ((IconTabProvider) pager.getAdapter())
                                .getPageIconResId(i));
            } else {
                addTextTab(i, pager.getAdapter().getPageTitle(i).toString());
            }

        }

        updateTabStyles();

        getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {

                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            getViewTreeObserver().removeGlobalOnLayoutListener(
                                    this);
                        } else {
                            getViewTreeObserver().removeOnGlobalLayoutListener(
                                    this);
                        }

                        currentPosition = pager.getCurrentItem();
                        scrollToChild(currentPosition, 0);

                        // 通知当前选中的是那个页面
                        if (delegatePageListener != null) {
                            delegatePageListener
                                    .onPageSelected(currentPosition);
                        }
                    }
                });

    }

    private void addTextTab(final int position, String title) {

        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        addTab(position, tab);
    }

    private void addIconTab(final int position, int resId) {

        ImageButton tab = new ImageButton(getContext());
        tab.setImageResource(resId);

        addTab(position, tab);

    }

    private void addTab(final int position, View tab) {
        tab.setFocusable(true);
        tab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(position);
            }
        });

        tab.setPadding(tabPadding, 0, tabPadding, 0);
        tabsContainer
                .addView(tab, position, shouldExpand ? expandedTabLayoutParams
                        : defaultTabLayoutParams);
    }

    private void updateTabStyles() {

        for (int i = 0; i < tabCount; i++) {

            View v = tabsContainer.getChildAt(i);

            v.setBackgroundResource(tabBackgroundResId);

            if (v instanceof TextView) {

                TextView tab = (TextView) v;
                tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
                tab.setTypeface(tabTypeface);
                tab.setTextColor(tabTextColor);// 设置的是单一色

                // ############ 源码修改 ##############
                // ###### 设置选中色 ############
                if (i == pager.getCurrentItem()) {
                    // 给选中的颜色
                    tab.setTextColor(tabTextSelectedColor);
                    tab.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    // 给未选中的颜色
                    tab.setTextColor(tabTextNormalColor);
                    tab.setTypeface(Typeface.DEFAULT);
                }
                // ###########################

                // setAllCaps() is only available from API 14, so the upper case
                // is made manually if we are on a
                // pre-ICS-build
                if (textAllCaps) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        tab.setAllCaps(true);
                    } else {
                        tab.setText(tab.getText().toString()
                                .toUpperCase(locale));
                    }
                }
            }
        }

    }

    private void scrollToChild(int position, int offset) {

        if (tabCount == 0) {
            return;
        }

        int newScrollX = tabsContainer.getChildAt(position).getLeft() + offset;

        if (position > 0 || offset > 0) {
            newScrollX -= scrollOffset;
        }

        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX;
            scrollTo(newScrollX, 0);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (isInEditMode() || tabCount == 0) {
                return;
            }

            final int height = getHeight();

            // draw indicator line

            rectPaint.setColor(indicatorColor);

            // default: line below current tab
            View currentTab = tabsContainer.getChildAt(currentPosition);
            float lineLeft = currentTab.getLeft();
            float lineRight = currentTab.getRight();
            TextView currentView = (TextView) tabsContainer.getChildAt(currentPosition);
            float width = currentView.getLayout().getLineWidth(0);

            float currentLeft = lineLeft;
            float currentRight = lineRight;

            // if there is an offset, start interpolating left and right coordinates
            // between current and next tab
            if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

                Log.d(TAG, "FlexLength currentPositionOffset:"+currentPositionOffset);

                View nextTab = tabsContainer.getChildAt(currentPosition + 1);
                TextView textView = (TextView) tabsContainer.getChildAt(currentPosition + 1);
                width = textView.getLayout().getLineWidth(0);
                final float nextTabLeft = nextTab.getLeft();
                final float nextTabRight = nextTab.getRight();

                if (dividerNeedFlex) {
                    float offset = currentRight - currentLeft;
                    if (dividerFlexLength <= 0) {
                        dividerFlexLength = offset;
                    }

                    float scale = dividerFlexLength/2/offset;
                    Log.d(TAG, "FlexLength scale:"+scale);
                    float a1;
                    float a2;
                    float b1;
                    float b2;

                    /**
                     * 0< x < scale 时 （x = currentPositionOffset）;
                     * left = 0.4 * x ;
                     * right = 1.6 * x;
                     */
                    if (currentPositionOffset < scale) {
                        a1 = 0.4f;
                        b1 = 0;
                        a2 = 1.6f;
                        b2 = 0;
                    }else if (currentPositionOffset >= 1 - scale) {
                        /**
                         * 1 - scale <= x <=1 时
                         * left = 1.6 * x - 0.6;
                         * right = 1.6 * x + 0.6;
                         */
                        a1 = 1.6f;
                        b1 = -0.6f;
                        a2 = 0.4f;
                        b2 = 0.6f;
                    }else {
                        /**
                         * scale < x < 1 - scale 时
                         * left = x - 0.6 * scale;
                         * right = x + 0.6 * scale;
                         */
                        a1 = 1f;
                        b1 = -0.6f * scale;
                        a2 = 1f;
                        b2 = 0.6f * scale;
                    }

                    lineLeft = (lineLeft + (a1 * currentPositionOffset + b1) * offset);
                    lineRight = (lineRight + (a2 * currentPositionOffset + b2) * offset);

                }else {
                    lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset)
                            * lineLeft);
                    lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset)
                            * lineRight);
                }


            }
            if (dividerExpand) {
                rectF.set(lineLeft + tabPadding, height - indicatorHeight-dividerPaddingBottom,
                        lineRight - tabPadding, height-dividerPaddingBottom);
                canvas.drawRoundRect(rectF, corner, corner, rectPaint);
            }else {
                if (indicatorWidth > 0) {
                    width = indicatorWidth;
                }

                Log.d(TAG, "currentPositionOffset:"+currentPositionOffset);

//                float flexlength = dividerFlexLength * currentPositionOffset;

//                Log.d(TAG, "flexlength:"+flexlength);

                float offset = currentRight - currentLeft - width;
                float left = lineLeft + offset/2;
                float right = lineRight - offset/2;

                rectF.set(left, height - indicatorHeight - dividerPaddingBottom,
                        right, height - dividerPaddingBottom);
                canvas.drawRoundRect(rectF, corner, corner, rectPaint);

            }
        }catch (Exception e){
            e.printStackTrace();
        }


//        canvas.drawRect(lineLeft, height - indicatorHeight, lineRight, height,
//                rectPaint);

        // draw underline

        // rectPaint.setColor(underlineColor);
        // canvas.drawRect(0, height - underlineHeight,
        // tabsContainer.getWidth(), height, rectPaint);

        // draw divider

        // dividerPaint.setColor(dividerColor);
        // for (int i = 0; i < tabCount - 1; i++) {
        // View tab = tabsContainer.getChildAt(i);
        // canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(),
        // height - dividerPadding, dividerPaint);
        // }
    }

    private class PageListener implements OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {

            Log.d(TAG, "position:"+position+",positionOffset:"+positionOffset+",positionOffsetPixels:"+positionOffsetPixels);

            currentPosition = position;
            currentPositionOffset = positionOffset;

            scrollToChild(position, (int) (positionOffset * tabsContainer
                    .getChildAt(position).getWidth()));

            invalidate();

            if (delegatePageListener != null) {
                delegatePageListener.onPageScrolled(position, positionOffset,
                        positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                scrollToChild(pager.getCurrentItem(), 0);
            }

            if (delegatePageListener != null) {
                delegatePageListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (delegatePageListener != null) {
                delegatePageListener.onPageSelected(position);
            }

            // ######################
            updateTabStyles();
            // ######################
        }

    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        invalidate();
    }

    public void setIndicatorColorResource(int resId) {
        this.indicatorColor = getResources().getColor(resId);
        invalidate();
    }

    public int getIndicatorColor() {
        return this.indicatorColor;
    }

    public void setIndicatorHeight(int indicatorLineHeightPx) {
        this.indicatorHeight = indicatorLineHeightPx;
        invalidate();
    }

    public int getIndicatorHeight() {
        return indicatorHeight;
    }

    public void setUnderlineColor(int underlineColor) {
        this.underlineColor = underlineColor;
        invalidate();
    }

    public void setUnderlineColorResource(int resId) {
        this.underlineColor = getResources().getColor(resId);
        invalidate();
    }

    public int getUnderlineColor() {
        return underlineColor;
    }

    public void setDividerColor(int dividerColor) {
        this.dividerColor = dividerColor;
        invalidate();
    }

    public void setDividerColorResource(int resId) {
        this.dividerColor = getResources().getColor(resId);
        invalidate();
    }

    public int getDividerColor() {
        return dividerColor;
    }

    public void setUnderlineHeight(int underlineHeightPx) {
        this.underlineHeight = underlineHeightPx;
        invalidate();
    }

    public int getUnderlineHeight() {
        return underlineHeight;
    }

    public void setDividerPadding(int dividerPaddingPx) {
        this.dividerPadding = dividerPaddingPx;
        invalidate();
    }

    public int getDividerPadding() {
        return dividerPadding;
    }

    public void setScrollOffset(int scrollOffsetPx) {
        this.scrollOffset = scrollOffsetPx;
        invalidate();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setShouldExpand(boolean shouldExpand) {
        this.shouldExpand = shouldExpand;
        requestLayout();
    }

    public boolean getShouldExpand() {
        return shouldExpand;
    }

    public boolean isTextAllCaps() {
        return textAllCaps;
    }

    public void setAllCaps(boolean textAllCaps) {
        this.textAllCaps = textAllCaps;
    }

    public void setTextSize(int textSizePx) {
        this.tabTextSize = textSizePx;
        updateTabStyles();
    }

    public int getTextSize() {
        return tabTextSize;
    }

    public void setTextColor(int textColor) {
        this.tabTextColor = textColor;
        updateTabStyles();
    }

    public void setTextColor(int textNormalColor, int textSelectedColor) {
        this.tabTextNormalColor = textNormalColor;
        this.tabTextSelectedColor = textSelectedColor;
        updateTabStyles();
    }

    public void setTextColorResource(int resId) {
        this.tabTextColor = getResources().getColor(resId);
        updateTabStyles();
    }

    public int getTextColor() {
        return tabTextColor;
    }

    public void setTypeface(Typeface typeface) {
        this.tabTypeface = typeface;
        updateTabStyles();
    }

    public void setTabBackground(int resId) {
        this.tabBackgroundResId = resId;
    }

    public int getTabBackground() {
        return tabBackgroundResId;
    }

    public void setTabPaddingLeftRight(int paddingPx) {
        this.tabPadding = paddingPx;
        updateTabStyles();
    }

    public int getTabPaddingLeftRight() {
        return tabPadding;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentPosition = savedState.currentPosition;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPosition = currentPosition;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPosition;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPosition);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
