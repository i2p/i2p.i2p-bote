package i2p.bote.android;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.htmltextview.HtmlTextView;


public class HelpAboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help_about, container, false);

        TextView versionText = (TextView) view.findViewById(R.id.help_about_version);
        versionText.setText(getString(R.string.version) + " " + getVersion());

        TextView licenseText = (TextView) view.findViewById(R.id.help_about_license);
        licenseText.setText(getString(R.string.license, "GPLv3+"));

        HtmlTextView aboutLibsView = (HtmlTextView) view.findViewById(R.id.help_about_libraries);

        // load html from raw resource (Parsing handled by HtmlTextView library)
        aboutLibsView.setHtml(R.raw.help_about_libraries);

        // no flickering when clicking textview for Android < 4
        aboutLibsView.setTextColor(getResources().getColor(android.R.color.black));

        return view;
    }

    /**
     * Get the current package version.
     *
     * @return The current version.
     */
    private String getVersion() {
        String result = "";
        try {
            PackageManager manager = getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);

            result = String.format("%s (%s)", info.versionName, info.versionCode);
        } catch (NameNotFoundException e) {
            Log.w(Constants.ANDROID_LOG_TAG, "Unable to get application version: " + e.getMessage());
            result = "Unable to get application version.";
        }

        return result;
    }
}
