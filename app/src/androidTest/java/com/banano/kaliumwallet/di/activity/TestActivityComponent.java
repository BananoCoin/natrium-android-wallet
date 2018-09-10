package com.banano.kaliumwallet.di.activity;

import com.banano.kaliumwallet.di.application.ApplicationComponent;
import com.banano.kaliumwallet.model.KaliumWalletTest;

import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface TestActivityComponent extends ActivityComponent {
    void inject(KaliumWalletTest kaliumWalletTest);
}
