package i2p.bote;

import java.security.GeneralSecurityException;
import java.util.List;

import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FolderListAdapter extends ArrayAdapter<EmailFolder> {
    private final LayoutInflater mInflater;

    public FolderListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<EmailFolder> folders) {
        clear();
        if (folders != null) {
            for (EmailFolder folder : folders) {
                add(folder);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.listitem_folder, parent, false);
        EmailFolder folder = getItem(position);

        TextView name = (TextView) v.findViewById(R.id.folder_name);
        // TODO: This needs to be updated when emails change.
        try {
            name.setText(BoteHelper.getFolderDisplayName(getContext(), folder, true));
        } catch (PasswordException e) {
            // TODO: Get password from user and retry
            name.setText("ERROR: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            // TODO: Handle properly
            name.setText("ERROR: " + e.getMessage());
        }

        return v;
    }
}
