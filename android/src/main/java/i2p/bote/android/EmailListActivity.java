package i2p.bote.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.IIcon;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import net.i2p.android.ui.I2PAndroidHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.mail.MessagingException;

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
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.IdentitiesListener;
import i2p.bote.fileencryption.PasswordCacheListener;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.network.NetworkStatusListener;

public class EmailListActivity extends BoteActivityBase implements
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
    private AccountHeader mAccountHeader;
    private Drawer mDrawer;
    private long mSelected;

    private static final String PREF_FIRST_START = "firstStart";

    private static final int SHOW_INTRODUCTION = 1;
    private static final int RUN_SETUP = 2;

    private static final int ID_ADDRESS_BOOK = 1;
    private static final int ID_NET_STATUS = 2;
    private static final int ID_ALL_MAIL = 3;
    private static final int ID_LOCKED = 4;

    private static final int LOADER_IDENTITIES = 0;
    private static final int LOADER_DRAWER_FOLDERS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Initialize variables
        mHelper = new I2PAndroidHelper(this);
        mSharedPrefs = getSharedPreferences(Constants.SHARED_PREFS, 0);

        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(getLockedProfile())
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile.getIdentifier() == ID_LOCKED)
                            findViewById(R.id.action_log_in).performClick();
                        else if (!currentProfile)
                            identitySelected(profile);
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        IDrawerItem addressBook = new PrimaryDrawerItem()
                .withIdentifier(ID_ADDRESS_BOOK)
                .withName(R.string.address_book)
                .withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_contacts).colorRes(R.color.md_grey_600).sizeDp(24))
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(R.color.primary);
        IDrawerItem networkStatus = getNetStatusItem(
                R.string.network_status, GoogleMaterial.Icon.gmd_cloud_off, R.color.md_grey_600, 0);

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
                .withAccountHeader(mAccountHeader)
                .addStickyDrawerItems(addressBook, networkStatus)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int i, IDrawerItem iDrawerItem) {
                        long id = iDrawerItem.getIdentifier();
                        if (id == ID_ADDRESS_BOOK) {
                            mDrawer.setSelection(mSelected, false);
                            mDrawer.closeDrawer();
                            Intent ai = new Intent(EmailListActivity.this, AddressBookActivity.class);
                            startActivity(ai);
                            return true;
                        } else if (id == ID_NET_STATUS) {
                            mDrawer.setSelection(mSelected, false);
                            netStatusSelected();
                            return true;
                        } else {
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
        mAccountHeader.saveInstanceState(outState);
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

        if (I2PBote.getInstance().isPasswordRequired()) {
            // Ensure any existing data is destroyed.
            getSupportLoaderManager().destroyLoader(LOADER_IDENTITIES);
        } else {
            // Password is cached, or not set.
            getSupportLoaderManager().initLoader(LOADER_IDENTITIES, null, new IdentityLoaderCallbacks());
        }

        getSupportLoaderManager().initLoader(LOADER_DRAWER_FOLDERS, null, new DrawerFolderLoaderCallbacks());
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

    private IProfile getLockedProfile() {
        return new ProfileDrawerItem()
                .withIdentifier(ID_LOCKED)
                .withEmail(getString(R.string.touch_lock_to_log_in))
                .withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_lock).color(Color.WHITE).sizeRes(com.mikepenz.materialdrawer.R.dimen.material_drawer_account_header_selected));
    }

    private IDrawerItem getNetStatusItem(int nameRes, IIcon icon, int iconColorRes, int padding) {
        return new PrimaryDrawerItem()
                .withIdentifier(ID_NET_STATUS)
                .withName(nameRes)
                .withIcon(new IconicsDrawable(this, icon).colorRes(iconColorRes).sizeDp(24).paddingDp(padding))
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(R.color.primary);
    }

    private void identitySelected(IProfile profile) {
        EmailIdentity identity = (EmailIdentity) ((ProfileDrawerItem) profile).getTag();
        mSharedPrefs.edit()
                .putString(Constants.PREF_SELECTED_IDENTITY,
                        identity == null ? null : identity.getKey())
                .apply();
        // Trigger the drawer folder loader to update the drawer badges
        getSupportLoaderManager().restartLoader(LOADER_DRAWER_FOLDERS, null, new DrawerFolderLoaderCallbacks());
        EmailListFragment f = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.list_fragment);
        f.onIdentitySelected();
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
                DialogFragment df = NetStatusDialogFragment.newInstance(boteNotStartedMessage);
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

    public static class NetStatusDialogFragment extends DialogFragment {
        public static DialogFragment newInstance(int message) {
            DialogFragment f = new NetStatusDialogFragment();
            Bundle args = new Bundle();
            args.putInt("message", message);
            f.setArguments(args);
            return f;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int message = getArguments().getInt("message");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            return builder.create();
        }
    }


    //
    // Loaders
    //

    private class IdentityLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<IProfile>> {
        @Override
        public Loader<ArrayList<IProfile>> onCreateLoader(int id, Bundle args) {
            return new DrawerIdentityLoader(EmailListActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<IProfile>> loader, ArrayList<IProfile> data) {
            mAccountHeader.setProfiles(data);
            String selectedIdentity = mSharedPrefs.getString(Constants.PREF_SELECTED_IDENTITY, null);
            for (IProfile profile : data) {
                EmailIdentity identity = (EmailIdentity) ((ProfileDrawerItem) profile).getTag();
                if ((identity == null && selectedIdentity == null) ||
                        (identity != null && identity.getKey().equals(selectedIdentity))) {
                    mAccountHeader.setActiveProfile(profile, true);
                    break;
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<IProfile>> loader) {
            mAccountHeader.clear();
            mAccountHeader.addProfiles(getLockedProfile());
        }
    }

    private static class DrawerIdentityLoader extends BetterAsyncTaskLoader<ArrayList<IProfile>> implements IdentitiesListener {
        private int identiconSize;

        public DrawerIdentityLoader(Context context) {
            super(context);
            // Must be a multiple of nine
            identiconSize = context.getResources().getDimensionPixelSize(R.dimen.identicon);
        }

        @Override
        public ArrayList<IProfile> loadInBackground() {
            ArrayList<IProfile> profiles = new ArrayList<>();
            try {
                // Fetch the identities first, so we trigger any exceptions
                Collection<EmailIdentity> identities = I2PBote.getInstance().getIdentities().getAll();
                profiles.add(new ProfileDrawerItem()
                        .withIdentifier(ID_ALL_MAIL)
                        .withTag(null)
                        .withEmail(getContext().getString(R.string.all_mail))
                        .withIcon(getContext().getResources().getDrawable(R.drawable.ic_contact_picture))
                );
                for (EmailIdentity identity : identities) {
                    profiles.add(getIdentityDrawerItem(identity));
                }
            } catch (PasswordException e) {
                // TODO handle, but should not get here
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return profiles;
        }

        private IProfile getIdentityDrawerItem(EmailIdentity identity) {
            return new ProfileDrawerItem()
                    .withIdentifier(identity.hashCode())
                    .withTag(identity)
                    .withName(identity.getDescription())
                    .withEmail(identity.getPublicName() + " <" + identity.getKey().substring(0, 4) + ">")
                    .withIcon(BoteHelper.getIdentityPicture(identity, identiconSize, identiconSize));
        }

        @Override
        protected void onStartMonitoring() {
            I2PBote.getInstance().getIdentities().addIdentitiesListener(this);
        }

        @Override
        protected void onStopMonitoring() {
            I2PBote.getInstance().getIdentities().removeIdentitiesListener(this);
        }

        @Override
        protected void releaseResources(ArrayList<IProfile> data) {
        }

        // IdentitiesListener

        @Override
        public void identityAdded(String s) {
            onContentChanged();
        }

        @Override
        public void identityUpdated(String s) {
            onContentChanged();
        }

        @Override
        public void identityRemoved(String s) {
            onContentChanged();
        }
    }

    private class DrawerFolderLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<IDrawerItem>> {
        @Override
        public Loader<ArrayList<IDrawerItem>> onCreateLoader(int id, Bundle args) {
            return new DrawerFolderLoader(EmailListActivity.this, I2PBote.getInstance().getEmailFolders());
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<IDrawerItem>> loader, ArrayList<IDrawerItem> data) {
            if (mDrawer.getDrawerItems() == null || mDrawer.getDrawerItems().size() == 0)
                mDrawer.setItems(data);
            else {
                // Assumes that no folders have been added or removed
                // TODO change this if necessary when user folders are implemented
                for (IDrawerItem item : data) {
                    mDrawer.updateItem(item);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<IDrawerItem>> loader) {
            mDrawer.removeAllItems();
        }
    }

    private static class DrawerFolderLoader extends BetterAsyncTaskLoader<ArrayList<IDrawerItem>> implements FolderListener {
        private List<EmailFolder> mFolders;

        public DrawerFolderLoader(Context context, List<EmailFolder> folders) {
            super(context);
            mFolders = folders;
        }

        @Override
        public ArrayList<IDrawerItem> loadInBackground() {
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
                int numNew = BoteHelper.getNumNewEmails(getContext(), folder);
                if (numNew > 0)
                    item.withBadge("" + numNew);
            } catch (PasswordException e) {
                // Password fetching is handled in EmailListFragment
            } catch (MessagingException | GeneralSecurityException | IOException e) {
                e.printStackTrace();
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
        protected void releaseResources(ArrayList<IDrawerItem> data) {
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


    //
    // Interfaces
    //

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
        // Password is cached, or not set.
        getSupportLoaderManager().restartLoader(LOADER_IDENTITIES, null, new IdentityLoaderCallbacks());
        // Trigger the drawer folder loader to show the drawer badges
        getSupportLoaderManager().restartLoader(LOADER_DRAWER_FOLDERS, null, new DrawerFolderLoaderCallbacks());
    }

    @Override
    public void passwordCleared() {
        // Ensure any existing data is destroyed.
        getSupportLoaderManager().destroyLoader(LOADER_IDENTITIES);
        // Trigger the drawer folder loader to hide the drawer badges
        getSupportLoaderManager().restartLoader(LOADER_DRAWER_FOLDERS, null, new DrawerFolderLoaderCallbacks());
        // Hide account selection list
        if (mAccountHeader.isSelectionListShown())
            mAccountHeader.toggleSelectionList(this);
    }

    // NetworkStatusListener

    @Override
    public void networkStatusChanged() {
        // Update network status
        final int statusText;
        final IIcon statusIcon;
        final int colorRes;
        final int padding;
        switch (I2PBote.getInstance().getNetworkStatus()) {
            case DELAY:
                statusText = R.string.connect_delay;
                statusIcon = GoogleMaterial.Icon.gmd_av_timer;
                colorRes = R.color.md_grey_600;
                padding = 3;
                break;
            case CONNECTING:
                statusText = R.string.connecting;
                statusIcon = GoogleMaterial.Icon.gmd_cloud_queue;
                colorRes = R.color.md_grey_600;
                padding = 0;
                break;
            case CONNECTED:
                statusText = R.string.connected;
                statusIcon = GoogleMaterial.Icon.gmd_cloud_done;
                colorRes = R.color.md_grey_600;
                padding = 0;
                break;
            case ERROR:
                statusText = R.string.error;
                statusIcon = GoogleMaterial.Icon.gmd_error;
                colorRes = R.color.red;
                padding = 2;
                break;
            case NOT_STARTED:
            default:
                statusText = R.string.not_started;
                statusIcon = GoogleMaterial.Icon.gmd_cloud_off;
                colorRes = R.color.md_grey_600;
                padding = 0;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDrawer.updateStickyFooterItem(getNetStatusItem(statusText, statusIcon, colorRes, padding));
            }
        });
    }
}
