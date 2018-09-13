package com.banano.natriumwallet.ui.common;

import com.banano.natriumwallet.di.activity.ActivityComponent;
import com.banano.natriumwallet.di.application.ApplicationComponent;

/**
 * Interface for Activity with a Component
 */

public interface ActivityWithComponent {
    ActivityComponent getActivityComponent();

    ApplicationComponent getApplicationComponent();
}
