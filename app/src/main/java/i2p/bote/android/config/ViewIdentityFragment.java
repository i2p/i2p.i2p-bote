package i2p.bote.android.config;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.io.IOException;
import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;

public class ViewIdentityFragment extends Fragment {
    public static final String IDENTITY_KEY = "identity_key";

    private String mKey;
    private EmailIdentity mIdentity;

    ImageView mIdentityPicture;
    TextView mNameField;
    TextView mDescField;
    TextView mFingerprintField;
    TextView mCryptoField;
    TextView mKeyField;

    Button mGenQRCode;

    public static ViewIdentityFragment newInstance(String key) {
        ViewIdentityFragment f = new ViewIdentityFragment();
        Bundle args = new Bundle();
        args.putString(IDENTITY_KEY, key);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mKey = getArguments().getString(IDENTITY_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_identity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mIdentityPicture = (ImageView) view.findViewById(R.id.identity_picture);
        mNameField = (TextView) view.findViewById(R.id.public_name);
        mDescField = (TextView) view.findViewById(R.id.description);
        mFingerprintField = (TextView) view.findViewById(R.id.fingerprint);
        mCryptoField = (TextView) view.findViewById(R.id.crypto_impl);
        mKeyField = (TextView) view.findViewById(R.id.key);
        mGenQRCode = (Button) view.findViewById(R.id.generate_qr);

        if (mKey != null) {
            try {
                mIdentity = BoteHelper.getIdentity(mKey);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIdentity != null) {
            Bitmap picture = BoteHelper.decodePicture(mIdentity.getPictureBase64());
            if (picture != null)
                mIdentityPicture.setImageBitmap(picture);

            mNameField.setText(mIdentity.getPublicName());
            mDescField.setText(mIdentity.getDescription());
            try {
                String locale = getActivity().getResources().getConfiguration().locale.getLanguage();
                mFingerprintField.setText(BoteHelper.getFingerprint(mIdentity, locale));
            } catch (GeneralSecurityException e) {
                // Could not get fingerprint
                mFingerprintField.setText(e.getLocalizedMessage());
            }
            mCryptoField.setText(mIdentity.getCryptoImpl().getName());
            mKeyField.setText(mKey);

            mGenQRCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IntentIntegrator i = new IntentIntegrator(getActivity());
                    i.shareText("bote:" + mKey);
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_identity, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mKey != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mKey);
            shareIntent.setType("text/plain");
            shareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_edit_identity:
            Intent ei = new Intent(getActivity(), EditIdentityActivity.class);
            ei.putExtra(EditIdentityFragment.IDENTITY_KEY, mKey);
            startActivity(ei);
            return true;

        case R.id.action_delete_identity:
            DialogFragment df = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.delete_identity)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            try {
                                BoteHelper.deleteIdentity(mKey);
                                getActivity().setResult(Activity.RESULT_OK);
                                getActivity().finish();
                            } catch (PasswordException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (GeneralSecurityException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    return builder.create();
                }
            };
            df.show(getActivity().getSupportFragmentManager(), "deletecontact");
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public NdefMessage createNdefMessage() {
        NdefMessage msg = new NdefMessage(new NdefRecord[]{
                createNameRecord(),
                createDestinationRecord()
        });
        return msg;
    }

    private NdefRecord createNameRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contact".getBytes(),
                    new byte[0],
                    mIdentity.getPublicName().getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contact", mIdentity.getPublicName().getBytes()
            );
    }

    private NdefRecord createDestinationRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contactDestination".getBytes(),
                    new byte[0],
                    mIdentity.getKey().getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contactDestination", mIdentity.getKey().getBytes()
            );
    }
}
