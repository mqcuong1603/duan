package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single uncertain transaction where each item has an
 * existential probability of appearing in the transaction.
 *
 * In an uncertain database, items are not simply present or absent.
 * Instead, each item has a probability between 0 and 1 indicating
 * the likelihood of its existence in the transaction.
 *
 * Example: Transaction "1(0.8) 3(0.6) 4(0.9)" means:
 *   - Item 1 appears with probability 0.8
 *   - Item 3 appears with probability 0.6
 *   - Item 4 appears with probability 0.9
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class UncertainTransaction {

    /** Transaction ID (0-indexed position in the database) */
    private final int transactionId;

    /** Maps each item to its existential probability in this transaction */
    private final Map<Integer, Double> itemProbabilities;

    /**
     * Creates a new uncertain transaction.
     *
     * @param transactionId the unique ID of this transaction
     * @param itemProbabilities map of item → probability (each probability in (0, 1])
     */
    public UncertainTransaction(int transactionId, Map<Integer, Double> itemProbabilities) {
        this.transactionId = transactionId;
        this.itemProbabilities = new LinkedHashMap<>(itemProbabilities);
    }

    /**
     * Returns the transaction ID.
     *
     * @return transaction ID
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Returns the probability of a given item in this transaction.
     * Returns 0.0 if the item does not appear in this transaction.
     *
     * @param item the item to look up
     * @return the existential probability, or 0.0 if absent
     */
    public double getProbability(int item) {
        return itemProbabilities.getOrDefault(item, 0.0);
    }

    /**
     * Checks whether a given item appears in this transaction (probability > 0).
     *
     * @param item the item to check
     * @return true if the item exists in this transaction
     */
    public boolean containsItem(int item) {
        return itemProbabilities.containsKey(item);
    }

    /**
     * Returns the set of all items in this transaction.
     *
     * @return unmodifiable set of item IDs
     */
    public Set<Integer> getItems() {
        return Collections.unmodifiableSet(itemProbabilities.keySet());
    }

    /**
     * Returns the number of items in this transaction.
     *
     * @return number of items
     */
    public int size() {
        return itemProbabilities.size();
    }

    /**
     * Computes the expected support (probability) of a given itemset
     * within this single transaction.
     *
     * The expected support is the product of the existential probabilities
     * of all items in the itemset within this transaction.
     * If any item is not present in this transaction, returns 0.0.
     *
     * Example: expSup({1,3}, t1) where t1 = {1(0.8), 3(0.6), 4(0.9)}
     *          = P(1) × P(3) = 0.8 × 0.6 = 0.48
     *
     * @param itemset array of item IDs
     * @return the expected support of the itemset in this transaction
     */
    public double computeExpectedSupport(int[] itemset) {
        double product = 1.0;
        for (int item : itemset) {
            double prob = itemProbabilities.getOrDefault(item, 0.0);
            if (prob == 0.0) {
                return 0.0; // Item not in this transaction
            }
            product *= prob;
        }
        return product;
    }

    /**
     * Returns a string representation of this transaction.
     * Format: "t{id}: {item1(prob1), item2(prob2), ...}"
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("t").append(transactionId).append(": {");
        boolean first = true;
        for (Map.Entry<Integer, Double> entry : itemProbabilities.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("(").append(String.format("%.2f", entry.getValue())).append(")");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}