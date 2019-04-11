package com.blockchain.koin

import android.app.Application
import com.blockchain.injection.kycModule
import com.blockchain.injection.kycNabuModule
import com.blockchain.koin.modules.apiInterceptorsModule
import com.blockchain.koin.modules.appProperties
import com.blockchain.koin.modules.applicationModule
import com.blockchain.koin.modules.dashboardModule
import com.blockchain.koin.modules.environmentModule
import com.blockchain.koin.modules.features
import com.blockchain.koin.modules.homeBrewModule
import com.blockchain.koin.modules.keys
import com.blockchain.koin.modules.localShapeShift
import com.blockchain.koin.modules.morphMethodModule
import com.blockchain.koin.modules.moshiModule
import com.blockchain.koin.modules.nabuUrlModule
import com.blockchain.koin.modules.serviceModule
import com.blockchain.koin.modules.shapeShiftModule
import com.blockchain.koin.modules.urls
import com.blockchain.koin.modules.xlmModule
import com.blockchain.lockbox.koin.lockboxModule
import com.blockchain.network.modules.apiModule
import com.blockchain.notifications.koin.notificationModule
import org.koin.android.ext.android.startKoin
import org.koin.log.Logger
import org.koin.standalone.StandAloneContext
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.dashboard.announcements.dashboardAnnouncementsModule
import timber.log.Timber

object KoinStarter {

    private lateinit var application: Application

    @JvmStatic
    fun start(application: Application) {
        StandAloneContext.closeKoin()
        @Suppress("ConstantConditionIf")
        application.startKoin(
            application,
            listOf(
                environmentModule,
                walletModule,
                coreModule,
                coreUiModule,
                coreUiFeatureFlagsModule,
                dashboardModule,
                dashboardAnnouncementsModule,
                apiModule,
                apiInterceptorsModule,
                serviceModule,
                applicationModule,
                shapeShiftModule,
                localShapeShift,
                buySellModule,
                moshiModule,
                kycModule,
                kycNabuModule,
                morphMethodModule,
                morphUiModule,
                nabuModule,
                nabuUrlModule,
                homeBrewModule,
                notificationModule,
                sunriverModule,
                lockboxModule,
                xlmModule
            ),
            extraProperties = features + appProperties + keys + urls,
            logger = if (BuildConfig.LOG_KOIN_STARTUP) TimberLogger() else NullLogger()
        )
        KoinStarter.application = application
    }
}

private class TimberLogger : Logger {
    override fun debug(msg: String) {
        Timber.d(msg)
    }

    override fun err(msg: String) {
        Timber.e(msg)
    }

    override fun log(msg: String) {
        Timber.i(msg)
    }
}

private class NullLogger : Logger {
    override fun debug(msg: String) { }
    override fun err(msg: String) { }
    override fun log(msg: String) { }
}