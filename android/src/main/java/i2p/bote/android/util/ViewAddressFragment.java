package i2p.bote.android.util;

import android.app.Activity;
import android.content.Context;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
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
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import i2p.bote.android.Constants;
import i2p.bote.android.R;

public abstract class ViewAddressFragment extends Fragment implements
        DeleteAddressDialogFragment.DeleteAddressDialogListener {
    public static final String ADDRESS = "address";

    protected String mAddress;

    Toolbar mToolbar;
    protected ImageView mPicture;
    protected TextView mPublicName;
    protected TextView mDescription;
    protected TextView mCryptoImplName;
    TextView mAddressField;
    ImageView mAddressQrCode;
    TextView mFingerprint;
    ImageView mExpandedQrCode;

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mQrCodeAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAddress = getArguments().getString(ADDRESS);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_address, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        mPicture = (ImageView) view.findViewById(R.id.picture);
        mPublicName = (TextView) view.findViewById(R.id.public_name);
        mDescription = (TextView) view.findViewById(R.id.description);
        mFingerprint = (TextView) view.findViewById(R.id.fingerprint);
        mCryptoImplName = (TextView) view.findViewById(R.id.crypto_impl_name);
        mAddressField = (TextView) view.findViewById(R.id.email_dest);
        mAddressQrCode = (ImageView) view.findViewById(R.id.email_dest_qr_code);
        mExpandedQrCode = (ImageView) view.findViewById(R.id.expanded_qr_code);

        view.findViewById(R.id.copy_key).setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View view) {
                Object clipboardService = getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) clipboardService;
                    clipboard.setText(mAddress);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) clipboardService;
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            getString(R.string.bote_dest_for, getPublicName()), mAddress);
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        });

        if (mAddress != null) {
            loadAddress();
        } else {
            // No address provided, finish
            // Should not happen
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

    protected abstract void loadAddress();

    protected abstract String getPublicName();

    protected abstract int getDeleteAddressMessage();

    protected abstract void onEditAddress();

    public abstract void onDeleteAddress();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity activity = ((AppCompatActivity) getActivity());

        // Set the action bar
        activity.setSupportActionBar(mToolbar);

        // Enable ActionBar app icon to behave as action to go back
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        mAddressField.setText(mAddress);

        mAddressQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomQrCode();
            }
        });

        loadQrCode();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_address, menu);
        menu.findItem(R.id.action_edit_address).setIcon(BoteHelper.getMenuIcon(getActivity(), GoogleMaterial.Icon.gmd_create));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_address:
                onEditAddress();
                return true;

            case R.id.action_delete_address:
                DialogFragment df = DeleteAddressDialogFragment.newInstance(getDeleteAddressMessage());
                df.show(getChildFragmentManager(), "deleteaddress");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public NdefMessage createNdefMessage() {
        return new NdefMessage(new NdefRecord[]{
                createNameRecord(),
                createDestinationRecord()
        });
    }

    private NdefRecord createNameRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contact".getBytes(),
                    new byte[0],
                    getPublicName().getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contact", getPublicName().getBytes()
            );
    }

    private NdefRecord createDestinationRecord() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return new NdefRecord(
                    NdefRecord.TNF_EXTERNAL_TYPE,
                    "i2p.bote:contactDestination".getBytes(),
                    new byte[0],
                    mAddress.getBytes()
            );
        else
            return NdefRecord.createExternal(
                    "i2p.bote", "contactDestination", mAddress.getBytes()
            );
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode() {
        AsyncTask<Void, Void, Bitmap[]> loadTask =
                new AsyncTask<Void, Void, Bitmap[]>() {
                    protected Bitmap[] doInBackground(Void... unused) {
                        String qrCodeContent = Constants.EMAILDEST_SCHEME + ":" + mAddress;
                        // render with minimal size
                        Bitmap qrCode = QrCodeUtils.getQRCodeBitmap(qrCodeContent, 0);
                        Bitmap[] scaled = new Bitmap[2];

                        // scale the image up to our actual size. we do this in code rather
                        // than let the ImageView do this because we don't require filtering.
                        int size = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
                        scaled[0] = Bitmap.createScaledBitmap(qrCode, size, size, false);

                        // scale for the expanded image
                        DisplayMetrics dm = new DisplayMetrics();
                        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                        int smallestDimen = Math.min(dm.widthPixels, dm.heightPixels);
                        scaled[1] = Bitmap.createScaledBitmap(qrCode,
                                smallestDimen, smallestDimen,
                                false);

                        return scaled;
                    }

                    protected void onPostExecute(Bitmap[] scaled) {
                        // only change view, if fragment is attached to activity
                        if (ViewAddressFragment.this.isAdded()) {
                            mAddressQrCode.setImageBitmap(scaled[0]);
                            mExpandedQrCode.setImageBitmap(scaled[1]);
                            // simple fade-in animation
                            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                            anim.setDuration(200);
                            mAddressQrCode.startAnimation(anim);
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
        mAddressQrCode.getGlobalVisibleRect(startBounds);
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
        ViewHelper.setAlpha(mAddressQrCode, 0f);
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
                        ViewHelper.setAlpha(mAddressQrCode, 1f);
                        mExpandedQrCode.setVisibility(View.GONE);
                        mExpandedQrCode.setClickable(false);
                        mQrCodeAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        ViewHelper.setAlpha(mAddressQrCode, 1f);
                        mExpandedQrCode.setVisibility(View.GONE);
                        mExpandedQrCode.setClickable(false);
                        mQrCodeAnimator = null;
                    }
                });
                set.start();
                mQrCodeAnimator = set;
            }
        });
    }
}
