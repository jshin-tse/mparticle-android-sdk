package com.mparticle.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;

import com.mparticle.MParticle;

/**
 * <p>{@code Activity} implementation to be sub-classed that automatically handles screen tracking as
 * well as application background and foreground tracking.</p>
 *
 * <p></p>This class need only be used on pre-ICS (API level 14) devices. For pre-ICS, every {@code Activity} in your
 * application <i>must</i> either subclass an mParticle {@code Activity} implementation,
 * or must manually call {@link com.mparticle.MParticle#activityStarted(android.app.Activity)} within {@code onStart()} and
 * {@link com.mparticle.MParticle#activityStopped(android.app.Activity)} within {@code onStop()}.</p>
 *
 * <p>For ICS and later, screen tracking and application state tracking are handled automatically.</p>
 *
 * @see com.mparticle.activity.MPListActivity
 * @see com.mparticle.activity.MPFragmentActivity
 *
 */
@SuppressLint("Registered")
public class MPActivity extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            MParticle.getInstance().activityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            MParticle.getInstance().activityStopped(this);
        }
    }
}