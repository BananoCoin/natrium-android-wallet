package co.banano.natriumwallet;

import android.content.Context;
import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Base64;

import co.banano.natriumwallet.di.application.ApplicationComponent;
import co.banano.natriumwallet.di.application.ApplicationModule;
import co.banano.natriumwallet.di.application.DaggerApplicationComponent;
import co.banano.natriumwallet.util.Vault;
import com.github.ajalt.reprint.core.Reprint;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Any custom application logic can go here
 */
public class KaliumApplication extends MultiDexApplication {
    private ApplicationComponent mApplicationComponent;

    public static KaliumApplication getApplication(Context context) {
        return (KaliumApplication) context.getApplicationContext();
    }

    public void onCreate() {
        super.onCreate();

        // initialize Realm database
        Realm.init(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        // create new instance of the application component (DI)
        mApplicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        // initialize vault
        Vault.initializeVault(this);
        generateEncryptionKey();

        // initialize fingerprint
        Reprint.initialize(this);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    /**
     * generate an encryption key and store in the vault
     */
    private void generateEncryptionKey() {
        if (Vault.getVault().getString(Vault.ENCRYPTION_KEY_NAME, null) == null) {
            Vault.getVault()
                    .edit()
                    .putString(Vault.ENCRYPTION_KEY_NAME,
                            Base64.encodeToString(Vault.generateKey(), Base64.DEFAULT))
                    .apply();
        }
    }

    /**
     * Retrieve instance of application Dagger component
     *
     * @return ApplicationComponent
     */
    public ApplicationComponent getApplicationComponent() {
        return mApplicationComponent;
    }
}