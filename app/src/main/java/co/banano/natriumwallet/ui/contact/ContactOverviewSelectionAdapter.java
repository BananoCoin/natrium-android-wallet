package co.banano.natriumwallet.ui.contact;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.ContactSelected;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.ViewHolderContactBinding;
import co.banano.natriumwallet.model.Contact;

import java.util.List;


public class ContactOverviewSelectionAdapter extends RecyclerView.Adapter<ContactOverviewSelectionAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context context;

    public ContactOverviewSelectionAdapter(List<Contact> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
    }

    @Override
    public ContactOverviewSelectionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                         int viewType) {
        ViewHolderContactBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.view_holder_contact, parent, false);

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
        if (position > 0) {
            holder.contactItemBinding.dividerLineTop.setVisibility(View.GONE);
        } else {
            holder.contactItemBinding.dividerLineTop.setVisibility(View.VISIBLE);
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

    public List<Contact> getContactList() {
        return contactList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolderContactBinding contactItemBinding;

        public ViewHolder(ViewHolderContactBinding contactItemLayoutBinding) {
            super(contactItemLayoutBinding.getRoot());
            contactItemBinding = contactItemLayoutBinding;
        }
    }
}
