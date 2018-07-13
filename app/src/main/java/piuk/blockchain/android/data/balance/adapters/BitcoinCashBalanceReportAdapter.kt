package piuk.blockchain.android.data.balance.adapters

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.BalanceReporter
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import info.blockchain.balance.CryptoCurrency
import java.math.BigInteger

fun BchDataManager.toBalanceReporter(): BalanceReporter = BitcoinCashBalanceReportAdapter(this)

private class BitcoinCashBalanceReportAdapter(
    private val dataManager: BchDataManager
) : BalanceReporter {
    override fun entireBalance() =
        dataManager.getWalletBalance().toBalance()

    override fun importedAddressBalance() =
        dataManager.getImportedAddressBalance().toBalance()

    override fun addressBalance(address: String) =
        dataManager.getAddressBalance(address).toBalance()

    private fun BigInteger.toBalance() =
        CryptoValue(CryptoCurrency.BCH, this.toLong())
}