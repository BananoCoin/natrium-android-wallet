package co.banano.natriumwallet.ui.common;

import co.banano.natriumwallet.di.activity.ActivityComponent;
import co.banano.natriumwallet.di.application.ApplicationComponent;

/**
 * Interface for Activity with a Component
 */

public interface ActivityWithComponent {
    ActivityComponent getActivityComponent();

    ApplicationComponent getApplicationComponent();
}
