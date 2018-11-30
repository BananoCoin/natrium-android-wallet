package co.banano.natriumwallet.ui.contact;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.ContactSelected;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.ViewHolderContactSmBinding;
import co.banano.natriumwallet.model.Contact;

import java.util.List;

public class ContactSelectionAdapter extends RecyclerView.Adapter<ContactSelectionAdapter.ViewHolder> {
    private List<Contact> contactList;

    public ContactSelectionAdapter(List<Contact> flsLst) {
        contactList = flsLst;
    }

    @Override
    public ContactSelectionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        ViewHolderContactSmBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.view_holder_contact_sm, parent, false);

        ViewHolder viewHolder = new ViewHolder(binding);
        View view = binding.getRoot();
        view.setOnClickListener((View v) -> {
            Contact c = (Contact) view.getTag();
            if (c != null) {
                RxBus.get().post(new ContactSelected(c.getName(), c.getAddress()));
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.contactItemBinding.setContact(contact);
        if (position == (getItemCount() - 1)) {
            holder.contactItemBinding.contactDivider.setVisibility(View.INVISIBLE);
        } else {
            holder.contactItemBinding.contactDivider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void updateList(List<Contact> newList) {
        List<Contact> oldList = this.contactList;
        this.contactList = newList;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ContactSelectionDiffCallback(oldList, newList), false);
        diffResult.dispatchUpdatesTo(this);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolderContactSmBinding contactItemBinding;

        public ViewHolder(ViewHolderContactSmBinding contactSmItemLayoutBinding) {
            super(contactSmItemLayoutBinding.getRoot());
            contactItemBinding = contactSmItemLayoutBinding;
        }
    }
}
