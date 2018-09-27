package co.banano.natriumwallet.di.application;

import android.content.Context;

import com.google.firebase.messaging.FirebaseMessagingService;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {
    final Context mContext;
    private FirebaseMessagingService messagingService;

    public ApplicationModule(Context context) {
        mContext = context;
    }

    @Provides
    Context providesApplicationContext() {
        return mContext;
    }
}
