package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseAbstractTest

class PinningTestNetworkOptionsEnabled : PinningTest() {
    override fun shouldPin(): Boolean {
        return false
    }

    protected fun transformMParticleOptions(builder: MParticleOptions.Builder): MParticleOptions.Builder {
        return builder
            .environment(MParticle.Environment.Development)
            .networkOptions(
                NetworkOptions.builder()
                    .setPinningDisabledInDevelopment(true)
                    .build()
            )
    }
}