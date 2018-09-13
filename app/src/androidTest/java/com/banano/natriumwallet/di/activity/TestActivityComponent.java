package com.banano.natriumwallet.di.activity;

import com.banano.natriumwallet.di.application.ApplicationComponent;
import com.banano.natriumwallet.model.KaliumWalletTest;

import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface TestActivityComponent extends ActivityComponent {
    void inject(KaliumWalletTest kaliumWalletTest);
}
