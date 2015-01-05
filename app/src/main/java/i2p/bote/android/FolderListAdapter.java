package i2p.bote.android;

import java.util.List;

import i2p.bote.android.util.BoteHelper;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FolderListAdapter extends ArrayAdapter<EmailFolder> {
    private final LayoutInflater mInflater;

    public FolderListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<EmailFolder> folders, FolderListener folderListener) {
        // Remove previous FolderListeners
        for (int i = 0; i < getCount(); i++) {
            getItem(i).removeFolderListener(folderListener);
        }
        clear();
        if (folders != null) {
            for (EmailFolder folder : folders) {
                add(folder);
                folder.addFolderListener(folderListener);
            }
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view;

        if (convertView == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.listitem_folder_with_icon, parent, false);
            holder.icon = (ImageView) view.findViewById(R.id.folder_icon);
            holder.name = (TextView) view.findViewById(R.id.folder_name);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        EmailFolder folder = getItem(position);

        holder.icon.setImageDrawable(BoteHelper.getFolderIcon(getContext(), folder));
        try {
            holder.name.setText(BoteHelper.getFolderDisplayNameWithNew(getContext(), folder));
        } catch (PasswordException e) {
            // Password fetching is handled in EmailListFragment
            holder.name.setText(BoteHelper.getFolderDisplayName(getContext(), folder));
        }

        return view;
    }
}
