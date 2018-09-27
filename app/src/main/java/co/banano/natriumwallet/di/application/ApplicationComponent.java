package co.banano.natriumwallet.di.application;


import co.banano.natriumwallet.KaliumApplication;
import co.banano.natriumwallet.KaliumMessagingService;
import co.banano.natriumwallet.di.persistence.PersistenceModule;
import co.banano.natriumwallet.util.SharedPreferencesUtil;

import javax.inject.Named;

import dagger.BindsInstance;
import dagger.Component;
import io.realm.Realm;

@Component(modules = {ApplicationModule.class, PersistenceModule.class})
@ApplicationScope
public interface ApplicationComponent {
    // persistence module
    SharedPreferencesUtil provideSharedPreferencesUtil();

    // database
    Realm provideRealm();

    // encryption key
    @Named("encryption_key")
    byte[] providesEncryptionKey();

    //void inject(KaliumMessagingService kaliumMessagingService);
}
