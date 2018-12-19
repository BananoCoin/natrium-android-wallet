package co.banano.natriumwallet.di.activity;

import co.banano.natriumwallet.MainActivity;
import co.banano.natriumwallet.di.application.ApplicationComponent;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.ui.contact.AddContactDialogFragment;
import co.banano.natriumwallet.ui.contact.ContactOverviewFragment;
import co.banano.natriumwallet.ui.contact.ContactViewDialogFragment;
import co.banano.natriumwallet.ui.home.HomeFragment;
import co.banano.natriumwallet.ui.home.TranDetailsFragment;
import co.banano.natriumwallet.ui.intro.IntroNewWalletBackupFragment;
import co.banano.natriumwallet.ui.intro.IntroNewWalletFragment;
import co.banano.natriumwallet.ui.intro.IntroNewWalletWarningFragment;
import co.banano.natriumwallet.ui.intro.IntroSeedFragment;
import co.banano.natriumwallet.ui.intro.IntroWelcomeFragment;
import co.banano.natriumwallet.ui.pin.CreatePinDialogFragment;
import co.banano.natriumwallet.ui.pin.PinDialogFragment;
import co.banano.natriumwallet.ui.receive.ReceiveDialogFragment;
import co.banano.natriumwallet.ui.send.SendCompleteDialogFragment;
import co.banano.natriumwallet.ui.send.SendConfirmDialogFragment;
import co.banano.natriumwallet.ui.send.SendDialogFragment;
import co.banano.natriumwallet.ui.settings.BackupSeedDialogFragment;
import co.banano.natriumwallet.ui.settings.ChangeRepDialogFragment;
import co.banano.natriumwallet.ui.settings.SettingsFragment;
import com.google.gson.Gson;

import co.banano.natriumwallet.ui.transfer.TransferCompleteDialogFragment;
import co.banano.natriumwallet.ui.transfer.TransferConfirmDialogFragment;
import co.banano.natriumwallet.ui.transfer.TransferIntroDialogFragment;
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

    void inject(TransferIntroDialogFragment transferIntroDialogFragment);

    void inject(TransferConfirmDialogFragment transferConfirmDialogFragment);

    void inject(TransferCompleteDialogFragment transferCompleteDialogFragment);
}
