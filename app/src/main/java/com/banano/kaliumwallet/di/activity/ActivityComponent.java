package com.banano.kaliumwallet.di.activity;

import com.banano.kaliumwallet.model.KaliumWallet;
import com.banano.kaliumwallet.ui.contact.AddContactDialogFragment;
import com.banano.kaliumwallet.ui.home.TranDetailsFragment;
import com.banano.kaliumwallet.ui.intro.IntroNewWalletBackupFragment;
import com.banano.kaliumwallet.ui.intro.IntroNewWalletWarningFragment;
import com.banano.kaliumwallet.ui.send.SendCompleteDialogFragment;
import com.banano.kaliumwallet.ui.send.SendConfirmDialogFragment;
import com.banano.kaliumwallet.ui.send.SendDialogFragment;
import com.banano.kaliumwallet.ui.settings.BackupSeedDialogFragment;
import com.banano.kaliumwallet.ui.settings.ChangeRepDialogFragment;
import com.banano.kaliumwallet.ui.settings.SettingsFragment;
import com.google.gson.Gson;

import com.banano.kaliumwallet.MainActivity;
import com.banano.kaliumwallet.di.application.ApplicationComponent;
import com.banano.kaliumwallet.network.AccountService;
import com.banano.kaliumwallet.ui.home.HomeFragment;
import com.banano.kaliumwallet.ui.intro.IntroNewWalletFragment;
import com.banano.kaliumwallet.ui.intro.IntroSeedFragment;
import com.banano.kaliumwallet.ui.intro.IntroWelcomeFragment;
import com.banano.kaliumwallet.ui.pin.CreatePinDialogFragment;
import com.banano.kaliumwallet.ui.pin.PinDialogFragment;
import com.banano.kaliumwallet.ui.receive.ReceiveDialogFragment;

import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface ActivityComponent {
    @ActivityScope
    AccountService provideAccountService();

    // wallet
    KaliumWallet provideNanoWallet();

    @ActivityScope
    Gson provideGson();

    void inject(AccountService accountService);

    void inject(AddContactDialogFragment addContactDialogFragment);

    void inject(BackupSeedDialogFragment backupSeedDialogFragment);

    void inject(ChangeRepDialogFragment changeRepDialogFragment);

    void inject(CreatePinDialogFragment createPinDialogFragment);

    void inject(HomeFragment homeFragment);

    void inject(IntroNewWalletFragment introNewWalletFragment);

    void inject(IntroNewWalletBackupFragment introNewWalletBackupFragment);

    void inject(IntroNewWalletWarningFragment introNewWalletWarningFragment);

    void inject(IntroWelcomeFragment introWelcomeFragment);

    void inject(IntroSeedFragment introSeedFragment);

    void inject(MainActivity mainActivity);

    void inject(KaliumWallet nanoWallet);

    void inject(PinDialogFragment pinDialogFragment);

    void inject(ReceiveDialogFragment receiveDialogFragment);

    void inject(SendDialogFragment sendDialogFragment);

    void inject(SendConfirmDialogFragment sendConfirmDialogFragment);

    void inject(SendCompleteDialogFragment sendCompleteDialogFragment);

    void inject(SettingsFragment settingsDialogFragment);

    void inject(TranDetailsFragment tranDetailsFragment);
}
