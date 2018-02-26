package com.coursera.assignment1;

import java.util.ArrayList;
import java.util.List;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current com.coursera.assignment1.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the com.coursera.assignment1.UTXOPool(com.coursera.assignment1.UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool=new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current com.coursera.assignment1.UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no com.coursera.assignment1.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUTXOs = new UTXOPool();

        double txSum = 0.0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);

            //(1) all outputs claimed by {@code tx} are in the current com.coursera.assignment1.UTXO pool
            if (!utxoPool.contains(utxo)) {
                return false;
            }

            //(2) the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), in.signature)) {
                return false;
            }

            //(3) no com.coursera.assignment1.UTXO is claimed multiple times by {@code tx}
            if (uniqueUTXOs.contains(utxo)) {
                return false;
            }
            uniqueUTXOs.addUTXO(utxo, output);

            txSum += output.value;
        }

        for (Transaction.Output out : tx.getOutputs()) {
            //(4) all of {@code tx}s output values are non-negative
            if (out.value < 0) {
                return false;
            }
            txSum -= out.value;
        }

        //(5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        //values; and false otherwise.
        return txSum>=0;
    }

    public double calculateFee(Transaction tx) {
        double sum = 0.0;
        for(Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            sum+=utxoPool.getTxOutput(utxo).value;
        }

        for(Transaction.Output output : tx.getOutputs()) {
            sum-=output.value;
        }

        return sum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current com.coursera.assignment1.UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> acceptedTxList = new ArrayList<>();
        for(Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                for(Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }

                for(int i=0; i<tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, tx.getOutputs().get(i));
                }
                acceptedTxList.add(tx);
            }
        }

        Transaction[] accepted = new Transaction[acceptedTxList.size()];

        return acceptedTxList.toArray(accepted);
    }
}
