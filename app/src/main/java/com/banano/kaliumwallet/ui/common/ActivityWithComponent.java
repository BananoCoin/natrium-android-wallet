package com.banano.kaliumwallet.ui.common;

import com.banano.kaliumwallet.di.activity.ActivityComponent;
import com.banano.kaliumwallet.di.application.ApplicationComponent;

/**
 * Interface for Activity with a Component
 */

public interface ActivityWithComponent {
    ActivityComponent getActivityComponent();

    ApplicationComponent getApplicationComponent();
}
