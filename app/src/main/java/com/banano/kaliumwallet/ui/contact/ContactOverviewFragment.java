package com.banano.kaliumwallet.ui.contact;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
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
import com.banano.kaliumwallet.model.Address;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.task.DownloadOrRetrieveFileTask;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseFragment;
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.ui.common.WindowControl;
import com.hwangjr.rxbus.annotation.Subscribe;

import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final int READ_STORAGE_PERMISSION = 1;
    private static final int WRITE_STORAGE_PERMISSION = 2;
    private static final int READ_RESULT_CODE = 3;

    private FragmentContactOverviewBinding binding;
    private ContactOverviewSelectionAdapter mAdapter;

    private boolean showExport = false;
    private boolean showImport = false;

    private DownloadOrRetrieveFileTask downloadMonkeyTask;

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
        showExport = false;
        showImport = false;
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

        // Find contacts
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        binding.contactRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ContactOverviewSelectionAdapter(realm.copyFromRealm(contacts), getContext());
        binding.contactRecyclerview.setAdapter(mAdapter);
        // Download any monKeys that we don't have yet
        initMonkeys();

        return view;
    }

    private void initMonkeys() {
        // Find contacts
        List<Contact> contacts = mAdapter.getContactList();
        if (contacts.size() == 0) {
            return;
        }
        // Get list of monkey URLs that need downloaded here
        List<String> monkeyUrls = new ArrayList<>();
        for (Contact c: contacts) {
            if (c.getMonkeyPath() == null) {
                monkeyUrls.add(getString(R.string.monkey_api_url, c.getAddress()));
            }
        }
        if (monkeyUrls.isEmpty()) {
            return; // Nothing to download
        }
        // Download monKeys and notify adapter of change
        downloadMonkeyTask = new DownloadOrRetrieveFileTask(getContext().getFilesDir());
        downloadMonkeyTask.setListener((List<File> monkeys) -> {
            if (monkeys == null || monkeys.isEmpty()) {
                return;
            }
            int updatedCount = 0;
            for (File f: monkeys) {
                if (f.exists()) {
                    String address = Address.findAddress(f.getAbsolutePath());
                    if (address != null && !address.isEmpty()) {
                        realm.executeTransaction(realm -> {
                            Contact c = realm.where(Contact.class).equalTo("address", address).findFirst();
                            c.setMonkeyPath(f.getAbsolutePath());
                        });
                        updatedCount++;
                    }
                }
            }
            if (updatedCount > 0) {
                refreshContacts();
            }
        });
        downloadMonkeyTask.execute(monkeyUrls.toArray(new String[monkeyUrls.size()]));
    }

    private void refreshContacts() {
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        mAdapter.updateList(realm.copyFromRealm(contacts));
    }

    @Subscribe
    public void receiveContactAdded(ContactAdded contactAdded) {
        refreshContacts();
        initMonkeys();
    }

    @Subscribe
    public void receiveContactRemoved(ContactRemoved contactRemoved) {
        refreshContacts();
    }

    @Subscribe
    public void receiveContactSelected(ContactSelected contactSelected) {
        // show send dialog
        if (getActivity() instanceof WindowControl) {
            ContactViewDialogFragment dialog = ContactViewDialogFragment.newInstance(contactSelected.getName(), contactSelected.getAddress());
            dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                    ContactViewDialogFragment.TAG);
            ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
        // Avoid leaks
        if (downloadMonkeyTask != null) {
            downloadMonkeyTask.setListener(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (showExport) {
                        showExport = false;
                        showExportDialog();
                    }
                } else {
                    UIUtil.showToast(getString(R.string.contact_export_permission_error), getContext());
                }
            }
            case READ_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (showImport) {
                        showImport = false;
                        showImportDialog();
                    }
                } else {
                    UIUtil.showToast(getString(R.string.contact_import_permission_error), getContext());
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == READ_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                realm.executeTransaction(realm -> {
                    try (InputStream is = getContext().getContentResolver().openInputStream(resultData.getData())) {
                        long oldCount = realm.where(Contact.class).count();
                        realm.createOrUpdateAllFromJson(Contact.class, is);
                        long count = realm.where(Contact.class).count();
                        long diff = count - oldCount;
                        if (diff > 0) {
                            UIUtil.showToast(getString(R.string.contact_import_success, diff), getContext());
                        } else {
                            UIUtil.showToast(getString(R.string.contact_import_none), getContext());
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                        UIUtil.showToast(getString(R.string.contact_import_error), getContext());
                    }
                });
                refreshContacts();
                initMonkeys();
            }
        }
    }

    private void showImportDialog() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            showImport = true;
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_PERMISSION);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, READ_RESULT_CODE);
    }

    private void showExportDialog() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            showExport = true;
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
        // Save file
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss", Locale.US);
            String fileName = String.format("%s_contacts_%s.json", getString(R.string.app_name), dateFormat.format(new Date())).toLowerCase();
            FileWriter out = new FileWriter(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName));
            out.write(contactJson.toString());
            out.close();
            UIUtil.showToast(getString(R.string.contact_export_success, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).getAbsoluteFile()), getContext());
        } catch (IOException e) {
            Timber.e(e);
            UIUtil.showToast(getString(R.string.contact_export_error), getContext());
        }
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

        public void onClickImport(View view) {
            showImportDialog();
        }
    }
}
