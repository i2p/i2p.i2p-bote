package i2p.bote.android.util;

import java.io.File;
import java.util.List;

import i2p.bote.android.R;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class EditPictureFragment extends Fragment {
    static final int REQUEST_PICTURE_FILE = 1;
    static final int CROP_PICTURE = 2;

    Uri mPictureCaptureUri;
    Bitmap mPicture;
    ImageView mPictureView; 

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPictureView = (ImageView) view.findViewById(R.id.picture);

        // Set up listener for picture changing
        mPictureView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(i, getResources().getString(R.string.select_a_picture)),
                        REQUEST_PICTURE_FILE);
            }
        });
    }

    protected void setPictureB64(String pic) {
        mPicture = BoteHelper.decodePicture(pic);
        if (mPicture != null)
            mPictureView.setImageBitmap(mPicture);
    }

    protected String getPictureB64() {
        return BoteHelper.encodePicture(mPicture);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                System.out.println("Cancelled");
                if (mPictureCaptureUri != null ) {
                    getActivity().getContentResolver().delete(mPictureCaptureUri, null, null);
                    mPictureCaptureUri = null;
                }
            }
            return;
        }

        switch (requestCode) {
        case REQUEST_PICTURE_FILE:
            mPictureCaptureUri = data.getData();
            cropPicture();
            break;

        case CROP_PICTURE:
            Bundle extras = data.getExtras();
            if (extras != null) {
                mPicture = extras.getParcelable("data");
                mPictureView.setImageBitmap(mPicture);
            }
            File f = new File(mPictureCaptureUri.getPath());
            if (f.exists())
                f.delete();
            break;

        }
    }

    private void cropPicture() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        if (list.size() == 0) {
            Toast.makeText(getActivity(), R.string.no_image_cropping_app_found, Toast.LENGTH_SHORT)
                    .show();
        } else {
            intent.setData(mPictureCaptureUri);
            intent.putExtra("outputX", 72);
            intent.putExtra("outputY", 72);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            startActivityForResult(
                    Intent.createChooser(intent,
                            getResources().getString(R.string.select_a_cropping_app)),
                    CROP_PICTURE);
        }
    }
}
