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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
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

import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Locale;


public class PagerSlidingTabStrip2 extends HorizontalScrollView {

    private static final String TAG = PagerSlidingTabStrip2.class.getSimpleName();
    private final int mTextBold;

    private static final int TEXT_BOLD_NONE = 0;
    private static final int TEXT_BOLD_WHEN_SELECT = 1;
    private static final int TEXT_BOLD_BOTH = 2;

    public interface IconTabProvider {
        public int getPageIconResId(int position);
    }

    public interface TextTabProvider {
        public String getPageTitle(int position);
    }

    // @formatter:off
    private static final int[] ATTRS = new int[]{android.R.attr.textSize,
            android.R.attr.textColor};
    // @formatter:on

    private LinearLayout.LayoutParams defaultTabLayoutParams;
    private LinearLayout.LayoutParams expandedTabLayoutParams;

    private final PageListener pageListener = new PageListener();
    public ViewPager2.OnPageChangeCallback delegatePageListener;

    private LinearLayout tabsContainer;
    private ViewPager2 pager;

    private int tabCount;

    private int currentPosition = 0;
    private float currentPositionOffset = 0f;

    private Paint rectPaint;
    private Paint dividerPaint;

    private int indicatorColor = 0xFFff5050;

    private boolean shouldExpand = false;
    private boolean textAllCaps = false;

    private int scrollOffset = 52;
    private int indicatorHeight = 8;
    private int tabPadding = 7;
    private int dividerWidth = 1;

    private float dividerFlexLength = 0;
    private boolean dividerNeedFlex = false;

    private int tabTextSize = 15;
    private int tabSelectTextSize = tabTextSize;
    private int tabTextColor = 0xFF5b5b5b;
    private int tabTextSelectedColor = 0xFFff5050;
    private int tabTextNormalColor = tabTextColor;

    private boolean dividerExpand;
    private int dividerPaddingBottom;
    private int indicatorWidth;
    private int iconGravity;
    private int iconPadding;

    /**
     * 圆角
     */
    private float corner = 2;

    RectF rectF = new RectF();

    private Typeface tabTypeface = null;
    private int tabTypefaceStyle = Typeface.BOLD;

    private int lastScrollX = 0;

    private int tabBackgroundResId = android.R.color.transparent;

    private Locale locale;

    public PagerSlidingTabStrip2(Context context) {
        this(context, null);
    }

    public PagerSlidingTabStrip2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerSlidingTabStrip2(Context context, AttributeSet attrs,
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
        tabPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        tabTextSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);
        tabSelectTextSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, tabSelectTextSize, dm);
        corner = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, corner, dm);

        // get system attrs (android:textSize and android:textColor)

        // get custom attrs

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PagerSlidingTabStrip);

        indicatorColor = a.getColor(
                R.styleable.PagerSlidingTabStrip_pstsIndicatorColor,
                indicatorColor);
        indicatorHeight = a.getDimensionPixelSize(
                R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight,
                indicatorHeight);
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
        tabTextNormalColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsNormalText, tabTextNormalColor);
        tabTextSelectedColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsSelectText, tabTextSelectedColor);
        tabTextSize = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTextSize, tabTextSize);
        tabSelectTextSize = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsSelectTextSize, tabSelectTextSize);
        corner = a.getDimension(R.styleable.PagerSlidingTabStrip_pstsDividerRadius, corner);

        mTextBold = a.getInt(R.styleable.PagerSlidingTabStrip_pstsTextBold, TEXT_BOLD_NONE);

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

    public void setViewPager(ViewPager2 pager) {
        this.pager = pager;

        if (pager.getAdapter() == null) {
            throw new IllegalStateException(
                    "ViewPager does not have adapter instance.");
        }

        pager.registerOnPageChangeCallback(pageListener);

        notifyDataSetChanged();
    }

    public void setGravity(int gravity) {
        tabsContainer.setGravity(gravity);
    }

    public void setOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
        this.delegatePageListener = listener;
    }

    public void notifyDataSetChanged() {

        tabsContainer.removeAllViews();

        tabCount = pager.getAdapter().getItemCount();

        for (int i = 0; i < tabCount; i++) {

            int iconRes = 0;
            String title = "";
            if (pager.getAdapter() instanceof IconTabProvider) {
                iconRes = ((IconTabProvider) pager.getAdapter()).getPageIconResId(i);
            } else if (pager.getAdapter() instanceof TextTabProvider) {
                title = ((TextTabProvider) pager.getAdapter()).getPageTitle(i);
            }
            if (iconRes != 0 && !TextUtils.isEmpty(title)) {
                addTextTab(i, title, iconRes, iconPadding, iconGravity);
            } else if (iconRes != 0) {
                addIconTab(i, iconRes);
            } else {
                addTextTab(i, title);
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

    private void addTextTab(final int position, String title, int iconRes, int iconPadding, int gravity) {
        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        Drawable drawable = getResources().getDrawable(iconRes);
        tab.setCompoundDrawablePadding(iconPadding);
        switch (gravity) {
            case Gravity.START:
                tab.setCompoundDrawablesRelative(drawable, null, null, null);
                break;
            case Gravity.END:
                tab.setCompoundDrawablesRelative(null, null, drawable, null);
                break;
            case Gravity.TOP:
                tab.setCompoundDrawablesRelative(null, drawable, null, null);
                break;
            case Gravity.BOTTOM:
                tab.setCompoundDrawablesRelative(null, null, null, drawable);
                break;
        }
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
        tabsContainer.addView(tab, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
    }

    private void updateTabStyles() {

        for (int i = 0; i < tabCount; i++) {

            View v = tabsContainer.getChildAt(i);

            v.setBackgroundResource(tabBackgroundResId);

            if (v instanceof TextView) {

                TextView tab = (TextView) v;
                tab.setTypeface(tabTypeface);
                tab.setTextColor(tabTextColor);// 设置的是单一色

                // ############ 源码修改 ##############
                // ###### 设置选中色 ############
                if (i == pager.getCurrentItem()) {
                    // 给选中的颜色
                    tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabSelectTextSize);
                    tab.setTextColor(tabTextSelectedColor);
                } else {
                    // 给未选中的颜色
                    tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
                    tab.setTextColor(tabTextNormalColor);
                }
                if (mTextBold == TEXT_BOLD_BOTH) {
                    tab.setTypeface(Typeface.DEFAULT_BOLD);
                } else if (mTextBold == TEXT_BOLD_NONE) {
                    tab.setTypeface(Typeface.DEFAULT);
                } else if (mTextBold == TEXT_BOLD_WHEN_SELECT) {
                    tab.setTypeface(i == pager.getCurrentItem() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
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
            float width = lineRight - lineLeft;

            float currentLeft = lineLeft;
            float currentRight = lineRight;

            float currentM = currentLeft + (currentRight - currentLeft) / 2;
            float localIndicatorWidth = width;
            if (indicatorWidth > 0 && !dividerExpand) {
                localIndicatorWidth = (int) indicatorWidth;
            }
            // if there is an offset, start interpolating left and right coordinates
            // between current and next tab
            if (currentPosition < tabCount - 1) {

                View nextTab = tabsContainer.getChildAt(currentPosition + 1);
                final float nextTabLeft = nextTab.getLeft();
                final float nextTabRight = nextTab.getRight();

                //每个item的中间值
                float nextM = nextTabLeft + (nextTabRight - nextTabLeft) / 2;
                //当前item的中间值距离下一个item的中间值
                float length = nextM - currentM;
                if (indicatorWidth <= 0 || dividerExpand) {
                    localIndicatorWidth = (int) length;
                }

                if (currentPositionOffset > 0f) {
                    Log.d(TAG, "FlexLength currentPositionOffset:" + currentPositionOffset);

                    if (dividerNeedFlex) {
                        float offset = length;
                        if (dividerFlexLength <= 0) {
                            dividerFlexLength = offset;
                        }

                        float scale = dividerFlexLength / 2 / offset;
                        Log.d(TAG, "FlexLength mType:" + scale);
                        float a1;
                        float a2;
                        float b1;
                        float b2;

                        /**
                         * 0< x < mType 时 （x = currentPositionOffset）;
                         * left = 0.4 * x ;
                         * right = 1.6 * x;
                         */
                        if (currentPositionOffset < scale) {
                            a1 = 0.4f;
                            b1 = 0;
                            a2 = 1.6f;
                            b2 = 0;
                        } else if (currentPositionOffset >= 1 - scale) {
                            /**
                             * 1 - mType <= x <=1 时
                             * left = 1.6 * x - 0.6;
                             * right = 1.6 * x + 0.6;
                             */
                            a1 = 1.6f;
                            b1 = -0.6f;
                            a2 = 0.4f;
                            b2 = 0.6f;
                        } else {
                            /**
                             * mType < x < 1 - mType 时
                             * left = x - 0.6 * mType;
                             * right = x + 0.6 * mType;
                             */
                            a1 = 1f;
                            b1 = -0.6f * scale;
                            a2 = 1f;
                            b2 = 0.6f * scale;
                        }

                        lineLeft = (currentM + (a1 * currentPositionOffset + b1) * offset)- localIndicatorWidth / 2.f;
                        lineRight = (currentM + (a2 * currentPositionOffset + b2) * offset)+ localIndicatorWidth / 2.f;

                    } else {
                        lineLeft = length * currentPositionOffset + currentM - localIndicatorWidth / 2.f;
                        lineRight = lineLeft + localIndicatorWidth;
                        Log.d(TAG, "nextM:" + nextM + ",currentM:" + currentM + ",lineLeft:" + lineLeft + ",lineRight:" + lineRight);
                    }
                } else {
                    if (dividerExpand) {
                        localIndicatorWidth = (int) width;
                    }
                    lineLeft = currentM - localIndicatorWidth / 2.f;
                    lineRight = lineLeft + localIndicatorWidth;
                }
            }else {
                lineLeft = currentM - localIndicatorWidth / 2.f;
                lineRight = lineLeft + localIndicatorWidth;
            }

            Log.d(TAG, "localIndicatorWidth:" + localIndicatorWidth+",width:"+width);
            if (dividerExpand) {
                rectF.set(lineLeft + tabPadding + getPaddingLeft(), height - indicatorHeight - dividerPaddingBottom,
                        lineRight - tabPadding + getPaddingLeft(), height - dividerPaddingBottom);
                canvas.drawRoundRect(rectF, corner, corner, rectPaint);
            } else {

                float left = lineLeft;// + offset/2;
                float right = lineRight;// - offset/2;

                rectF.set(left + getPaddingLeft(), height - indicatorHeight - dividerPaddingBottom,
                        right + getPaddingLeft(), height - dividerPaddingBottom);
                Log.d(TAG, "rectF:" + rectF.width());
                canvas.drawRoundRect(rectF, corner, corner, rectPaint);

            }
        } catch (Exception e) {
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

    private class PageListener extends ViewPager2.OnPageChangeCallback {

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {

            Log.d(TAG, "position:" + position + ",positionOffset:" + positionOffset + ",positionOffsetPixels:" + positionOffsetPixels);

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
