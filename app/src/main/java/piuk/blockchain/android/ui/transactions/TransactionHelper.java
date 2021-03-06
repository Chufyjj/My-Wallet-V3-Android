package piuk.blockchain.android.ui.transactions;

import androidx.annotation.NonNull;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payment.Payment;
import org.apache.commons.lang3.tuple.Pair;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.transactions.models.Displayable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static info.blockchain.wallet.multiaddress.TransactionSummary.Direction.RECEIVED;

public class TransactionHelper {

    private PayloadDataManager payloadDataManager;
    private BchDataManager bchDataManager;

    public TransactionHelper(PayloadDataManager payloadDataManager, BchDataManager bchDataManager) {
        this.payloadDataManager = payloadDataManager;
        this.bchDataManager = bchDataManager;
    }

    /**
     * Return a Pair of maps that correspond to the inputs and outputs of a transaction, whilst
     * filtering out Change addresses.
     *
     * @param transactionSummary A {@link TransactionSummary} object
     * @return A Pair of Maps representing the input and output of the transaction
     */
    @NonNull
    Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> filterNonChangeAddresses(Displayable transactionSummary) {

        HashMap<String, BigInteger> inputMap = new HashMap<>();
        HashMap<String, BigInteger> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transactionSummary.getDirection().equals(RECEIVED) && !transactionSummary.getInputsMap().isEmpty()) {
            // Only 1 addr for receive
            TreeMap<String, BigInteger> treeMap = new TreeMap<>(transactionSummary.getInputsMap());
            inputMap.put(treeMap.lastKey(), treeMap.lastEntry().getValue());
        } else {
            for (String inputAddress : transactionSummary.getInputsMap().keySet()) {
                BigInteger inputValue = transactionSummary.getInputsMap().get(inputAddress);
                // Move or Send
                // The address belongs to us
                String xpub = payloadDataManager.getXpubFromAddress(inputAddress);

                // Address belongs to xpub we own
                if (xpub != null) {
                    // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap.put(inputAddress, inputValue);
                        inputXpubList.add(xpub);
                    }
                } else {
                    // Legacy Address or someone else's address
                    inputMap.put(inputAddress, inputValue);
                }
            }
        }

        // Outputs / To field
        for (String outputAddress : transactionSummary.getOutputsMap().keySet()) {
            BigInteger outputValue = transactionSummary.getOutputsMap().get(outputAddress);

            if (payloadDataManager.isOwnHDAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = payloadDataManager.getXpubFromAddress(outputAddress);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    BigInteger prevAmount = outputMap.get(outputAddress).add(outputValue);
                    outputMap.put(outputAddress, prevAmount);
                } else {
                    outputMap.put(outputAddress, outputValue);
                }

            } else if (payloadDataManager.getWallet().getLegacyAddressStringList().contains(outputAddress)
                    || payloadDataManager.getWallet().getWatchOnlyAddressStringList().contains(outputAddress)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(outputAddress) && outputValue.abs().compareTo(transactionSummary.getTotal()) != 0) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (outputValue.abs().compareTo(transactionSummary.getTotal()) > 0) {
                    continue;
                }

                outputMap.put(outputAddress, outputValue);
            } else {
                if (!transactionSummary.getDirection().equals(RECEIVED)) {
                    outputMap.put(outputAddress, outputValue);
                }
            }
        }

        return Pair.of(inputMap, outputMap);
    }

    Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> filterNonChangeAddressesBch(Displayable transactionSummary) {

        HashMap<String, BigInteger> inputMap = new HashMap<>();
        HashMap<String, BigInteger> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transactionSummary.getDirection().equals(RECEIVED) && !transactionSummary.getInputsMap().isEmpty()) {
            for (Map.Entry<String, BigInteger> entry : transactionSummary.getInputsMap().entrySet()) {
                String address = entry.getKey();
                BigInteger value = entry.getValue();
                if (value.equals(Payment.DUST)) continue;
                inputMap.put(address, value);
            }
        } else {
            for (String inputAddress : transactionSummary.getInputsMap().keySet()) {
                BigInteger inputValue = transactionSummary.getInputsMap().get(inputAddress);
                // Move or Send
                // The address belongs to us
                String xpub = bchDataManager.getXpubFromAddress(inputAddress);

                // Skip dust input
                if (inputValue != null && inputValue.equals(Payment.DUST)) continue;

                // Address belongs to xpub we own
                if (xpub != null) {
                    // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap.put(inputAddress, inputValue);
                        inputXpubList.add(xpub);
                    }
                } else {
                    // Legacy Address or someone else's address
                    inputMap.put(inputAddress, inputValue);
                }
            }
        }

        // Outputs / To field
        for (String outputAddress : transactionSummary.getOutputsMap().keySet()) {
            BigInteger outputValue = transactionSummary.getOutputsMap().get(outputAddress);

            // Skip dust output
            if (outputValue != null && outputValue.equals(Payment.DUST)) continue;

            if (bchDataManager.isOwnAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = bchDataManager.getXpubFromAddress(outputAddress);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    BigInteger prevAmount = outputMap.get(outputAddress).add(outputValue);
                    outputMap.put(outputAddress, prevAmount);
                } else {
                    outputMap.put(outputAddress, outputValue);
                }

            } else if (bchDataManager.getLegacyAddressStringList().contains(outputAddress)
                    || bchDataManager.getWatchOnlyAddressStringList().contains(outputAddress)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(outputAddress) && outputValue.abs().compareTo(transactionSummary.getTotal()) != 0) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (outputValue.abs().compareTo(transactionSummary.getTotal()) > 0) {
                    continue;
                }

                outputMap.put(outputAddress, outputValue);
            } else {
                if (!transactionSummary.getDirection().equals(RECEIVED)) {
                    outputMap.put(outputAddress, outputValue);
                }
            }
        }

        return Pair.of(inputMap, outputMap);
    }
}
