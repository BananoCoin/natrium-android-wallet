package com.banano.natriumwallet.di.activity;

import com.banano.natriumwallet.MainActivity;
import com.banano.natriumwallet.di.application.ApplicationComponent;
import com.banano.natriumwallet.model.KaliumWallet;
import com.banano.natriumwallet.network.AccountService;
import com.banano.natriumwallet.ui.contact.AddContactDialogFragment;
import com.banano.natriumwallet.ui.contact.ContactOverviewFragment;
import com.banano.natriumwallet.ui.contact.ContactViewDialogFragment;
import com.banano.natriumwallet.ui.home.HomeFragment;
import com.banano.natriumwallet.ui.home.TranDetailsFragment;
import com.banano.natriumwallet.ui.intro.IntroNewWalletBackupFragment;
import com.banano.natriumwallet.ui.intro.IntroNewWalletFragment;
import com.banano.natriumwallet.ui.intro.IntroNewWalletWarningFragment;
import com.banano.natriumwallet.ui.intro.IntroSeedFragment;
import com.banano.natriumwallet.ui.intro.IntroWelcomeFragment;
import com.banano.natriumwallet.ui.pin.CreatePinDialogFragment;
import com.banano.natriumwallet.ui.pin.PinDialogFragment;
import com.banano.natriumwallet.ui.receive.ReceiveDialogFragment;
import com.banano.natriumwallet.ui.send.SendCompleteDialogFragment;
import com.banano.natriumwallet.ui.send.SendConfirmDialogFragment;
import com.banano.natriumwallet.ui.send.SendDialogFragment;
import com.banano.natriumwallet.ui.settings.BackupSeedDialogFragment;
import com.banano.natriumwallet.ui.settings.ChangeRepDialogFragment;
import com.banano.natriumwallet.ui.settings.SettingsFragment;
import com.google.gson.Gson;

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

    void inject(ContactOverviewFragment contactOverviewFragment);

    void inject(ContactViewDialogFragment contactViewDialogFragment);

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
