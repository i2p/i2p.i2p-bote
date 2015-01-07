package i2p.bote.android.addressbook;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.packet.dht.Contact;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    private List<Contact> mContacts;
    private AddressBookFragment.OnContactSelectedListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mPicture;
        public TextView mName;

        public ViewHolder(View itemView) {
            super(itemView);
            mPicture = (ImageView) itemView.findViewById(R.id.contact_picture);
            mName = (TextView) itemView.findViewById(R.id.contact_name);
        }
    }

    public ContactAdapter(AddressBookFragment.OnContactSelectedListener listener) {
        mListener = listener;
    }

    public void setContacts(SortedSet<Contact> contacts) {
        if (contacts != null) {
            mContacts = new ArrayList<Contact>();
            mContacts.addAll(contacts);
        } else
            mContacts = null;

        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ContactAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_contact, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Contact contact = mContacts.get(position);

        String pic = contact.getPictureBase64();
        if (pic != null && !pic.isEmpty())
            holder.mPicture.setImageBitmap(BoteHelper.decodePicture(pic));
        else {
            ViewGroup.LayoutParams lp = holder.mPicture.getLayoutParams();
            holder.mPicture.setImageBitmap(BoteHelper.getIdenticonForAddress(contact.getBase64Dest(), lp.width, lp.height));
        }

        holder.mName.setText(contact.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onContactSelected(mContacts.get(holder.getPosition()));
            }
        });
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mContacts != null)
            return mContacts.size();
        return 0;
    }
}
