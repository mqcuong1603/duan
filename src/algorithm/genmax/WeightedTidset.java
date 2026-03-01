package algorithm.genmax;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a weighted tidset for uncertain transaction databases.
 *
 * In standard (binary) databases, a tidset is simply the set of transaction IDs
 * where an item/itemset appears. In uncertain databases, each entry also carries
 * the probability product — the expected support contribution from that transaction.
 *
 * Example for item A with probabilities:
 *   t1: A(0.8), t3: A(0.6), t5: A(0.9)
 *   → WeightedTidset = [(t1, 0.8), (t3, 0.6), (t5, 0.9)]
 *   → expectedSupport = 0.8 + 0.6 + 0.9 = 2.3
 *
 * For an itemset {A, B}:
 *   Intersect weighted tidsets of A and B:
 *   → For each common transaction, multiply probabilities
 *   → expSup({A,B}) = sum of products
 *
 * Entries are kept sorted by transaction ID for efficient O(n+m) intersection.
 *
 * @author [Your Name]
 */
public class WeightedTidset {

    /** List of (transactionId, probabilityProduct) pairs, sorted by transactionId */
    private final List<Entry> entries;

    /** Cached expected support (sum of all probability products) */
    private double expectedSupport;

    /**
     * A single entry: one transaction's contribution to the expected support.
     */
    public static class Entry {
        public final int transactionId;
        public final double probability;

        public Entry(int transactionId, double probability) {
            this.transactionId = transactionId;
            this.probability = probability;
        }
    }

    /**
     * Creates an empty weighted tidset.
     */
    public WeightedTidset() {
        this.entries = new ArrayList<>();
        this.expectedSupport = 0.0;
    }

    /**
     * Creates a weighted tidset with pre-allocated capacity.
     *
     * @param initialCapacity estimated number of entries
     */
    public WeightedTidset(int initialCapacity) {
        this.entries = new ArrayList<>(initialCapacity);
        this.expectedSupport = 0.0;
    }

    /**
     * Adds an entry to this weighted tidset.
     * Entries must be added in sorted order by transactionId.
     *
     * @param transactionId the transaction ID
     * @param probability the probability product for this transaction
     */
    public void add(int transactionId, double probability) {
        entries.add(new Entry(transactionId, probability));
        expectedSupport += probability;
    }

    /**
     * Returns the expected support (sum of all probability products).
     *
     * @return expected support value
     */
    public double getExpectedSupport() {
        return expectedSupport;
    }

    /**
     * Returns the number of transactions in this tidset.
     *
     * @return number of entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns the list of entries.
     *
     * @return list of (transactionId, probability) pairs
     */
    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * Intersects this weighted tidset with another, producing a new weighted tidset
     * for the combined itemset.
     *
     * For each transaction that appears in BOTH tidsets, the new probability
     * is the product of the two probabilities (items are independent).
     *
     * Uses merge-join on sorted transaction IDs → O(n + m) time complexity.
     *
     * Example:
     *   tidset(A) = [(t1, 0.8), (t3, 0.6), (t5, 0.9)]
     *   tidset(B) = [(t2, 0.7), (t3, 0.8), (t5, 0.75)]
     *
     *   intersect(A, B) = [(t3, 0.6*0.8), (t5, 0.9*0.75)]
     *                   = [(t3, 0.48), (t5, 0.675)]
     *   expSup({A,B}) = 0.48 + 0.675 = 1.155
     *
     * @param other the other weighted tidset to intersect with
     * @return new weighted tidset representing the intersection
     */
    public WeightedTidset intersect(WeightedTidset other) {
        // Pre-allocate with smaller size as upper bound
        WeightedTidset result = new WeightedTidset(Math.min(this.entries.size(), other.entries.size()));

        int i = 0, j = 0;
        int thisSize = this.entries.size();
        int otherSize = other.entries.size();

        // Step 1: Merge-join — walk both sorted lists simultaneously
        while (i < thisSize && j < otherSize) {
            Entry a = this.entries.get(i);
            Entry b = other.entries.get(j);

            if (a.transactionId == b.transactionId) {
                // Step 2: Common transaction found — multiply probabilities
                // This implements P(A∩B) = P(A) * P(B) assuming independence
                double product = a.probability * b.probability;
                if (product > 0.0) {
                    result.add(a.transactionId, product);
                }
                i++;
                j++;
            } else if (a.transactionId < b.transactionId) {
                // Step 3: Advance the pointer with smaller transaction ID
                i++;
            } else {
                j++;
            }
        }

        return result;
    }

    /**8
     * Computes an upper bound on the expected support of the intersection
     * of this tidset with another, without actually computing the full intersection.
     *
     * Upper bound: min(this.expSup, other.expSup)
     * This is valid because intersection can only reduce expected support.
     *
     * Used for early pruning — if the upper bound < minsup, we can skip
     * the expensive intersection computation.
     *
     * @param other the other weighted tidset
     * @return upper bound on the intersection's expected support
     */
    public double upperBoundIntersect(WeightedTidset other) {
        return Math.min(this.expectedSupport, other.expectedSupport);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(entries.size(), 5); i++) {
            if (i > 0) sb.append(", ");
            Entry e = entries.get(i);
            sb.append("(t").append(e.transactionId)
                    .append(", ").append(String.format("%.4f", e.probability)).append(")");
        }
        if (entries.size() > 5) sb.append(", ... (").append(entries.size()).append(" total)");
        sb.append("] expSup=").append(String.format("%.4f", expectedSupport));
        return sb.toString();
    }
}