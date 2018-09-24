package co.banano.natriumwallet.di.activity;

import co.banano.natriumwallet.di.application.ApplicationComponent;
import co.banano.natriumwallet.model.KaliumWalletTest;

import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface TestActivityComponent extends ActivityComponent {
    void inject(KaliumWalletTest kaliumWalletTest);
}
