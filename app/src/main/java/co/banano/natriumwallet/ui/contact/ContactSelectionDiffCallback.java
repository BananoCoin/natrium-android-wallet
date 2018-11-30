package co.banano.natriumwallet.ui.contact;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import co.banano.natriumwallet.model.Contact;

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
        return (newItem.getName().equals(oldItem.getName()));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}

