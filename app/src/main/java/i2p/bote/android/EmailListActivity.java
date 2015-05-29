package i2p.bote.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.i2p.android.ui.I2PAndroidHelper;

import i2p.bote.I2PBote;
import i2p.bote.android.addressbook.AddressBookActivity;
import i2p.bote.android.config.SettingsActivity;
import i2p.bote.android.intro.IntroActivity;
import i2p.bote.android.intro.SetupActivity;
import i2p.bote.android.service.BoteService;
import i2p.bote.android.service.Init;
import i2p.bote.android.service.Init.RouterChoice;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.fileencryption.PasswordCacheListener;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.network.NetworkStatusListener;

public class EmailListActivity extends BoteActivityBase implements
        FolderListAdapter.OnFolderSelectedListener,
        EmailListFragment.OnEmailSelectedListener,
        MoveToDialogFragment.MoveToDialogListener,
        PasswordCacheListener,
        FolderListener,
        NetworkStatusListener {
    private I2PAndroidHelper mHelper;
    private RouterChoice mRouterChoice;

    private SharedPreferences mSharedPrefs;

    /**
     * Navigation drawer variables
     */
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mDrawerOuter;
    private FolderListAdapter mFolderAdapter;
    private ImageView mNetworkStatusIcon;
    private TextView mNetworkStatusText;
    private ActionBarDrawerToggle mDrawerToggle;

    private static final String SHARED_PREFS = "i2p.bote";
    private static final String PREF_NAV_DRAWER_OPENED = "navDrawerOpened";
    private static final String PREF_FIRST_START = "firstStart";
    private static final String ACTIVE_FOLDER = "activeFolder";

    private static final int SHOW_INTRODUCTION = 1;
    private static final int RUN_SETUP = 2;

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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerOuter = (RelativeLayout) findViewById(R.id.drawer_outer);
        RecyclerView mFolderList = (RecyclerView) findViewById(R.id.drawer);
        mNetworkStatusIcon = (ImageView) findViewById(R.id.network_status_icon);
        mNetworkStatusText = (TextView) findViewById(R.id.network_status_text);

        // Set the drawer width per Material design spec
        // http://www.google.com/design/spec/layout/structure.html#structure-side-nav-1
        // Mobile: side nav width = min(screen width - app bar height, 320dp)
        // Desktop: side nav width = min(screen width - app bar height, 400dp)
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.nav_max_width);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        DrawerLayout.LayoutParams lp = (DrawerLayout.LayoutParams) mDrawerOuter.getLayoutParams();
        lp.width = Math.min(dm.widthPixels - toolbar.getLayoutParams().height, maxWidth);

        // Set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mFolderList.setLayoutManager(mLayoutManager);

        mFolderAdapter = new FolderListAdapter(this, this);

        // Set the list of folders
        // TODO: This is slow, needs a loader
        mFolderAdapter.setFolders(I2PBote.getInstance().getEmailFolders(), this);

        // Set the adapter for the list view
        mFolderList.setAdapter(mFolderAdapter);

        // Enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up drawer toggle
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            private boolean wasDragged = false;

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                // Don't mark as opened if the user closed by dragging
                // but uses the action bar icon to open
                wasDragged = false;
                supportInvalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View view) {
                if (wasDragged && !mSharedPrefs.getBoolean(PREF_NAV_DRAWER_OPENED, false)) {
                    SharedPreferences.Editor edit = mSharedPrefs.edit();
                    edit.putBoolean(PREF_NAV_DRAWER_OPENED, true);
                    edit.apply();
                }
                supportInvalidateOptionsMenu();
            }

            /** Called when the drawer motion state changes. */
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    wasDragged = true;
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            EmailListFragment f = EmailListFragment.newInstance("inbox");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.list_fragment, f).commit();
        } else {
            mFolderAdapter.setSelected(savedInstanceState.getInt(ACTIVE_FOLDER));
        }

        // Set up fixed actions
        findViewById(R.id.address_book).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mDrawerLayout.closeDrawer(mDrawerOuter);
                Intent ai = new Intent(EmailListActivity.this, AddressBookActivity.class);
                startActivity(ai);
            }
        });
        findViewById(R.id.network_status).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
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
                        mDrawerLayout.closeDrawer(mDrawerOuter);
                        Intent nii = new Intent(EmailListActivity.this, NetworkInfoActivity.class);
                        startActivity(nii);
                }
            }
        });

        // Open nav drawer if the user has never opened it themselves
        if (!mSharedPrefs.getBoolean(PREF_NAV_DRAWER_OPENED, false))
            mDrawerLayout.openDrawer(mDrawerOuter);

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
        outState.putInt(ACTIVE_FOLDER, mFolderAdapter.getSelected());
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
    protected void onStop() {
        super.onStop();
        mHelper.unbind();

        I2PBote.getInstance().removePasswordCacheListener(this);
        I2PBote.getInstance().removeNetworkStatusListener(this);
    }

    private boolean isBoteServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BoteService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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

    private void startBote() {
        Intent start = new Intent(this, BoteService.class);
        start.putExtra(BoteService.ROUTER_CHOICE, mRouterChoice);
        startService(start);
        supportInvalidateOptionsMenu();
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // FolderListAdapter.OnFolderSelectedListener

    public void onDrawerFolderSelected(EmailFolder folder, boolean alreadySelected) {
        if (!alreadySelected) {
            // Create the new fragment
            EmailListFragment f = EmailListFragment.newInstance(folder.getName());

            // Insert the fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.list_fragment, f).commit();
        }
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawerOuter);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFolderAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void passwordCleared() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFolderAdapter.notifyDataSetChanged();
            }
        });
    }

    // FolderListener

    @Override
    public void elementAdded(String messageId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFolderAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void elementUpdated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFolderAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void elementRemoved(String messageId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFolderAdapter.notifyDataSetChanged();
            }
        });
    }

    // NetworkStatusListener

    @Override
    public void networkStatusChanged() {
        // Update network status
        final int statusText;
        final Drawable statusIcon;
        switch (I2PBote.getInstance().getNetworkStatus()) {
            case DELAY:
                statusText = R.string.connect_delay;
                statusIcon = getResources().getDrawable(R.drawable.ic_av_timer_grey600_24dp);
                break;
            case CONNECTING:
                statusText = R.string.connecting;
                statusIcon = getResources().getDrawable(R.drawable.ic_cloud_queue_grey600_24dp);
                break;
            case CONNECTED:
                statusText = R.string.connected;
                statusIcon = getResources().getDrawable(R.drawable.ic_cloud_done_grey600_24dp);
                break;
            case ERROR:
                statusText = R.string.error;
                statusIcon = getResources().getDrawable(R.drawable.ic_error_red_24dp);
                break;
            case NOT_STARTED:
            default:
                statusText = R.string.not_started;
                statusIcon = getResources().getDrawable(R.drawable.ic_cloud_off_grey600_24dp);
        }
        mNetworkStatusText.post(new Runnable() {
            @Override
            public void run() {
                mNetworkStatusText.setText(statusText);
            }
        });
        mNetworkStatusIcon.post(new Runnable() {
            @Override
            public void run() {
                mNetworkStatusIcon.setImageDrawable(statusIcon);
            }
        });
    }
}
