package i2p.bote.android.identities;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.EmailIdentity;

public class IdentityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mCtx;
    private List<EmailIdentity> mIdentities;
    private IdentityListFragment.IdentityListListener mListener;

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class IdentityViewHolder extends RecyclerView.ViewHolder {
        public ImageView mPicture;
        public TextView mName;

        public IdentityViewHolder(View itemView) {
            super(itemView);
            mPicture = (ImageView) itemView.findViewById(R.id.identity_picture);
            mName = (TextView) itemView.findViewById(R.id.identity_name);
        }
    }

    public IdentityAdapter(Context context, IdentityListFragment.IdentityListListener listener) {
        mCtx = context;
        mListener = listener;
        setHasStableIds(true);
    }

    public void setIdentities(Collection<EmailIdentity> identities) {
        if (identities != null) {
            mIdentities = new ArrayList<>();
            mIdentities.addAll(identities);
        } else
            mIdentities = null;

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return R.layout.listitem_text;

        return R.layout.listitem_identity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        switch (viewType) {
            case R.layout.listitem_identity:
                return new IdentityViewHolder(v);
            default:
                return new SimpleViewHolder(v);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case R.layout.listitem_text:
                ((TextView) holder.itemView).setText(
                        mCtx.getResources().getString(R.string.new_identity));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onNewIdentity();
                    }
                });
                break;

            case R.layout.listitem_identity:
                final IdentityViewHolder cvh = (IdentityViewHolder) holder;
                EmailIdentity identity = mIdentities.get(position - 1);

                String pic = identity.getPictureBase64();
                if (pic != null && !pic.isEmpty())
                    cvh.mPicture.setImageBitmap(BoteHelper.decodePicture(pic));
                else {
                    ViewGroup.LayoutParams lp = cvh.mPicture.getLayoutParams();
                    cvh.mPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(identity.getKey(), lp.width, lp.height));
                }

                cvh.mName.setText(identity.getPublicName());

                cvh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onIdentitySelected(mIdentities.get(cvh.getAdapterPosition() - 1));
                    }
                });
                break;

            default:
                break;
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mIdentities.size() + 1;
    }

    public long getItemId(int position) {
        if (position == 0)
            return 0;

        EmailIdentity identity = mIdentities.get(position - 1);
        return identity.getHash().hashCode();
    }
}
