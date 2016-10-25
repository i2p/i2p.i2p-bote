package i2p.bote.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

public class ViewEmailActivity extends BoteActivityBase implements
        LoaderManager.LoaderCallbacks<List<String>> {
    public static final String FOLDER_NAME = "folder_name";
    public static final String MESSAGE_ID = "message_id";

    private static final int MESSAGE_ID_LIST_LOADER = 1;

    private EmailFolder mFolder;
    // The messageId of the currently-viewed Email
    private String mMessageId;
    private ViewPager mPager;
    private ViewEmailPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_email);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent i = getIntent();
        String folderName = i.getStringExtra(FOLDER_NAME);
        mFolder = BoteHelper.getMailFolder(
                folderName == null ? "inbox" : folderName);
        mMessageId = i.getStringExtra(MESSAGE_ID);

        // Instantiate the ViewPager and PagerAdapter
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ViewEmailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                mMessageId = mPagerAdapter.getMessageId(position);

                // Mark the visible email as not new
                if (mMessageId != null) {
                    try {
                        if (!BoteHelper.isOutbox(mFolder))
                            mFolder.setNew(mMessageId, false);
                        mFolder.setRecent(mMessageId, false);
                    } catch (PasswordException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (GeneralSecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        // Fire off a Loader to fetch the list of Emails
        getSupportLoaderManager().initLoader(MESSAGE_ID_LIST_LOADER, null, this);
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

        public int getPosition(String messageId) {
            if (mIds == null)
                return 0;
            else
                return mIds.indexOf(messageId);
        }

        public String getMessageId(int position) {
            if (mIds == null)
                return null;
            else
                return mIds.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            if (mIds == null)
                return null;
            else
                return ViewEmailFragment.newInstance(
                        mFolder.getName(), mIds.get(position));
        }

        @Override
        public int getCount() {
            if (mIds == null)
                return 0;
            else
                return mIds.size();
        }
    }

    // LoaderManager.LoaderCallbacks<List<String>>

    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        return new MessageIdListLoader(this, mFolder);
    }

    private static class MessageIdListLoader extends BetterAsyncTaskLoader<List<String>> implements
    FolderListener {
        private EmailFolder mFolder;

        public MessageIdListLoader(Context context, EmailFolder folder) {
            super(context);
            mFolder = folder;
        }

        @Override
        public List<String> loadInBackground() {
            List<String> messageIds = null;
            try {
                List<Email> emails = BoteHelper.getEmails(mFolder, null, true);
                messageIds = new ArrayList<String>();
                for (Email email : emails)
                    messageIds.add(email.getMessageID());
            } catch (PasswordException pe) {
                // TODO: Handle this error properly (get user to log in)
            }
            return messageIds;
        }

        protected void onStartMonitoring() {
            mFolder.addFolderListener(this);
        }

        protected void onStopMonitoring() {
            mFolder.removeFolderListener(this);
        }

        protected void releaseResources(List<String> data) {
        }

        // FolderListener

        @Override
        public void elementAdded(String messageId) {
            onContentChanged();
        }

        @Override
        public void elementUpdated() {
            onContentChanged();
        }

        @Override
        public void elementRemoved(String messageId) {
            onContentChanged();
        }
    }

    public void onLoadFinished(Loader<List<String>> loader,
            List<String> data) {
        mPagerAdapter.setData(data);
        mPager.setCurrentItem(
                mPagerAdapter.getPosition(mMessageId));

        // Mark the current email as not new
        if (mMessageId != null) {
            try {
                if (!BoteHelper.isOutbox(mFolder))
                    mFolder.setNew(mMessageId, false);
                mFolder.setRecent(mMessageId, false);
            } catch (PasswordException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void onLoaderReset(Loader<List<String>> loader) {
        mPagerAdapter.setData(null);
    }
}
