package i2p.bote.android.identities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.security.GeneralSecurityException;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.ViewAddressFragment;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;

public class ViewIdentityFragment extends ViewAddressFragment {
    private EmailIdentity mIdentity;

    public static ViewIdentityFragment newInstance(String key) {
        ViewIdentityFragment f = new ViewIdentityFragment();
        Bundle args = new Bundle();
        args.putString(ADDRESS, key);
        f.setArguments(args);
        return f;
    }

    @Override
    protected void loadAddress() {
        try {
            mIdentity = BoteHelper.getIdentity(mAddress);
            if (mIdentity == null) {
                // No identity found, finish
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
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
    protected String getPublicName() {
        return mIdentity.getPublicName();
    }

    @Override
    protected int getDeleteAddressMessage() {
        return R.string.delete_identity;
    }

    @Override
    public void onResume() {
        super.onResume();

        Bitmap picture = BoteHelper.decodePicture(mIdentity.getPictureBase64());
        if (picture != null)
            mPicture.setImageBitmap(picture);
        else {
            ViewGroup.LayoutParams lp = mPicture.getLayoutParams();
            mPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(mAddress, lp.width, lp.height));
        }

        mPublicName.setText(mIdentity.getPublicName());
        if (mIdentity.getDescription().isEmpty())
            mDescription.setVisibility(View.GONE);
        else {
            mDescription.setText(mIdentity.getDescription());
            mDescription.setVisibility(View.VISIBLE);
        }
        mCryptoImplName.setText(mIdentity.getCryptoImpl().getName());
    }

    @Override
    protected void onEditAddress() {
        Intent ei = new Intent(getActivity(), EditIdentityActivity.class);
        ei.putExtra(EditIdentityFragment.IDENTITY_KEY, mAddress);
        startActivity(ei);
    }

    @Override
    public void onDeleteAddress() {
        try {
            BoteHelper.deleteIdentity(mAddress);
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
}
