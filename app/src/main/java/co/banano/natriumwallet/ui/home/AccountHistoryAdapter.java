package co.banano.natriumwallet.ui.home;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.bus.TransactionItemClicked;
import co.banano.natriumwallet.databinding.ViewHolderTransactionBinding;
import co.banano.natriumwallet.network.model.response.AccountHistoryResponseItem;

import java.util.List;

public class AccountHistoryAdapter extends RecyclerView.Adapter<AccountHistoryAdapter.ViewHolder> {
    private List<AccountHistoryResponseItem> historyList;

    public AccountHistoryAdapter(List<AccountHistoryResponseItem> flsLst) {
        historyList = flsLst;
    }

    @Override
    public AccountHistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                               int viewType) {
        ViewHolderTransactionBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.view_holder_transaction, parent, false);

        ViewHolder viewHolder = new ViewHolder(binding);
        View view = binding.getRoot();
        view.setOnClickListener((View v) -> {
            AccountHistoryResponseItem h = (AccountHistoryResponseItem) view.getTag();
            if (h != null) {
                RxBus.get().post(new TransactionItemClicked(h.getHash(), h.getAccount()));
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AccountHistoryResponseItem accountHistoryResponseItem = historyList.get(position);
        holder.tranItemBinding.setAccountHistoryItem(accountHistoryResponseItem);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateList(List<AccountHistoryResponseItem> newList) {
        List<AccountHistoryResponseItem> oldList = this.historyList;
        this.historyList = newList;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AccountHistoryDiffCallback(oldList, newList), true);
        diffResult.dispatchUpdatesTo(this);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolderTransactionBinding tranItemBinding;

        public ViewHolder(ViewHolderTransactionBinding tranBinding) {
            super(tranBinding.getRoot());
            tranItemBinding = tranBinding;
        }
    }
}
