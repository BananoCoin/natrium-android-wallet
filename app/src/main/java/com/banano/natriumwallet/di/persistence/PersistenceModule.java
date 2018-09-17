package com.banano.natriumwallet.di.persistence;

import android.content.Context;
import android.util.Base64;

import com.banano.natriumwallet.bus.Logout;
import com.banano.natriumwallet.bus.RxBus;
import com.banano.natriumwallet.db.Migration;
import com.banano.natriumwallet.di.application.ApplicationScope;
import com.banano.natriumwallet.util.SharedPreferencesUtil;
import com.banano.natriumwallet.util.Vault;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmFileException;

@Module
public class PersistenceModule {
    private static final int SCHEMA_VERSION = 1;
    private static final String DB_NAME = "natrium.realm";

    @Provides
    @ApplicationScope
    SharedPreferencesUtil providesSharedPreferencesUtil(Context context) {
        return new SharedPreferencesUtil(context);
    }

    @Provides
    Realm providesRealmInstance(@Named("encryption_key") byte[] key) {
        try {
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .name(DB_NAME)
                    .encryptionKey(key)
                    .schemaVersion(SCHEMA_VERSION)
                    .migration(new Migration())
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            // Open the Realm with encryption enabled
            return Realm.getInstance(realmConfiguration);
        } catch (RealmFileException e) {
            // regenerate key and open realm with new key
            Vault.getVault()
                    .edit()
                    .putString(Vault.ENCRYPTION_KEY_NAME,
                            Base64.encodeToString(Vault.generateKey(), Base64.DEFAULT))
                    .apply();

            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .name(DB_NAME)
                    .encryptionKey(Base64.decode(Vault.getVault().getString(Vault.ENCRYPTION_KEY_NAME, null), Base64.DEFAULT))
                    .schemaVersion(SCHEMA_VERSION)
                    .migration(new Migration())
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm.deleteRealm(realmConfiguration);

            RxBus.get().post(new Logout());

            return Realm.getInstance(realmConfiguration);
        }
    }

    @Provides
    @Named("cachedir")
    @ApplicationScope
    String providesCacheDirectory(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    @Provides
    @Named("encryption_key")
    byte[] providesEncryptionKey() {
        if (Vault.getVault().getString(Vault.ENCRYPTION_KEY_NAME, null) == null) {
            Vault.getVault()
                    .edit()
                    .putString(Vault.ENCRYPTION_KEY_NAME,
                            Base64.encodeToString(Vault.generateKey(), Base64.DEFAULT))
                    .apply();
        }
        if (Vault.getVault() != null && Vault.getVault().getString(Vault.ENCRYPTION_KEY_NAME, null) != null) {
            return Base64.decode(Vault.getVault().getString(Vault.ENCRYPTION_KEY_NAME, null), Base64.DEFAULT);
        } else {
            return Vault.generateKey();
        }
    }
}
