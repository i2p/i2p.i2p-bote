package i2p.bote.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.ViewHolder> {
    private Context mCtx;
    private List<EmailFolder> mFolders;
    private int mSelectedFolder;
    private OnFolderSelectedListener mListener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mIcon;
        public TextView mName;

        public ViewHolder(View v) {
            super(v);
            mIcon = (ImageView) v.findViewById(R.id.folder_icon);
            mName = (TextView) v.findViewById(R.id.folder_name);
        }
    }

    public static interface OnFolderSelectedListener {
        /**
         * Called when a folder has been selected from the navigation drawer.
         *
         * @param folder
         * The EmailFolder that has just been selected.
         * @param alreadySelected
         * Is the selected folder already selected?
         */
        public void onDrawerFolderSelected(EmailFolder folder, boolean alreadySelected);
    }

    public FolderListAdapter(Context ctx, OnFolderSelectedListener listener) {
        mCtx = ctx;
        mSelectedFolder = -1;
        mListener = listener;
    }

    public void setFolders(List<EmailFolder> folders, FolderListener folderListener) {
        // Remove previous FolderListeners
        if (mFolders != null) {
            for (EmailFolder folder : mFolders) {
                folder.removeFolderListener(folderListener);
            }
        }

        mFolders = folders;
        for (EmailFolder folder : folders) {
            folder.addFolderListener(folderListener);
        }

        if (mSelectedFolder < 0)
            mSelectedFolder = 0;

        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        if (position != mSelectedFolder) {
            int oldSelected = mSelectedFolder;
            mSelectedFolder = position;
            notifyItemChanged(oldSelected);
            notifyItemChanged(mSelectedFolder);
        }
    }

    public int getSelected() {
        return mSelectedFolder;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FolderListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_folder_with_icon, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final EmailFolder folder = mFolders.get(position);
        final boolean isSelected = position == mSelectedFolder;

        Drawable folderIcon = DrawableCompat.wrap(BoteHelper.getFolderIcon(mCtx, folder));
        DrawableCompat.setTintList(folderIcon, mCtx.getResources().getColorStateList(R.color.folder_icon));
        holder.mIcon.setImageDrawable(folderIcon);

        try {
            holder.mName.setText(BoteHelper.getFolderDisplayNameWithNew(mCtx, folder));
        } catch (PasswordException e) {
            // Password fetching is handled in EmailListFragment
            holder.mName.setText(BoteHelper.getFolderDisplayName(mCtx, folder));
        }

        holder.itemView.setSelected(isSelected);

        holder.mName.setTextAppearance(mCtx, isSelected ?
                R.style.TextAppearance_AppCompat_NavDrawer_Selected :
                R.style.TextAppearance_AppCompat_NavDrawer);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                setSelected(position);
                mListener.onDrawerFolderSelected(folder, isSelected);
            }
        });
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mFolders != null)
            return mFolders.size();
        return 0;
    }
}
