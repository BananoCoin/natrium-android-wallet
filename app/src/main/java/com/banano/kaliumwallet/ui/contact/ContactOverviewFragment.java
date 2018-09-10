package com.banano.kaliumwallet.ui.contact;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.ContactAdded;
import com.banano.kaliumwallet.bus.ContactRemoved;
import com.banano.kaliumwallet.bus.ContactSelected;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.databinding.FragmentContactOverviewBinding;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseFragment;
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.ui.common.WindowControl;
import com.codekidlabs.storagechooser.StorageChooser;
import com.hwangjr.rxbus.annotation.Subscribe;

import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

/**
 *
 */
public class ContactOverviewFragment extends BaseFragment {
    public static final String TAG = ContactOverviewFragment.class.getSimpleName();
    private static final int WRITE_STORAGE_PERMISSION = 2;

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
        binding.setHandlers(new ClickHandlers());

        // subscribe to bus
        RxBus.get().register(this);

        // Prepare contacts info
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        binding.contactRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ContactOverviewSelectionAdapter(realm.copyFromRealm(contacts));
        binding.contactRecyclerview.setAdapter(mAdapter);

        return view;
    }

    @Subscribe
    public void receiveContactAdded(ContactAdded contactAdded) {
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        mAdapter.updateList(realm.copyFromRealm(contacts));
    }

    @Subscribe
    public void receiveContactRemoved(ContactRemoved contactRemoved) {
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        mAdapter.updateList(realm.copyFromRealm(contacts));
    }

    @Subscribe
    public void receiveContactSelected(ContactSelected contactSelected) {
        // show send dialog
        ContactViewDialogFragment dialog = ContactViewDialogFragment.newInstance(contactSelected.getName(), contactSelected.getAddress());
        dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                ContactViewDialogFragment.TAG);
        ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showExportDialog();
                } else {
                    UIUtil.showToast(getString(R.string.contact_export_permission_error), getContext());
                }
            }
        }
    }
    private void showExportDialog() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION);
            return;
        }
        List<Contact> contacts = realm.where(Contact.class).findAll();
        if (contacts.size() == 0) {
            UIUtil.showToast(getString(R.string.contact_export_none), getContext());
            return;
        }
        JSONArray contactJson = new JSONArray();
        for (Contact c : contacts) {
            contactJson.put(c.getJson());
        }
        // Initialize Builder
        StorageChooser.Theme theme = new StorageChooser.Theme(getContext());
        theme.setScheme(getResources().getIntArray(R.array.file_chooser_theme));
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(getActivity())
                .withFragmentManager(getActivity().getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setTheme(theme)
                .setDialogTitle(getString(R.string.contact_export_chooser))
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .build();
        chooser.show();
        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss", Locale.US);
                    String fileName = String.format("%s_contacts_%s.json", getString(R.string.app_name), dateFormat.format(new Date()));
                    FileWriter out = new FileWriter(new File(path, fileName));
                    out.write(contactJson.toString());
                    out.close();
                    UIUtil.showToast(getString(R.string.contact_export_success, new File(path, fileName).getAbsoluteFile()), getContext());
                } catch (IOException e) {
                    Timber.e(e);
                    UIUtil.showToast(getString(R.string.contact_export_error), getContext());
                }
            }
        });
    }

    public class ClickHandlers {
        public void onClickBack(View view) {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }

        public void onClickAdd(View view) {
            if (getActivity() instanceof WindowControl) {
                // show receive dialog
                AddContactDialogFragment dialog = AddContactDialogFragment.newInstance(null);
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        AddContactDialogFragment.TAG);
                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
            }
        }

        public void onClickExport(View view) {
            showExportDialog();
        }
    }
}
