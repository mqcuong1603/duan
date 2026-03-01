package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A min-heap that maintains the Top-K maximal frequent itemsets
 * with the highest expected support values.
 *
 * This is the core data structure for the Top-K approach. It enables
 * dynamic threshold raising: as better itemsets are discovered, the
 * minimum expected support in the heap increases, which allows the
 * mining algorithm to prune more aggressively.
 *
 * The heap stores at most K itemsets. When a new itemset is inserted
 * and the heap is full, the itemset with the lowest expected support
 * is removed if the new one is better.
 *
 * @author [Your Name]
 */
public class TopKHeap {

    /** Maximum number of itemsets to keep */
    private final int k;

    /** Min-heap: the itemset with the LOWEST expected support is at the top */
    private final PriorityQueue<Itemset> heap;

    /**
     * Creates a new TopKHeap with the given capacity.
     *
     * @param k the maximum number of itemsets to maintain
     */
    public TopKHeap(int k) {
        this.k = k;
        // Min-heap: smallest expected support at the top
        this.heap = new PriorityQueue<>((a, b) ->
                Double.compare(a.getExpectedSupport(), b.getExpectedSupport()));
    }

    /**
     * Attempts to add an itemset to the Top-K heap.
     *
     * If the heap has fewer than K items, the itemset is added directly.
     * If the heap is full and the new itemset has higher expected support
     * than the current minimum, the minimum is replaced.
     *
     * Returns true if the itemset was added (and the threshold may have changed).
     *
     * @param itemset the itemset to add
     * @return true if the itemset was added to the heap
     */
    public boolean offer(Itemset itemset) {
        if (heap.size() < k) {
            heap.offer(itemset);
            return true;
        } else if (itemset.getExpectedSupport() > heap.peek().getExpectedSupport()) {
            heap.poll();       // Remove the smallest
            heap.offer(itemset); // Add the new one
            return true;
        }
        return false;
    }

    /**
     * Returns the current dynamic minimum support threshold.
     *
     * This is the expected support of the itemset at the top of the min-heap.
     * If the heap has fewer than K items, returns 0.0 (accept everything).
     *
     * The mining algorithm should use this value as its minsup threshold,
     * checking it frequently to take advantage of threshold raising.
     *
     * @return current minimum expected support threshold
     */
    public double getCurrentThreshold() {
        if (heap.size() < k) {
            return 0.0;
        }
        return heap.peek().getExpectedSupport();
    }

    /**
     * Returns the number of itemsets currently in the heap.
     *
     * @return current size
     */
    public int size() {
        return heap.size();
    }

    /**
     * Checks if the heap is full (has K itemsets).
     *
     * @return true if size == K
     */
    public boolean isFull() {
        return heap.size() >= k;
    }

    /**
     * Returns all itemsets in the heap, sorted by expected support descending.
     *
     * @return sorted list of Top-K itemsets
     */
    public List<Itemset> getResults() {
        List<Itemset> results = new ArrayList<>(heap);
        Collections.sort(results); // Uses Itemset.compareTo (descending by expSup)
        return results;
    }

    /**
     * Returns a string representation of the heap contents.
     *
     * @return string showing all itemsets in the heap
     */
    @Override
    public String toString() {
        List<Itemset> sorted = getResults();
        StringBuilder sb = new StringBuilder("TopKHeap (k=").append(k);
        sb.append(", size=").append(heap.size());
        sb.append(", threshold=").append(String.format("%.4f", getCurrentThreshold()));
        sb.append("):\n");
        for (Itemset item : sorted) {
            sb.append("  ").append(item).append("\n");
        }
        return sb.toString();
    }
}