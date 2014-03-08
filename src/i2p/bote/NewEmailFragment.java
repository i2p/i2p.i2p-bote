package i2p.bote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class NewEmailFragment extends Fragment {
    Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_email, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSpinner = (Spinner) view.findViewById(R.id.sender_spinner);
        IdentityAdapter adapter = new IdentityAdapter(getActivity());
        mSpinner.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_email, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_send_email:
            // TODO Handle
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private class IdentityAdapter extends ArrayAdapter<EmailIdentity> {
        public IdentityAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            try {
                Collection<EmailIdentity> identities = I2PBote.getInstance().getIdentities().getAll();
                for (EmailIdentity identity : identities) {
                    add(identity);
                    if (identity.isDefault())
                        mSpinner.setSelection(getPosition(identity));
                }
            } catch (PasswordException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Handle
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                // TODO Handle
                e.printStackTrace();
            }
        }

        @Override
        public EmailIdentity getItem(int position) {
            if (position > 0)
                return super.getItem(position - 1);
            else
                return null;
        }

        @Override
        public int getPosition(EmailIdentity item) {
            if (item != null)
                return super.getPosition(item) + 1;
            else
                return 0;
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            setViewText(v, position);
            return v;
        }

        @Override
        public View getDropDownView (int position, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);
            setViewText(v, position);
            return v;
        }

        private void setViewText(View v, int position) {
            TextView text = (TextView) v.findViewById(android.R.id.text1);
            EmailIdentity identity = getItem(position);
            if (identity == null)
                text.setText("Anonymous");
            else
                text.setText(identity.getPublicName());
        }
    }
}
