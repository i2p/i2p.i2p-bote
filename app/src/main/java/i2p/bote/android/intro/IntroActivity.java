package i2p.bote.android.intro;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.viewpagerindicator.LinePageIndicator;

import i2p.bote.android.BoteActivityBase;
import i2p.bote.android.R;

public class IntroActivity extends BoteActivityBase {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Create the sections adapter.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Bind the page indicator to the pager.
        LinePageIndicator pageIndicator = (LinePageIndicator)findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(mViewPager);

        findViewById(R.id.skip_intro).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the intro sections.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 6;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    return inflater.inflate(R.layout.fragment_intro_1, container, false);
                case 2:
                    return inflater.inflate(R.layout.fragment_intro_2, container, false);
                case 3:
                    return inflater.inflate(R.layout.fragment_intro_3, container, false);
                case 4:
                    return inflater.inflate(R.layout.fragment_intro_4, container, false);
                case 5:
                    View v5 = inflater.inflate(R.layout.fragment_intro_5, container, false);
                    Button b = (Button) v5.findViewById(R.id.start_setup_wizard);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getActivity().setResult(Activity.RESULT_OK);
                            getActivity().finish();
                        }
                    });
                    return v5;

                default:
                    View v0 = inflater.inflate(R.layout.fragment_intro_0, container, false);
                    TextView tv = (TextView) v0.findViewById(R.id.intro_app_name);
                    tv.append(".");

                    TextView swipe = (TextView) v0.findViewById(R.id.intro_swipe_to_start);
                    swipe.setCompoundDrawablesWithIntrinsicBounds(
                            new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_arrow_back)
                                    .colorRes(R.color.md_grey_600).sizeDp(24).paddingDp(4),
                            null, null, null
                    );

                    return v0;
            }
        }
    }
}
