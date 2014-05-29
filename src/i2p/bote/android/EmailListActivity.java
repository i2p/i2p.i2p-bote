package i2p.bote.android;

import net.i2p.client.I2PClient;
import i2p.bote.I2PBote;
import i2p.bote.android.addressbook.AddressBookActivity;
import i2p.bote.android.config.SettingsActivity;
import i2p.bote.android.service.BoteService;
import i2p.bote.android.util.MoveToDialogFragment;
import i2p.bote.folder.EmailFolder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
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

public class EmailListActivity extends ActionBarActivity implements
        EmailListFragment.OnEmailSelectedListener,
        MoveToDialogFragment.MoveToDialogListener {
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

    private static final String SHARED_PREFS = "i2p.bote";
    private static final String PREF_NAV_DRAWER_OPENED = "navDrawerOpened";
    private static final String ACTIVE_FOLDER = "activeFolder";

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

                // Update network status
                Drawable statusIcon;
                switch (I2PBote.getInstance().getNetworkStatus()) {
                case DELAY:
                    mNetworkStatus.setText(R.string.connect_delay);
                    statusIcon = getResources().getDrawable(android.R.drawable.presence_away);
                    break;
                case CONNECTING:
                    mNetworkStatus.setText(R.string.connecting);
                    statusIcon = getResources().getDrawable(android.R.drawable.presence_away);
                    break;
                case CONNECTED:
                    mNetworkStatus.setText(R.string.connected);
                    statusIcon = getResources().getDrawable(android.R.drawable.presence_online);
                    break;
                case ERROR:
                    mNetworkStatus.setText(R.string.error);
                    statusIcon = getResources().getDrawable(android.R.drawable.presence_busy);
                    break;
                case NOT_STARTED:
                default:
                    mNetworkStatus.setText(R.string.not_started);
                    statusIcon = getResources().getDrawable(android.R.drawable.presence_offline);
                }
                mNetworkStatus.setCompoundDrawablesWithIntrinsicBounds(
                        statusIcon, null, null, null);
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
                switch (I2PBote.getInstance().getNetworkStatus()) {
                case NOT_STARTED:
                case DELAY:
                    DialogFragment df = new DialogFragment() {
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.network_info_unavailable)
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
            Intent start = new Intent(this, BoteService.class);
            startService(start);
            return true;

        case R.id.action_stop_bote:
            Intent stop = new Intent(this, BoteService.class);
            stopService(stop);
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

    private class InitActivities {
        private final Context ctx;
        private final String myDir;

        public InitActivities(Context c) {
            ctx = c;
            // This needs to be changed so that we can have an alternative place
            myDir = c.getFilesDir().getAbsolutePath();
        }

        void initialize() {
            // Set up the locations so settings can find them
            System.setProperty("i2p.dir.base", myDir);
            System.setProperty("i2p.dir.config", myDir);
            System.setProperty("wrapper.logfile", myDir + "/wrapper.log");
        }
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
}
