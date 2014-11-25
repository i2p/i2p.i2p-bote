package i2p.bote.android.config;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;

import i2p.bote.android.Constants;
import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.QrCodeUtils;
import i2p.bote.email.EmailIdentity;
import i2p.bote.fileencryption.PasswordException;

public class ViewIdentityFragment extends Fragment {
    public static final String IDENTITY_KEY = "identity_key";

    private String mKey;
    private EmailIdentity mIdentity;

    Toolbar mToolbar;
    ImageView mIdentityPicture;
    TextView mNameField;
    TextView mDescField;
    TextView mFingerprintField;
    TextView mCryptoField;
    TextView mKeyField;
    ImageView mKeyQrCode;
    ImageView mExpandedQrCode;

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mQrCodeAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration;

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

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_identity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        mIdentityPicture = (ImageView) view.findViewById(R.id.identity_picture);
        mNameField = (TextView) view.findViewById(R.id.public_name);
        mDescField = (TextView) view.findViewById(R.id.description);
        mFingerprintField = (TextView) view.findViewById(R.id.fingerprint);
        mCryptoField = (TextView) view.findViewById(R.id.crypto_impl);
        mKeyField = (TextView) view.findViewById(R.id.key);
        mKeyQrCode = (ImageView) view.findViewById(R.id.key_qr_code);
        mExpandedQrCode = (ImageView) view.findViewById(R.id.expanded_qr_code);

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBarActivity activity = ((ActionBarActivity) getActivity());

        // Set the action bar
        activity.setSupportActionBar(mToolbar);

        // Enable ActionBar app icon to behave as action to go back
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIdentity != null) {
            Bitmap picture = BoteHelper.decodePicture(mIdentity.getPictureBase64());
            if (picture != null)
                mIdentityPicture.setImageBitmap(picture);
            else {
                ViewGroup.LayoutParams lp = mIdentityPicture.getLayoutParams();
                mIdentityPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(mKey, lp.width, lp.height));
            }

            mNameField.setText(mIdentity.getPublicName());
            if (mIdentity.getDescription().isEmpty())
                mDescField.setVisibility(View.GONE);
            else {
                mDescField.setText(mIdentity.getDescription());
                mDescField.setVisibility(View.VISIBLE);
            }
            try {
                String locale = getActivity().getResources().getConfiguration().locale.getLanguage();
                mFingerprintField.setText(BoteHelper.getFingerprint(mIdentity, locale));
            } catch (GeneralSecurityException e) {
                // Could not get fingerprint
                mFingerprintField.setText(e.getLocalizedMessage());
            }
            mCryptoField.setText(mIdentity.getCryptoImpl().getName());
            mKeyField.setText(mKey);

            mKeyQrCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    zoomQrCode();
                }
            });

            loadQrCode();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_identity, menu);
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

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode() {
        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        String qrCodeContent = Constants.EMAILDEST_SCHEME + ":" + mKey;
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(qrCodeContent, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        // only change view, if fragment is attached to activity
                        if (ViewIdentityFragment.this.isAdded()) {
                            // scale the image up to our actual size. we do this in code rather
                            // than let the ImageView do this because we don't require filtering.
                            Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                    mKeyQrCode.getHeight(), mKeyQrCode.getHeight(),
                                    false);
                            mKeyQrCode.setImageBitmap(scaled);
                            // scale for the expanded image
                            int smallestDimen = Math.min(mExpandedQrCode.getWidth(), mExpandedQrCode.getHeight());
                            scaled = Bitmap.createScaledBitmap(qrCode,
                                    smallestDimen, smallestDimen,
                                    false);
                            mExpandedQrCode.setImageBitmap(scaled);
                            // simple fade-in animation
                            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                            anim.setDuration(200);
                            mKeyQrCode.startAnimation(anim);
                        }
                    }
                };
        loadTask.execute();
    }

    private void zoomQrCode() {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mQrCodeAnimator != null) {
            mQrCodeAnimator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        mKeyQrCode.getGlobalVisibleRect(startBounds);
        getActivity().findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        ViewHelper.setAlpha(mKeyQrCode, 0f);
        mExpandedQrCode.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        ViewHelper.setPivotX(mExpandedQrCode, 0f);
        ViewHelper.setPivotY(mExpandedQrCode, 0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mExpandedQrCode, "x",
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedQrCode, "y",
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(mExpandedQrCode, "scaleX",
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(mExpandedQrCode, "scaleY",
                        startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mQrCodeAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mQrCodeAnimator = null;
            }
        });
        set.start();
        mQrCodeAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        mExpandedQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mQrCodeAnimator != null) {
                    mQrCodeAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(mExpandedQrCode, "x", startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(mExpandedQrCode,
                                        "y", startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(mExpandedQrCode,
                                        "scaleX", startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(mExpandedQrCode,
                                        "scaleY", startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewHelper.setAlpha(mKeyQrCode, 1f);
                        mExpandedQrCode.setVisibility(View.GONE);
                        mQrCodeAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        ViewHelper.setAlpha(mKeyQrCode, 1f);
                        mExpandedQrCode.setVisibility(View.GONE);
                        mQrCodeAnimator = null;
                    }
                });
                set.start();
                mQrCodeAnimator = set;
            }
        });
    }
}
