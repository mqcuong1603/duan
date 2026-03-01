package model;

import java.util.Arrays;

/**
 * Represents an itemset (a set of items) along with its expected support.
 *
 * An itemset is an unordered set of distinct items. In the context of
 * uncertain databases, each itemset has an expected support value
 * computed as the sum of probability products across all transactions.
 *
 * Items are stored in sorted order for consistent comparison and hashing.
 *
 * @author [Your Name]
 */
public class Itemset implements Comparable<Itemset> {

    /** Sorted array of item IDs */
    private final int[] items;

    /** Expected support of this itemset in the database */
    private double expectedSupport;

    /**
     * Creates a new itemset with the given items and expected support.
     * Items are automatically sorted.
     *
     * @param items array of item IDs (will be copied and sorted)
     * @param expectedSupport the expected support value
     */
    public Itemset(int[] items, double expectedSupport) {
        this.items = items.clone();
        Arrays.sort(this.items);
        this.expectedSupport = expectedSupport;
    }

    /**
     * Creates a new itemset with unknown expected support (set to 0).
     *
     * @param items array of item IDs
     */
    public Itemset(int[] items) {
        this(items, 0.0);
    }

    /**
     * Returns the sorted array of items.
     *
     * @return copy of the items array
     */
    public int[] getItems() {
        return items.clone();
    }

    /**
     * Returns the number of items in this itemset.
     *
     * @return size of the itemset
     */
    public int size() {
        return items.length;
    }

    /**
     * Returns the expected support of this itemset.
     *
     * @return expected support value
     */
    public double getExpectedSupport() {
        return expectedSupport;
    }

    /**
     * Sets the expected support of this itemset.
     *
     * @param expectedSupport the new expected support value
     */
    public void setExpectedSupport(double expectedSupport) {
        this.expectedSupport = expectedSupport;
    }

    /**
     * Checks if this itemset contains all items of another itemset.
     * In other words, checks if 'other' is a subset of 'this'.
     *
     * Uses the fact that both arrays are sorted for O(n+m) comparison.
     *
     * @param other the other itemset to check
     * @return true if this itemset is a superset of (or equal to) other
     */
    public boolean containsAll(Itemset other) {
        int i = 0, j = 0;
        while (i < this.items.length && j < other.items.length) {
            if (this.items[i] == other.items[j]) {
                i++;
                j++;
            } else if (this.items[i] < other.items[j]) {
                i++;
            } else {
                return false; // other has an item we don't have
            }
        }
        return j == other.items.length;
    }

    /**
     * Checks if this itemset is a subset of another itemset.
     *
     * @param other the potential superset
     * @return true if this is a subset of other
     */
    public boolean isSubsetOf(Itemset other) {
        return other.containsAll(this);
    }

    /**
     * Creates a new itemset that is the union of this itemset and another item.
     *
     * @param item the item to add
     * @return new itemset containing all items of this + the new item
     */
    public Itemset unionWith(int item) {
        // Check if item already exists
        for (int i : items) {
            if (i == item) return this;
        }

        int[] newItems = new int[items.length + 1];
        System.arraycopy(items, 0, newItems, 0, items.length);
        newItems[items.length] = item;
        return new Itemset(newItems);
    }

    /**
     * Creates a new itemset that is the union of this itemset and another itemset.
     *
     * @param other the other itemset
     * @return new itemset containing all items from both
     */
    public Itemset unionWith(Itemset other) {
        // Merge two sorted arrays, removing duplicates
        int[] merged = new int[this.items.length + other.items.length];
        int i = 0, j = 0, k = 0;

        while (i < this.items.length && j < other.items.length) {
            if (this.items[i] == other.items[j]) {
                merged[k++] = this.items[i];
                i++;
                j++;
            } else if (this.items[i] < other.items[j]) {
                merged[k++] = this.items[i++];
            } else {
                merged[k++] = other.items[j++];
            }
        }
        while (i < this.items.length) merged[k++] = this.items[i++];
        while (j < other.items.length) merged[k++] = other.items[j++];

        return new Itemset(Arrays.copyOf(merged, k));
    }

    /**
     * Compares itemsets by expected support (descending), then by size (descending).
     * Used for sorting results.
     */
    @Override
    public int compareTo(Itemset other) {
        int cmp = Double.compare(other.expectedSupport, this.expectedSupport);
        if (cmp != 0) return cmp;
        return Integer.compare(other.items.length, this.items.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Itemset)) return false;
        return Arrays.equals(this.items, ((Itemset) obj).items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    /**
     * Returns a string representation.
     * Format: "{item1, item2, ...} (expSup=X.XXXX)"
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(items[i]);
        }
        sb.append("} (expSup=").append(String.format("%.4f", expectedSupport)).append(")");
        return sb.toString();
    }
}