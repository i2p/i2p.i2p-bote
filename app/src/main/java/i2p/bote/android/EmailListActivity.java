package i2p.bote.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.i2p.android.router.service.IRouterState;

import i2p.bote.I2PBote;
import i2p.bote.android.addressbook.AddressBookActivity;
import i2p.bote.android.config.SettingsActivity;
import i2p.bote.android.intro.IntroActivity;
import i2p.bote.android.intro.SetupActivity;
import i2p.bote.android.service.BoteService;
import i2p.bote.android.service.Init;
import i2p.bote.android.service.Init.RouterChoice;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.folder.EmailFolder;
import i2p.bote.network.NetworkStatusListener;

public class EmailListActivity extends ActionBarActivity implements
        EmailListFragment.OnEmailSelectedListener,
        MoveToDialogFragment.MoveToDialogListener,
        NetworkStatusListener {
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private SharedPreferences mSharedPrefs;

    /**
     * Navigation drawer variables
     */
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mDrawerOuter;
    private FolderListAdapter mFolderAdapter;
    private ListView mFolderList;
    private TextView mNetworkStatus;
    private ActionBarDrawerToggle mDrawerToggle;
    RouterChoice mRouterChoice;
    IRouterState mStateService = null;

    private static final String SHARED_PREFS = "i2p.bote";
    private static final String PREF_NAV_DRAWER_OPENED = "navDrawerOpened";
    private static final String PREF_FIRST_START = "firstStart";
    private static final String ACTIVE_FOLDER = "activeFolder";

    private static final int SHOW_INTRODUCTION = 1;
    private static final int RUN_SETUP = 2;
    private static final int REQUEST_START_I2P = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        // Initialize variables
        mTitle = mDrawerTitle = getTitle();
        mSharedPrefs = getSharedPreferences(SHARED_PREFS, 0);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerOuter = (RelativeLayout) findViewById(R.id.drawer_outer);
        mFolderAdapter = new FolderListAdapter(this);
        mFolderList = (ListView) findViewById(R.id.drawer);
        mNetworkStatus = (TextView) findViewById(R.id.network_status);

        // Set the list of folders
        // TODO: This is slow, needs a loader
        mFolderAdapter.setData(I2PBote.getInstance().getEmailFolders());

        // Set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // Set the adapter for the list view
        mFolderList.setAdapter(mFolderAdapter);
        // Set the list's click listener
        mFolderList.setOnItemClickListener(new DrawerItemClickListener());

        // Enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up drawer toggle
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            private boolean wasDragged = false;

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                // Don't mark as opened if the user closed by dragging
                // but uses the action bar icon to open
                wasDragged = false;
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View view) {
                if (wasDragged && !mSharedPrefs.getBoolean(PREF_NAV_DRAWER_OPENED, false)) {
                    SharedPreferences.Editor edit = mSharedPrefs.edit();
                    edit.putBoolean(PREF_NAV_DRAWER_OPENED, true);
                    edit.commit();
                }
                getSupportActionBar().setTitle(mDrawerTitle);
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
            mFolderList.setItemChecked(0, true);
        } else {
            mFolderList.setItemChecked(
                    savedInstanceState.getInt(ACTIVE_FOLDER), true);
        }

        // Set up fixed actions
        findViewById(R.id.address_book).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent ai = new Intent(EmailListActivity.this, AddressBookActivity.class);
                startActivity(ai);
            }
        });
        mNetworkStatus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int boteNotStartedMessage = R.string.network_info_unavailable;
                switch (I2PBote.getInstance().getNetworkStatus()) {
                case DELAY:
                    boteNotStartedMessage = R.string.network_info_unavailable_delay;
                case NOT_STARTED:
                    final int message = boteNotStartedMessage;
                    DialogFragment df = new DialogFragment() {
                        @Override
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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            selectItem(pos);
        }
    }

    private void selectItem(int position) {
        // Create the new fragment
        EmailFolder folder = mFolderAdapter.getItem(position);
        EmailListFragment f = EmailListFragment.newInstance(folder.getName());

        // Insert the fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.list_fragment, f).commit();

        // Highlight the selected item and close the drawer
        mFolderList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerOuter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTIVE_FOLDER, mFolderList.getSelectedItemPosition());
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
            Intent i2pIntent = new Intent(IRouterState.class.getName());
            try {
                mTriedBindState = bindService(
                        i2pIntent, mStateConnection, BIND_AUTO_CREATE);
            } catch (SecurityException e) {
                // Old version of I2P Android (pre-0.9.13), cannot use
                mStateService = null;
                mTriedBindState = false;
            }
        }

        I2PBote.getInstance().addNetworkStatusListener(this);
        // Fetch current network status
        networkStatusChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mTriedBindState)
            unbindService(mStateConnection);
        mTriedBindState = false;

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
        if(mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
        case R.id.action_start_bote:
            // Init from settings
            Init init = new Init(this);
            mRouterChoice = init.initialize(mStateService);

            if (mRouterChoice == RouterChoice.ANDROID) {
                try {
                    if (mStateService == null) {
                        // I2P Android not installed
                        // TODO: handle
                    } else if (!mStateService.isStarted()) {
                        // Ask user to start I2P Android
                        DialogFragment df = new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.start_i2p_android)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        Intent i = new Intent("net.i2p.android.router.START_I2P");
                                        EmailListActivity.this.startActivityForResult(i, REQUEST_START_I2P);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                                return builder.create();
                            }
                        };
                        df.show(getSupportFragmentManager(), "starti2p");
                    } else
                        startBote();
                } catch (RemoteException e) {
                    // TODO log
                }
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
        } else if (requestCode == REQUEST_START_I2P) {
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
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
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

    // NetworkStatusListener

    @Override
    public void networkStatusChanged() {
        // Update network status
        final int statusText;
        final Drawable statusIcon;
        switch (I2PBote.getInstance().getNetworkStatus()) {
            case DELAY:
                statusText = R.string.connect_delay;
                statusIcon = getResources().getDrawable(android.R.drawable.presence_away);
                break;
            case CONNECTING:
                statusText = R.string.connecting;
                statusIcon = getResources().getDrawable(android.R.drawable.presence_away);
                break;
            case CONNECTED:
                statusText = R.string.connected;
                statusIcon = getResources().getDrawable(android.R.drawable.presence_online);
                break;
            case ERROR:
                statusText = R.string.error;
                statusIcon = getResources().getDrawable(android.R.drawable.presence_busy);
                break;
            case NOT_STARTED:
            default:
                statusText = R.string.not_started;
                statusIcon = getResources().getDrawable(android.R.drawable.presence_offline);
        }
        mNetworkStatus.post(new Runnable() {
            @Override
            public void run() {
                mNetworkStatus.setText(statusText);
                mNetworkStatus.setCompoundDrawablesWithIntrinsicBounds(
                        statusIcon, null, null, null);
            }
        });
    }


    //
    // I2P Android helpers
    //

    private boolean mTriedBindState;
    private ServiceConnection mStateConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mStateService = IRouterState.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mStateService = null;
        }
    };
}
