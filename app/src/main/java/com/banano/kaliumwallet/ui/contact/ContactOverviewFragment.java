package com.banano.kaliumwallet.ui.contact;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.banano.kaliumwallet.BuildConfig;
import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.databinding.FragmentContactOverviewBinding;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseFragment;
import com.banano.kaliumwallet.ui.settings.SettingsFragment;

import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;

/**
 *
 */
public class ContactOverviewFragment extends BaseFragment {
    public static final String TAG = ContactOverviewFragment.class.getSimpleName();

    private FragmentContactOverviewBinding binding;
    private ContactOverviewSelectionAdapter mAdapter;

    @Inject
    Realm realm;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of ContactOverviewFragment
     */
    public static ContactOverviewFragment newInstance() {
        Bundle args = new Bundle();
        ContactOverviewFragment fragment = new ContactOverviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inject
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_overview, container, false);
        view = binding.getRoot();

        // subscribe to bus
        RxBus.get().register(this);

        // Prepare contacts info
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        binding.contactRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ContactOverviewSelectionAdapter(contacts);
        binding.contactRecyclerview.setAdapter(mAdapter);
        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.list_divider));
        binding.contactRecyclerview.addItemDecoration(itemDecorator);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }
}
