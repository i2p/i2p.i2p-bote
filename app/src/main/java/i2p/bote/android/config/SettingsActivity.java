package i2p.bote.android.config;

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import i2p.bote.android.util.SummaryEditTextPreference;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
    // Actions for legacy settings
    private static final String ACTION_PREFS_GENERAL = "i2p.bote.PREFS_GENERAL";

    static final int ALTER_IDENTITY_LIST = 1;

    // Preference Header vars
    private Header[] mIdentityListHeaders;

    private String mRequestedIdentityKey;
    private String mDeletingIdentityKey;

    // Async tasks
    private LoadIdentityListTask mLoadIdentityListTask;


    //
    // Android lifecycle
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize I2P settings
        InitActivities init = new InitActivities(this);
        init.initialize();

        String action = getIntent().getAction();
        if (action != null) {
            loadLegacySettings(action);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            buildLegacyHeaders();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateIdentities();
    }

    @Override
    protected void onPause() {
        Configuration config = I2PBote.getInstance().getConfiguration();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Map<String, ?> all = prefs.getAll();
        Iterator<String> iterator = all.keySet().iterator();
        while (iterator.hasNext()) {
            String x = iterator.next();
            if (x.startsWith("i2pbote.")) // Skip over Android-specific settings
                continue;
            else if ("autoMailCheckEnabled".equals(x))
                config.setAutoMailCheckEnabled(prefs.getBoolean(x, true));
            else if ("mailCheckInterval".equals(x))
                config.setMailCheckInterval(prefs.getInt(x, 30));
            else if ("deliveryCheckEnabled".equals(x))
                config.setDeliveryCheckEnabled(prefs.getBoolean(x, true));
            else if ("hideLocale".equals(x))
                config.setHideLocale(prefs.getBoolean(x, true));
            else if ("includeSentTime".equals(x))
                config.setIncludeSentTime(prefs.getBoolean(x, true));
            else if ("numSendHops".equals(x))
                config.setNumStoreHops(Integer.parseInt(prefs.getString(x, "0")));
            else if ("relayMinDelay".equals(x))
                config.setRelayMinDelay(prefs.getInt(x, 5));
            else if ("relayMaxDelay".equals(x))
                config.setRelayMaxDelay(prefs.getInt(x, 40));
        }

        config.save();

        // Store the settings in Android
        super.onPause();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_identities:
                // TODO ask if file should be encrypted
                askForExportFilename(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void askForExportFilename(String password) {
        // TODO ask for filename
        String exportFilename = "identities";
        checkIfExportFileExists(exportFilename, password);
    }

    private void checkIfExportFileExists(String exportFilename, String password) {
        File exportFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
        ), exportFilename + (password == null ? ".txt" : ".bote"));
        if (exportFile.exists()) {
            // TODO ask to rename or overwrite
            //askForExportFilename(password);
            exportIdentities(exportFile, password);
        } else
            exportIdentities(exportFile, password);
    }

    private void exportIdentities(File exportFile, String password) {
        try {
            // TODO ask for password
            I2PBote.getInstance().getIdentities().export(exportFile, password);
            Toast.makeText(this, R.string.identities_exported, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (PasswordException e) {
            e.printStackTrace();
        }
    }


    //
    // Building Headers
    //

    @SuppressWarnings("deprecation")
    private void buildLegacyHeaders() {
        // Always add general preferences as first header
        addPreferencesFromResource(R.xml.settings_headers_legacy);

        // Then add zero or more identity headers as necessary
        // TODO: implement
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        // The resource com.android.internal.R.bool.preferences_prefer_dual_pane
        // has different definitions based upon screen size. At present, it will
        // be true for -sw720dp devices, false otherwise. For your curiosity, in
        // Nexus 7 it is false.

        // Always add general preferences as first header
        target.clear();
        loadHeadersFromResource(R.xml.settings_headers, target);

        // Then add zero or more identity headers as necessary
        if (mIdentityListHeaders != null) {
            final int headerCount = mIdentityListHeaders.length;
            for (int index = 0; index < headerCount; index++) {
                Header header = mIdentityListHeaders[index];
                if (header != null && header.id != HEADER_ID_UNDEFINED) {
                    String key = header.extras.getString(
                            ViewIdentityFragment.IDENTITY_KEY);
                    if (key != mDeletingIdentityKey) {
                        target.add(header);
                        if (key == mRequestedIdentityKey) {
                            mRequestedIdentityKey = null;
                        }
                    }
                }
            }
        }
    }


    //
    // Settings pages
    //

    @SuppressWarnings("deprecation")
    private void loadLegacySettings(String action) {
        if (ACTION_PREFS_GENERAL.equals(action)) {
            addPreferencesFromResource(R.xml.settings_general);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            if ("general".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_general);

                final PreferenceCategory i2pCat = (PreferenceCategory)findPreference("i2pCategory");
                CheckBoxPreference routerAuto = (CheckBoxPreference)findPreference("i2pbote.router.auto");

                if (!routerAuto.isChecked()) {
                    setupI2PCategory(getActivity(), i2pCat);
                }

                routerAuto.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final Boolean checked = (Boolean) newValue;
                        if (!checked.booleanValue()) {
                            setupI2PCategory(getActivity(), i2pCat);
                        } else {
                            Preference p1 = i2pCat.findPreference("i2pbote.router.use");
                            Preference p2 = i2pCat.findPreference("i2pbote.i2cp.tcp.host");
                            Preference p3 = i2pCat.findPreference("i2pbote.i2cp.tcp.port");
                            if (p1 != null)
                                i2pCat.removePreference(p1);
                            if (p2 != null)
                                i2pCat.removePreference(p2);
                            if (p3 != null)
                                i2pCat.removePreference(p3);
                        }
                        return true;
                    }
                });
            }
        }
    }

    private static void setupI2PCategory(Context context, PreferenceCategory i2pCat) {
        final ListPreference routerChoice = createRouterChoice(context);
        final EditTextPreference hostField = createHostField(context);
        final EditTextPreference portField = createPortField(context);
        i2pCat.addPreference(routerChoice);
        i2pCat.addPreference(hostField);
        i2pCat.addPreference(portField);

        if ("remote".equals(routerChoice.getValue())) {
            hostField.setEnabled(true);
            portField.setEnabled(true);
        }

        routerChoice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String val = newValue.toString();
                int index = routerChoice.findIndexOfValue(val);
                if (index == 2) {
                    hostField.setEnabled(true);
                    hostField.setText("127.0.0.1");
                    portField.setEnabled(true);
                    portField.setText("7654");
                } else {
                    hostField.setEnabled(false);
                    hostField.setText("internal");
                    portField.setEnabled(false);
                    portField.setText("internal");
                }
                return true;
            }
        });
    }

    private static ListPreference createRouterChoice(Context context) {
        ListPreference routerChoice = new ListPreference(context);
        routerChoice.setKey("i2pbote.router.use");
        routerChoice.setEntries(R.array.routerOptionNames);
        routerChoice.setEntryValues(R.array.routerOptions);
        routerChoice.setTitle("Router");
        routerChoice.setSummary("%s");
        routerChoice.setDialogTitle("Router to use");
        routerChoice.setDefaultValue("internal");
        return routerChoice;
    }

    private static EditTextPreference createHostField(Context context) {
        EditTextPreference p = new SummaryEditTextPreference(context);
        p.setKey("i2pbote.i2cp.tcp.host");
        p.setTitle(R.string.pref_title_i2cp_host);
        p.setSummary("%s");
        p.setDefaultValue("internal");
        p.setEnabled(false);
        return p;
    }

    private static EditTextPreference createPortField(Context context) {
        EditTextPreference p = new SummaryEditTextPreference(context);
        p.setKey("i2pbote.i2cp.tcp.port");
        p.setTitle(R.string.pref_title_i2cp_port);
        p.setSummary("%s");
        p.setDefaultValue("internal");
        p.setEnabled(false);
        return p;
    }


    //
    // Update list of identities in headers
    //

    /**
     * Starts the async reload of the identities list
     * (if the headers are being displayed)
     */
    private void updateIdentities() {
        if (shouldUpdateIdentities()) {
            mLoadIdentityListTask = (LoadIdentityListTask)
                    new LoadIdentityListTask().execute();
        }
    }

    private boolean shouldUpdateIdentities() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            return getIntent().getAction() == null;
        else
            return showingHeaders();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean showingHeaders() {
        return hasHeaders();
    }


    //
    // Load list of identities and convert
    // into appropriate type of header
    //

    private class LoadIdentityListTask extends AsyncTask<String, Void, Object[]> {
        protected Object[] doInBackground(String... params) {
            try {
                Collection<EmailIdentity> identities = I2PBote.getInstance().getIdentities().getAll();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    // Return legacy headers
                    return loadLegacyHeaders(identities);
                } else {
                    // Return list of Headers
                    return loadHeaders(identities);
                }
            } catch (PasswordException e) {
                cancel(false);
                return new Object[] {e};
            } catch (IOException e) {
                cancel(false);
                return new Object[] {e};
            } catch (GeneralSecurityException e) {
                cancel(false);
                return new Object[] {e};
            }
        }

        private Object[] loadLegacyHeaders(Collection<EmailIdentity> identities) {
            // TODO: implement
            return new Object[] {null};
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private Object[] loadHeaders(Collection<EmailIdentity> identities) {
            Header[] result = new Header[identities.size()];
            int index = 0;

            for (EmailIdentity identity : identities) {
                final long id = identity.getHash().hashCode();
                final String name = identity.getPublicName();
                final String desc = identity.getDescription();
                final String key = identity.getKey();
                final Intent intent = new Intent(
                        getApplicationContext(), ViewIdentityActivity.class);
                final Bundle args = new Bundle();
                args.putString(ViewIdentityFragment.IDENTITY_KEY, key);
                intent.putExtras(args);
                final Header newHeader = new Header();
                newHeader.id = id;
                newHeader.title = name;
                newHeader.summary = desc;
                newHeader.intent = intent;
                newHeader.extras = args;
                result[index++] = newHeader;
            }

            return new Object[] {result};
        }

        @Override
        protected void onPostExecute(Object[] result) {
            if (isCancelled()) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                showLegacyHeaders(result);
            } else {
                showHeaders(result);
            }
        }

        private void showLegacyHeaders(Object[] result) {
            // TODO: implement
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private void showHeaders(Object[] result) {
            final Header[] headers = (Header[]) result[0];
            mIdentityListHeaders = headers;
            invalidateHeaders();
        }

        @Override
        protected void onCancelled(Object[] result) {
        }
    }


    //
    // Styling for headers
    //

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            super.setListAdapter(adapter); // TODO: implement legacy headers styling
        } else {
            // TODO: Fix NPE when rotating screen
            super.setListAdapter(new HeaderAdapter(this, adapter));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_NORMAL + 1;

        private static class HeaderViewHolder {
            TextView title;
            TextView summary;
        }

        private ListAdapter mAdapter;
        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            if (header.fragment == null && header.intent == null) {
                return HEADER_TYPE_CATEGORY;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            try {
                return getItemViewType(position) != HEADER_TYPE_CATEGORY;
            } catch (IndexOutOfBoundsException e) {
                // Happens when deleting an identity
                return false;
            }
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, ListAdapter adapter) {
            super(context, 0);
            mAdapter = adapter;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Header getItem(int position) {
            return (Header) mAdapter.getItem(position);
        }

        @Override
        public int getCount() {
            return mAdapter.getCount();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();
                switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    view = new TextView(getContext(), null,
                            android.R.attr.listSeparatorTextViewStyle);
                    holder.title = (TextView) view;
                    break;

                case HEADER_TYPE_NORMAL:
                    view = mInflater.inflate(
                            R.layout.preference_header_item, parent,
                            false);
                    holder.title = (TextView)
                            view.findViewById(android.R.id.title);
                    holder.summary = (TextView)
                            view.findViewById(android.R.id.summary);
                    break;
                }
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
            case HEADER_TYPE_CATEGORY:
                holder.title.setText(header.getTitle(getContext().getResources()));
                break;

            case HEADER_TYPE_NORMAL:
                updateCommonHeaderView(header, holder);
                break;
            }

            return view;
        }

        private void updateCommonHeaderView(Header header, HeaderViewHolder holder) {
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (summary != null && !summary.toString().isEmpty()) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }
    }
}
