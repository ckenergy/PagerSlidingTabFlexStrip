package com.cc.navigation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.util.*

class MainActivity : AppCompatActivity() {
    private val mFragments = ArrayList<Fragment>()
    private val mTitles = arrayOf("第一一", "第二二二", "第三")
    lateinit var mPagerSlidingTabStrip: PagerSlidingTabStrip
    lateinit var mPagerSlidingTabStrip2: PagerSlidingTabStrip2
    lateinit var mViewPager: ViewPager
    lateinit var mViewPager2: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPagerSlidingTabStrip = findViewById<View>(R.id.main_tabs) as PagerSlidingTabStrip
        mPagerSlidingTabStrip2 = findViewById<View>(R.id.main_tabs1) as PagerSlidingTabStrip2
        mViewPager = findViewById<View>(R.id.viewpager) as ViewPager
        mViewPager2 = findViewById<View>(R.id.viewpager1) as ViewPager2
        val blankFragment = BlankFragment()
        val blankFragment1 = BlankFragment()
        val blankFragment3 = BlankFragment()
        mFragments.add(blankFragment)
        mFragments.add(blankFragment1)
        mFragments.add(blankFragment3)
        mViewPager.adapter = MainPagerAdapter(supportFragmentManager)
        mPagerSlidingTabStrip.setViewPager(mViewPager)

        initPager(mViewPager2, mPagerSlidingTabStrip2)
    }

    private fun initPager(viewPager: ViewPager2, tab: PagerSlidingTabStrip2) {
        viewPager.adapter = PagerAdapter(this)
        tab.setViewPager(viewPager)
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//            }
//        })
    }

    internal inner class PagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment), PagerSlidingTabStrip2.TextTabProvider
    {

        override fun getItemCount() = mTitles.size

        override fun createFragment(position: Int): Fragment {
            val fragment = BlankFragment()
            return fragment
        }

        override fun getPageTitle(position: Int) = mTitles[position]
//        override fun getPageIconResId(position: Int) = R.drawable.ic_icon_vip
    }

    internal inner class MainPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mTitles[position]
        }
    }

}