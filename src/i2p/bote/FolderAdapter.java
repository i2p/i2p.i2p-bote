package i2p.bote;

import java.util.List;

import i2p.bote.folder.EmailFolder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FolderAdapter extends ArrayAdapter<EmailFolder> {
    private final LayoutInflater mInflater;

    public FolderAdapter(Context context) {
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
        name.setText(BoteHelper.getFolderDisplayName(getContext(), folder, true));

        return v;
    }
}
