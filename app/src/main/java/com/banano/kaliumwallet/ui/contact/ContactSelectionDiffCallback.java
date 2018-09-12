package com.banano.kaliumwallet.ui.contact;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.banano.kaliumwallet.model.Contact;

import java.util.List;

public class ContactSelectionDiffCallback extends DiffUtil.Callback {

    List<Contact> oldItems;
    List<Contact> newItems;

    public ContactSelectionDiffCallback(List<Contact> oldItems, List<Contact> newItems) {
        this.newItems = newItems;
        this.oldItems = oldItems;
    }

    @Override
    public int getOldListSize() {
        return oldItems.size();
    }

    @Override
    public int getNewListSize() {
        return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldItems.get(oldItemPosition).getAddress().equals(newItems.get(newItemPosition).getAddress());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Contact oldItem = oldItems.get(oldItemPosition);
        Contact newItem = newItems.get(newItemPosition);
        String oldItemMonkeyPath = oldItem.getMonkeyPath();
        String newItemMonkeyPath = newItem.getMonkeyPath();
        if (oldItemMonkeyPath == null) {
            oldItemMonkeyPath = "";
        }
        if (newItemMonkeyPath == null) {
            newItemMonkeyPath = "";
        }
        return (newItemMonkeyPath.equals(oldItemMonkeyPath) && newItem.getName().equals(oldItem.getName()));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}

