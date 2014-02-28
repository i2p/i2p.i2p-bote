package i2p.bote;

import java.util.ArrayList;
import java.util.List;

import i2p.bote.folder.EmailFolder;
import i2p.bote.util.BoteHelper;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

public class ViewEmailActivity extends ActionBarActivity {
    public static final String FOLDER_NAME = "folder_name";
    public static final String MESSAGE_ID = "message_id";

    private EmailFolder mFolder;
    private ViewPager mPager;
    private ViewEmailPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_email);

        Intent i = getIntent();
        String folderName = i.getStringExtra(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(
                folderName == null ? "inbox" : folderName);
        String messageId = i.getStringExtra(MESSAGE_ID);

        // Instantiate the ViewPager and PagerAdapter
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ViewEmailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        // Start with a "fake" list of messageIds containing only
        // the selected messageId, so the UI starts up quickly
        List<String> messageIds = new ArrayList<String>();
        messageIds.add(messageId);
        mPagerAdapter.setData(messageIds);

        // Now fire off a Loader to fetch the real list
        // TODO: Implement
    }

    private class ViewEmailPagerAdapter extends FragmentStatePagerAdapter {
        private List<String> mIds;

        public ViewEmailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setData(List<String> data) {
            mIds = data;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            if (mIds == null)
                return null;

            return ViewEmailFragment.newInstance(
                    mFolder.getName(), mIds.get(position));
        }

        @Override
        public int getItemPosition(Object item) {
            ViewEmailFragment f = (ViewEmailFragment) item;
            String messageId = f.getMessageId();
            int position = mIds.indexOf(messageId);

            if (position >= 0)
                return position;
            else
                return POSITION_NONE;
        }

        @Override
        public int getCount() {
            if (mIds == null)
                return 0;
            else
                return mIds.size();
        }
    }
}
