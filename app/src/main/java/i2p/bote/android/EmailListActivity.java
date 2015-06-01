package i2p.bote.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import net.i2p.android.ui.I2PAndroidHelper;

import java.util.ArrayList;
import java.util.List;

import i2p.bote.I2PBote;
import i2p.bote.android.addressbook.AddressBookActivity;
import i2p.bote.android.config.SettingsActivity;
import i2p.bote.android.intro.IntroActivity;
import i2p.bote.android.intro.SetupActivity;
import i2p.bote.android.service.BoteService;
import i2p.bote.android.service.Init;
import i2p.bote.android.service.Init.RouterChoice;
import i2p.bote.android.util.BetterAsyncTaskLoader;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.fileencryption.PasswordCacheListener;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.network.NetworkStatusListener;

public class EmailListActivity extends BoteActivityBase implements
        LoaderManager.LoaderCallbacks<List<IDrawerItem>>,
        EmailListFragment.OnEmailSelectedListener,
        MoveToDialogFragment.MoveToDialogListener,
        PasswordCacheListener,
        NetworkStatusListener {
    private I2PAndroidHelper mHelper;
    private RouterChoice mRouterChoice;

    private SharedPreferences mSharedPrefs;

    /**
     * Navigation drawer variables
     */
    private Drawer mDrawer;
    private int mSelected;

    private static final String SHARED_PREFS = "i2p.bote";
    private static final String PREF_FIRST_START = "firstStart";

    private static final int SHOW_INTRODUCTION = 1;
    private static final int RUN_SETUP = 2;

    private static final int ID_ADDRESS_BOOK = 1;
    private static final int ID_NET_STATUS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Initialize variables
        mHelper = new I2PAndroidHelper(this);
        mSharedPrefs = getSharedPreferences(SHARED_PREFS, 0);

        IDrawerItem addressBook = new PrimaryDrawerItem()
                .withIdentifier(ID_ADDRESS_BOOK)
                .withName(R.string.address_book)
                .withIcon(R.drawable.ic_contacts_grey600_24dp)
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(R.color.primary);
        IDrawerItem networkStatus = getNetStatusItem(
                R.string.network_status, R.drawable.ic_cloud_off_grey600_24dp);

        // Set the drawer width per Material design spec
        // http://www.google.com/design/spec/layout/structure.html#structure-side-nav-1
        // Mobile: side nav width = min(screen width - app bar height, 320dp)
        // Desktop: side nav width = min(screen width - app bar height, 400dp)
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.nav_max_width);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int drawerWidth = Math.min(dm.widthPixels - toolbar.getLayoutParams().height, maxWidth);

        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withDrawerWidthPx(drawerWidth)
                .withShowDrawerOnFirstLaunch(true)
                .addStickyDrawerItems(addressBook, networkStatus)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem iDrawerItem) {
                        switch (iDrawerItem.getIdentifier()) {
                            case ID_ADDRESS_BOOK:
                                mDrawer.setSelection(mSelected, false);
                                mDrawer.closeDrawer();
                                Intent ai = new Intent(EmailListActivity.this, AddressBookActivity.class);
                                startActivity(ai);
                                return true;

                            case ID_NET_STATUS:
                                mDrawer.setSelection(mSelected, false);
                                netStatusSelected();
                                return true;

                            default:
                                drawerFolderSelected((EmailFolder) iDrawerItem.getTag(), mSelected == i);
                                mSelected = mDrawer.getCurrentSelection();
                                return false;
                        }
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        mSelected = mDrawer.getCurrentSelection();

        // Enable ActionBar app icon to behave as action to toggle nav drawer
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);

        if (savedInstanceState == null) {
            EmailListFragment f = EmailListFragment.newInstance("inbox");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.list_fragment, f).commit();
        }

        // If first start, go to introduction and setup wizard
        if (mSharedPrefs.getBoolean(PREF_FIRST_START, true)) {
            mSharedPrefs.edit().putBoolean(PREF_FIRST_START, false).apply();
            Intent i = new Intent(EmailListActivity.this, IntroActivity.class);
            startActivityForResult(i, SHOW_INTRODUCTION);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDrawer.saveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.main, menu);
        if (isBoteServiceRunning())
            menu.findItem(R.id.action_start_bote).setVisible(false);
        else
            menu.findItem(R.id.action_stop_bote).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("i2pbote.router.auto", true) ||
                prefs.getString("i2pbote.router.use", "internal").equals("android")) {
            // Try to bind to I2P Android
            mHelper.bind();
        }

        I2PBote.getInstance().addPasswordCacheListener(this);
        I2PBote.getInstance().addNetworkStatusListener(this);
        // Fetch current network status
        networkStatusChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHelper.unbind();

        I2PBote.getInstance().removePasswordCacheListener(this);
        I2PBote.getInstance().removeNetworkStatusListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_bote:
                // Init from settings
                Init init = new Init(this);
                mRouterChoice = init.initialize(mHelper);

                if (mRouterChoice == RouterChoice.ANDROID) {
                    if (!mHelper.isI2PAndroidInstalled()) {
                        // I2P Android not installed
                        mHelper.promptToInstall(this);
                    } else if (!mHelper.isI2PAndroidRunning()) {
                        // Ask user to start I2P Android
                        mHelper.requestI2PAndroidStart(this);
                    } else
                        startBote();
                } else
                    startBote();
                return true;

            case R.id.action_stop_bote:
                Intent stop = new Intent(this, BoteService.class);
                stopService(stop);
                supportInvalidateOptionsMenu();
                return true;

            case R.id.action_settings:
                Intent si = new Intent(this, SettingsActivity.class);
                startActivity(si);
                return true;

            case R.id.action_help:
                Intent hi = new Intent(this, HelpActivity.class);
                startActivity(hi);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SHOW_INTRODUCTION) {
            if (resultCode == RESULT_OK) {
                Intent i = new Intent(EmailListActivity.this, SetupActivity.class);
                startActivityForResult(i, RUN_SETUP);
            }
        } else if (requestCode == RUN_SETUP) {
            if (resultCode == RESULT_OK) {
                // TODO implement a UI tutorial?
            }
        } else if (requestCode == I2PAndroidHelper.REQUEST_START_I2P) {
            if (resultCode == RESULT_OK) {
                startBote();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }


    //
    // Helpers
    //

    private IDrawerItem getNetStatusItem(int nameRes, int icRes) {
        return new PrimaryDrawerItem()
                .withIdentifier(ID_NET_STATUS)
                .withName(nameRes)
                .withIcon(icRes)
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(R.color.primary);
    }

    private void drawerFolderSelected(EmailFolder folder, boolean alreadySelected) {
        if (!alreadySelected) {
            // Create the new fragment
            EmailListFragment f = EmailListFragment.newInstance(folder.getName());

            // Insert the fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.list_fragment, f).commit();
        }
        // Close the drawer
        mDrawer.closeDrawer();
    }

    private void netStatusSelected() {
        int boteNotStartedMessage = R.string.network_info_unavailable;
        switch (I2PBote.getInstance().getNetworkStatus()) {
            case DELAY:
                boteNotStartedMessage = R.string.network_info_unavailable_delay;
            case NOT_STARTED:
                final int message = boteNotStartedMessage;
                DialogFragment df = new DialogFragment() {
                    @Override
                    @NonNull
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(message)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        return builder.create();
                    }
                };
                df.show(getSupportFragmentManager(), "noinfo");
                break;
            default:
                mDrawer.closeDrawer();
                Intent nii = new Intent(EmailListActivity.this, NetworkInfoActivity.class);
                startActivity(nii);
        }
    }

    private boolean isBoteServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BoteService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    private void startBote() {
        Intent start = new Intent(this, BoteService.class);
        start.putExtra(BoteService.ROUTER_CHOICE, mRouterChoice);
        startService(start);
        supportInvalidateOptionsMenu();
    }


    //
    // Interfaces
    //

    // LoaderManager.LoaderCallbacks<List<IDrawerItem>>

    @Override
    public Loader<List<IDrawerItem>> onCreateLoader(int id, Bundle args) {
        return new DrawerFolderLoader(this, I2PBote.getInstance().getEmailFolders());
    }

    private static class DrawerFolderLoader extends BetterAsyncTaskLoader<List<IDrawerItem>> implements FolderListener {
        private List<EmailFolder> mFolders;

        public DrawerFolderLoader(Context context, List<EmailFolder> folders) {
            super(context);
            mFolders = folders;
        }

        @Override
        public List<IDrawerItem> loadInBackground() {
            ArrayList<IDrawerItem> drawerItems = new ArrayList<>();

            for (EmailFolder folder : mFolders) {
                drawerItems.add(getFolderDrawerItem(folder));
            }

            return drawerItems;
        }

        private IDrawerItem getFolderDrawerItem(EmailFolder folder) {
            PrimaryDrawerItem item = new PrimaryDrawerItem()
                    .withIdentifier(folder.hashCode())
                    .withTag(folder)
                    .withIconTintingEnabled(true)
                    .withSelectedIconColorRes(R.color.primary)
                    .withIcon(BoteHelper.getFolderIcon(getContext(), folder))
                    .withName(BoteHelper.getFolderDisplayName(getContext(), folder));

            try {
                int numNew = folder.getNumNewEmails();
                if (numNew > 0)
                    item.withBadge("" + numNew);
            } catch (PasswordException e) {
                // Password fetching is handled in EmailListFragment
            }

            return item;
        }

        @Override
        protected void onStartMonitoring() {
            if (mFolders != null) {
                for (EmailFolder folder : mFolders) {
                    folder.addFolderListener(this);
                }
            }
        }

        @Override
        protected void onStopMonitoring() {
            if (mFolders != null) {
                for (EmailFolder folder : mFolders) {
                    folder.removeFolderListener(this);
                }
            }
        }

        @Override
        protected void releaseResources(List<IDrawerItem> data) {
        }

        // FolderListener

        @Override
        public void elementAdded(String s) {
            onContentChanged();
        }

        @Override
        public void elementUpdated() {
            onContentChanged();
        }

        @Override
        public void elementRemoved(String s) {
            onContentChanged();
        }
    }

    @Override
    public void onLoadFinished(Loader<List<IDrawerItem>> loader, List<IDrawerItem> data) {
        if (mDrawer.getDrawerItems() == null || mDrawer.getDrawerItems().size() == 0)
            mDrawer.setItems((ArrayList<IDrawerItem>) data);
        else {
            // Assumes that no folders have been added or removed
            // TODO change this if necessary when user folders are implemented
            for (IDrawerItem item : data) {
                mDrawer.updateItem(item);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<IDrawerItem>> loader) {
        mDrawer.removeAllItems();
    }

    // FolderFragment.OnEmailSelectedListener

    @Override
    public void onEmailSelected(String folderName, String messageId) {
        // In single-pane mode, simply start the detail activity
        // for the selected message ID.
        Intent detailIntent = new Intent(this, ViewEmailActivity.class);
        detailIntent.putExtra(ViewEmailActivity.FOLDER_NAME, folderName);
        detailIntent.putExtra(ViewEmailActivity.MESSAGE_ID, messageId);
        startActivity(detailIntent);
    }

    // MoveToDialogFragment.MoveToDialogListener

    @Override
    public void onFolderSelected(EmailFolder newFolder) {
        EmailListFragment f = (EmailListFragment) getSupportFragmentManager().findFragmentById(R.id.list_fragment);
        f.onFolderSelected(newFolder);
    }

    // PasswordCacheListener

    @Override
    public void passwordProvided() {
        // Trigger the loader to show the drawer badges
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void passwordCleared() {
        // Trigger the loader to hide the drawer badges
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    // NetworkStatusListener

    @Override
    public void networkStatusChanged() {
        // Update network status
        final int statusText;
        final int statusIcon;
        switch (I2PBote.getInstance().getNetworkStatus()) {
            case DELAY:
                statusText = R.string.connect_delay;
                statusIcon = R.drawable.ic_av_timer_grey600_24dp;
                break;
            case CONNECTING:
                statusText = R.string.connecting;
                statusIcon = R.drawable.ic_cloud_queue_grey600_24dp;
                break;
            case CONNECTED:
                statusText = R.string.connected;
                statusIcon = R.drawable.ic_cloud_done_grey600_24dp;
                break;
            case ERROR:
                statusText = R.string.error;
                statusIcon = R.drawable.ic_error_red_24dp;
                break;
            case NOT_STARTED:
            default:
                statusText = R.string.not_started;
                statusIcon = R.drawable.ic_cloud_off_grey600_24dp;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO change this when #378 is resolved
                mDrawer.updateItem(getNetStatusItem(statusText, statusIcon));
            }
        });
    }
}
