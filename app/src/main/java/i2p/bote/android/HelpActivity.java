package i2p.bote.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;

import com.viewpagerindicator.TitlePageIndicator;

public class HelpActivity extends BoteActivityBase {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the sections adapter.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Bind the page indicator to the pager.
        TitlePageIndicator pageIndicator = (TitlePageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(mViewPager);
    }


    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the help sections.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return getString(R.string.pref_title_identities);
                case 2:
                    return getString(R.string.changelog);
                case 3:
                    return getString(R.string.about);
                case 0:
                default:
                    return getString(R.string.start);
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 1:
                    return HelpHtmlFragment.newInstance(R.raw.help_identities);
                case 2:
                    return HelpHtmlFragment.newInstance(R.raw.help_changelog);
                case 3:
                    return new HelpAboutFragment();
                case 0:
                default:
                    return HelpHtmlFragment.newInstance(R.raw.help_start);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
