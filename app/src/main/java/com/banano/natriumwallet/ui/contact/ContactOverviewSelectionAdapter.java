package com.banano.natriumwallet.ui.contact;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.banano.natriumwallet.R;
import com.banano.natriumwallet.bus.ContactSelected;
import com.banano.natriumwallet.bus.RxBus;
import com.banano.natriumwallet.databinding.ViewHolderContactBinding;
import com.banano.natriumwallet.model.Contact;
import com.banano.natriumwallet.ui.common.UIUtil;
import com.banano.natriumwallet.util.svg.SvgSoftwareLayerSetter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.List;

import timber.log.Timber;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class ContactOverviewSelectionAdapter extends RecyclerView.Adapter<ContactOverviewSelectionAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context context;
    private RequestBuilder<PictureDrawable> requestBuilder;
    private int monKeyDimension;

    public ContactOverviewSelectionAdapter(List<Contact> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
        this.requestBuilder = Glide.with(context)
                .as(PictureDrawable.class)
                .transition(withCrossFade())
                .listener(new SvgSoftwareLayerSetter());
        this.monKeyDimension = (int)UIUtil.convertDpToPixel(50, context);
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
