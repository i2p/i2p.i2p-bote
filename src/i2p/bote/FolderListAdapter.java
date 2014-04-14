package i2p.bote;

import java.util.List;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;
import i2p.bote.util.BoteHelper;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FolderListAdapter extends ArrayAdapter<EmailFolder> implements FolderListener {
    private final LayoutInflater mInflater;

    public FolderListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<EmailFolder> folders) {
        // Remove previous FolderListeners
        for (int i = 0; i < getCount(); i++) {
            getItem(i).removeFolderListener(this);
        }
        clear();
        if (folders != null) {
            for (EmailFolder folder : folders) {
                add(folder);
                folder.addFolderListener(this);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.listitem_folder, parent, false);
        EmailFolder folder = getItem(position);

        TextView name = (TextView) v.findViewById(R.id.folder_name);
        try {
            name.setText(BoteHelper.getFolderDisplayNameWithNew(getContext(), folder));
        } catch (PasswordException e) {
            // Password fetching is handled in EmailListFragment
            name.setText(BoteHelper.getFolderDisplayName(getContext(), folder));
        }

        return v;
    }

    // FolderListener

    @Override
    public void elementAdded() {
        notifyDataSetChanged();
    }

    @Override
    public void elementRemoved() {
        notifyDataSetChanged();
    }
}
