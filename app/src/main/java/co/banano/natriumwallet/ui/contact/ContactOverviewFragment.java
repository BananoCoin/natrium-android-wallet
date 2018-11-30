package co.banano.natriumwallet.ui.contact;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.ContactAdded;
import co.banano.natriumwallet.bus.ContactRemoved;
import co.banano.natriumwallet.bus.ContactSelected;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentContactOverviewBinding;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hwangjr.rxbus.annotation.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final int READ_STORAGE_PERMISSION = 1;
    private static final int WRITE_STORAGE_PERMISSION = 2;
    private static final int READ_RESULT_CODE = 3;

    private FragmentContactOverviewBinding binding;
    private ContactOverviewSelectionAdapter mAdapter;

    private boolean showExport = false;
    private boolean showImport = false;

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

        return view;
    }

    private void refreshContacts() {
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        mAdapter.updateList(realm.copyFromRealm(contacts));
    }

    @Subscribe
    public void receiveContactAdded(ContactAdded contactAdded) {
        refreshContacts();
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
                        // Do some validation on the json, we'll just remove invalid objects
                        JsonElement element = new JsonParser().parse(new InputStreamReader(is));
                        JSONArray inputJson = new JSONArray(element.getAsJsonArray().toString());
                        JSONArray validJson = new JSONArray();
                        for (int i = 0; i < inputJson.length(); i++) {
                            JSONObject jObj = inputJson.getJSONObject(i);
                            // Calling a get on name, which will raise an exception if it doesn't exist
                            try {
                                String name = jObj.getString("name");
                                if (name == null || name.isEmpty()) {
                                    continue;
                                } else if (!name.startsWith("@")) {
                                    name = "@" + name;
                                    jObj.put("name", name);
                                }
                            } catch (JSONException je) {
                                continue;
                            }
                            String address = Address.findAddress(jObj.getString("address"));
                            if (address != null && !address.isEmpty()) {
                                validJson.put(jObj);
                            }
                        }
                        long oldCount = realm.where(Contact.class).count();
                        realm.createOrUpdateAllFromJson(Contact.class, validJson);
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
