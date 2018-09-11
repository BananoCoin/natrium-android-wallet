package com.banano.kaliumwallet.ui.contact;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
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
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.util.svg.SvgSoftwareLayerSetter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class ContactOverviewSelectionAdapter extends RecyclerView.Adapter<ContactOverviewSelectionAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context context;
    private RequestBuilder<PictureDrawable> requestBuilder;
    private HashMap<String, Uri> monkeyUriMap;
    private int monKeyDimension;

    public ContactOverviewSelectionAdapter(List<Contact> contactList, Context context, HashMap<String, Uri> monkeyUriMap) {
        this.contactList = contactList;
        this.context = context;
        this.monkeyUriMap = monkeyUriMap;
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
        }
        if (context == null) {
            return;
        }

        Uri monkeyUri = monkeyUriMap.get(contact.getAddress());
        if (monkeyUri == null) {
            return;
        }
        try {
            if (requestBuilder != null) {
                requestBuilder.load(monkeyUri)
                              .apply(new RequestOptions().override(monKeyDimension, monKeyDimension))
                              .into(holder.contactItemBinding.contactOverviewMonkey);
            }
        } catch (Exception e) {
            Timber.e("Failed to load monKey file");
            e.printStackTrace();
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

    public void updateMap(HashMap<String, Uri> newMap) {
        this.monkeyUriMap = newMap;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolderContactBinding contactItemBinding;

        public ViewHolder(ViewHolderContactBinding contactItemLayoutBinding) {
            super(contactItemLayoutBinding.getRoot());
            contactItemBinding = contactItemLayoutBinding;
        }
    }
}
