package co.banano.natriumwallet.ui.home;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import co.banano.natriumwallet.network.model.response.AccountHistoryResponseItem;

import java.util.List;

public class AccountHistoryDiffCallback extends DiffUtil.Callback {

    List<AccountHistoryResponseItem> oldItems;
    List<AccountHistoryResponseItem> newItems;

    public AccountHistoryDiffCallback(List<AccountHistoryResponseItem> oldItems, List<AccountHistoryResponseItem> newItems) {
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
        return oldItems.get(oldItemPosition).getHash().equals(newItems.get(newItemPosition).getHash());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        String oldContactName = "";
        String newContactName = "";
        if (oldItems.get(oldItemPosition).getContactName() != null) {
            oldContactName = oldItems.get(oldItemPosition).getContactName();
        }
        if (newItems.get(newItemPosition).getContactName() != null) {
            newContactName = newItems.get(newItemPosition).getContactName();
        }
        return (oldItems.get(oldItemPosition).getAccount().equals(newItems.get(newItemPosition).getAccount()) &&
                oldContactName.equals(newContactName));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
