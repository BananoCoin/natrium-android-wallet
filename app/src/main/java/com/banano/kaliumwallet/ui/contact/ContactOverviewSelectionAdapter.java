package com.banano.kaliumwallet.ui.contact;

import android.databinding.DataBindingUtil;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.ContactSelected;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.databinding.ViewHolderContactBinding;
import com.banano.kaliumwallet.model.Contact;

import java.util.List;

public class ContactOverviewSelectionAdapter  extends RecyclerView.Adapter<ContactOverviewSelectionAdapter.ViewHolder> {
    private List<Contact> contactList;

    public ContactOverviewSelectionAdapter(List<Contact> flsLst) {
        contactList = flsLst;
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
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolderContactBinding contactItemBinding;

        public ViewHolder(ViewHolderContactBinding contactItemLayoutBinding) {
            super(contactItemLayoutBinding.getRoot());
            contactItemBinding = contactItemLayoutBinding;
        }
    }

    public void updateList(List<Contact> newList) {
        List<Contact> oldList = this.contactList;
        this.contactList = newList;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ContactSelectionDiffCallback(oldList, newList), true);
        diffResult.dispatchUpdatesTo(this);
    }
}
