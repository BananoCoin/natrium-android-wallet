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
import com.banano.kaliumwallet.databinding.ViewHolderContactSmBinding;
import com.banano.kaliumwallet.model.Contact;

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
