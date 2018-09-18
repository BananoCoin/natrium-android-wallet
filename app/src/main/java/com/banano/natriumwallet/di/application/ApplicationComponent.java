package com.banano.natriumwallet.di.application;


import com.banano.natriumwallet.KaliumApplication;
import com.banano.natriumwallet.KaliumMessagingService;
import com.banano.natriumwallet.di.persistence.PersistenceModule;
import com.banano.natriumwallet.util.SharedPreferencesUtil;

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
