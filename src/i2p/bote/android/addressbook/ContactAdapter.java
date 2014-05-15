package i2p.bote.android.addressbook;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.packet.dht.Contact;

import java.util.SortedSet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private final LayoutInflater mInflater;

    public ContactAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(SortedSet<Contact> contacts) {
        clear();
        if (contacts != null) {
            for (Contact contact : contacts) {
                add(contact);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.listitem_contact, parent, false);
        Contact contact = getItem(position);

        ImageView picture = (ImageView) v.findViewById(R.id.contact_picture);
        TextView name = (TextView) v.findViewById(R.id.contact_name);

        String pic = contact.getPictureBase64();
        if (pic != null && !pic.isEmpty())
            picture.setImageBitmap(BoteHelper.decodePicture(pic));

        name.setText(contact.getName());

        return v;
    }
}
