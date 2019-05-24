package com.mparticle;

import android.support.annotation.NonNull;

/**
 * Implement this interface and react to deep links.
 */
public interface AttributionListener {
    void onResult(@NonNull AttributionResult result);
    void onError(@NonNull AttributionError error);
}