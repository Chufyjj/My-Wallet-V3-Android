package piuk.blockchain.androidcore.data.charts

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import java.util.Calendar

enum class TimeSpan {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

/**
 * All time start times in epoch-seconds
 */

// 2010-08-18 00:00:00 UTC
const val FIRST_BTC_ENTRY_TIME = 1282089600L
// 2015-08-08 00:00:00 UTC
const val FIRST_ETH_ENTRY_TIME = 1438992000L
// 2017-07-24 00:00:00 UTC
const val FIRST_BCH_ENTRY_TIME = 1500854400L
// 2014-09-04 00:00:00 UTC
const val FIRST_XLM_ENTRY_TIME = 1409875200L

typealias PriceSeries = List<PriceDatum>

@Deprecated("Merge with ExchangeRateService")
class ChartsDataManager(private val historicPriceApi: PriceApi, rxBus: RxBus) {

    private val rxPinning = RxPinning(rxBus)

    fun getHistoricPriceSeries(
        cryptoCurrency: CryptoCurrency,
        fiatCurrency: String,
        timeSpan: TimeSpan,
        timeInterval: TimeInterval = suggestedTimeIntervalForSpan(timeSpan)
    ): Single<PriceSeries> {

        var proposedStartTime = getStartTimeForTimeSpan(timeSpan, cryptoCurrency)
        // It's possible that the selected start time is before the currency existed, so check here
        // and show ALL_TIME instead if that's the case.
        if (proposedStartTime < getFirstMeasurement(cryptoCurrency)) {
            proposedStartTime = getStartTimeForTimeSpan(TimeSpan.ALL_TIME, cryptoCurrency)
        }

        return rxPinning.callSingle<PriceSeries> {
            historicPriceApi.getHistoricPriceSeries(
                cryptoCurrency.symbol,
                fiatCurrency,
                proposedStartTime,
                timeInterval.intervalSeconds
            ).subscribeOn(Schedulers.io())
        }
    }

    private fun getStartTimeForTimeSpan(
        timeSpan: TimeSpan,
        cryptoCurrency: CryptoCurrency
    ): Long {
        val start = when (timeSpan) {
            TimeSpan.ALL_TIME -> return getFirstMeasurement(cryptoCurrency)
            TimeSpan.YEAR -> 365
            TimeSpan.MONTH -> 30
            TimeSpan.WEEK -> 7
            TimeSpan.DAY -> 1
        }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
        return cal.timeInMillis / 1000
    }

    /**
     * Provides the first timestamp for which we have prices, returned in epoch-seconds
     *
     * @param cryptoCurrency The [CryptoCurrency] that you want a start date for
     * @return A [Long] in epoch-seconds since the start of our data
     */
    private fun getFirstMeasurement(cryptoCurrency: CryptoCurrency): Long {
        return when (cryptoCurrency) {
            CryptoCurrency.BTC -> FIRST_BTC_ENTRY_TIME
            CryptoCurrency.ETHER -> FIRST_ETH_ENTRY_TIME
            CryptoCurrency.BCH -> FIRST_BCH_ENTRY_TIME
            CryptoCurrency.XLM -> FIRST_XLM_ENTRY_TIME
            CryptoCurrency.PAX -> TODO("PAX is not yet supported - AND-2003")
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }
    }

    private fun suggestedTimeIntervalForSpan(timeSpan: TimeSpan): TimeInterval =
        when (timeSpan) {
            TimeSpan.ALL_TIME -> TimeInterval.FIVE_DAYS
            TimeSpan.YEAR -> TimeInterval.ONE_DAY
            TimeSpan.MONTH -> TimeInterval.TWO_HOURS
            TimeSpan.WEEK -> TimeInterval.ONE_HOUR
            TimeSpan.DAY -> TimeInterval.FIFTEEN_MINUTES
        }

    companion object {
        /** All time start times in epoch-seconds */
        const val FIRST_BTC_ENTRY_TIME = 1282089600L // 2010-08-18 00:00:00 UTC
        const val FIRST_ETH_ENTRY_TIME = 1438992000L // 2015-08-08 00:00:00 UTC
        const val FIRST_BCH_ENTRY_TIME = 1500854400L // 2017-07-24 00:00:00 UTC
        const val FIRST_XLM_ENTRY_TIME = 1409875200L // 2014-09-04 00:00:00 UTC
    }
}
