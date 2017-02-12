package i2p.bote.android.addressbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.ViewAddressFragment;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.dht.Contact;

public class ViewContactFragment extends ViewAddressFragment {
    private Contact mContact;

    public static ViewContactFragment newInstance(String destination) {
        ViewContactFragment f = new ViewContactFragment();
        Bundle args = new Bundle();
        args.putString(ADDRESS, destination);
        f.setArguments(args);
        return f;
    }

    @Override
    protected void loadAddress() {
        try {
            mContact = BoteHelper.getContact(mAddress);
            if (mContact == null) {
                // No contact found, finish
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
        } catch (PasswordException e) {
            // TODO Handle
            e.printStackTrace();
        }
    }

    @Override
    protected String getPublicName() {
        return mContact.getName();
    }

    @Override
    protected int getDeleteAddressMessage() {
        return R.string.delete_contact;
    }

    @Override
    public void onResume() {
        super.onResume();

        Bitmap picture = BoteHelper.decodePicture(mContact.getPictureBase64());
        if (picture != null)
            mPicture.setImageBitmap(picture);
        else {
            ViewGroup.LayoutParams lp = mPicture.getLayoutParams();
            mPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(mAddress, lp.width, lp.height));
        }

        mPublicName.setText(mContact.getName());
        if (mContact.getText().isEmpty())
            mDescription.setVisibility(View.GONE);
        else {
            mDescription.setText(mContact.getText());
            mDescription.setVisibility(View.VISIBLE);
        }
        mCryptoImplName.setText(mContact.getDestination().getCryptoImpl().getName());
    }

    @Override
    protected void onEditAddress() {
        Intent ei = new Intent(getActivity(), EditContactActivity.class);
        ei.putExtra(EditContactFragment.CONTACT_DESTINATION, mAddress);
        startActivity(ei);
    }

    @Override
    public void onDeleteAddress() {
        try {
            String err = BoteHelper.deleteContact(mAddress);
            if (err == null) {
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            } else
                Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
        } catch (PasswordException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
