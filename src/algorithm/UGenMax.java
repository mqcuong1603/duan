package algorithm;

import algorithm.genmax.WeightedTidset;
import model.UncertainDatabase;
import model.UncertainTransaction;
import model.Itemset;
import model.TopKHeap;

import java.util.*;

/**
 * UGenMax: GenMax algorithm adapted for uncertain transaction databases.
 *
 * This algorithm discovers frequent maximal itemsets from an uncertain
 * transaction database where each item has an existential probability.
 * It extends the GenMax algorithm (Gouda and Zaki, 2005) by:
 *   1. Using weighted tidsets (transaction ID + probability product pairs)
 *   2. Computing expected support via probability products
 *   3. Supporting Top-K mode with dynamic threshold raising
 *
 * ===== BRANCH-AND-BOUND STRATEGY =====
 *
 * BRANCH: Depth-first backtracking through the itemset enumeration tree.
 *   Each node represents a prefix itemset P. Children are formed by adding
 *   one candidate item to P. Candidates are ordered so each combination
 *   is explored exactly once (no duplicates).
 *
 * BOUND (three pruning rules):
 *
 *   Pruning 1 — Anti-monotonicity:
 *     If expSup(P ∪ {item}) < minsup, prune the entire subtree.
 *     Rationale: Adding more items can only decrease expected support
 *     (each probability is ≤ 1), so no superset can become frequent.
 *     This is the primary bound derived from the mathematical property:
 *       expSup(X ∪ {y}) = Σ_t [P(X,t) × P(y,t)] ≤ Σ_t P(X,t) = expSup(X)
 *
 *   Pruning 2 — Progressive Focusing (superset checking):
 *     If P ∪ {all remaining candidates} ⊆ some known maximal itemset M,
 *     then no new maximal itemset can be found in this subtree.
 *     Rationale: Any itemset we could discover here would be a subset
 *     of M, and therefore not maximal.
 *
 *   Pruning 3 — Top-K Threshold Raising (dynamic bound):
 *     In Top-K mode, minsup starts at 0 and increases dynamically as
 *     the K-th best itemset improves. This progressively strengthens
 *     Pruning 1, creating a feedback loop: better results → higher
 *     threshold → more pruning → faster convergence.
 *
 * ===== ALGORITHM STEPS =====
 *
 *   1. Build weighted tidsets for each single item
 *   2. Remove infrequent items (expSup < minsup)
 *   3. Sort items by expected support ascending (better pruning)
 *   4. Depth-first backtracking search with three pruning rules
 *   5. At each leaf/dead-end, check maximality against known MFIs
 *   6. Return all maximal frequent itemsets (or Top-K results)
 *
 * @author [Your Name]
 */
public class UGenMax {

    /** The uncertain transaction database */
    private final UncertainDatabase database;

    /** Minimum expected support threshold (may increase in Top-K mode) */
    private double minsup;

    /** Number of top results to keep (-1 = use static minsup) */
    private final int topK;

    /** Heap for Top-K mode */
    private TopKHeap topKHeap;

    /** Set of discovered maximal frequent itemsets (for maximality checking) */
    private List<Itemset> maximalItemsets;

    /** Weighted tidsets for each single item: item -> WeightedTidset */
    private Map<Integer, WeightedTidset> itemTidsets;

    /** Frequent items sorted by expected support ascending */
    private List<Integer> frequentItems;

    // --- Statistics counters ---
    private int nodesExplored = 0;
    private int pruneAntiMonotone = 0;
    private int pruneProgressiveFocus = 0;

    /**
     * Creates a new UGenMax instance.
     *
     * @param database the uncertain transaction database
     * @param minsup minimum expected support threshold (0.0 for Top-K mode)
     * @param topK number of top results (-1 for static minsup mode)
     */
    public UGenMax(UncertainDatabase database, double minsup, int topK) {
        this.database = database;
        this.minsup = minsup;
        this.topK = topK;
        this.maximalItemsets = new ArrayList<>();

        if (topK > 0) {
            this.topKHeap = new TopKHeap(topK);
        }
    }

    /**
     * Runs the UGenMax algorithm and returns the discovered maximal frequent itemsets.
     *
     * @return list of maximal frequent itemsets found
     */
    public List<Itemset> run() {
        // --- Step 1: Build weighted tidsets for each item ---
        System.out.println("  [UGenMax] Building weighted tidsets...");
        buildWeightedTidsets();

        // --- Step 2: Top-K initialization ---
        // In Top-K mode, we seed the heap with single-item and 2-item frequent
        // itemsets to establish a reasonable initial minsup threshold.
        // Without this, minsup=0 means ALL items are frequent, leading to
        // a single giant itemset with expSup≈0 that subsumes everything.
        if (topK > 0 && topKHeap != null) {
            System.out.println("  [UGenMax] Top-K mode: seeding initial threshold...");
            seedTopKHeap();
            double seededThreshold = topKHeap.getCurrentThreshold();
            System.out.println("  [UGenMax] Seeded threshold: "
                    + String.format("%.4f", seededThreshold));

            // Use a fraction of the seeded threshold as starting minsup.
            // The seeded threshold represents the K-th best score found from
            // singles and pairs only. The actual search may find multi-item
            // maximal itemsets with lower support that still deserve Top-K.
            // Using 50% ensures enough items remain frequent to explore
            // deeper combinations, while still providing meaningful pruning.
            // The threshold will rise dynamically during search as better
            // results are found (Pruning 3).
            minsup = seededThreshold * 0.5;
            System.out.println("  [UGenMax] Initial minsup (50% of seeded): "
                    + String.format("%.4f", minsup));
        }

        // --- Step 3 & 4: Filter items and run search ---
        // In Top-K mode, if fewer than K results found, retry with lower threshold
        int maxRetries = (topK > 0) ? 3 : 0;
        int attempt = 0;

        do {
            // --- Step 3: Get frequent items (expSup >= minsup), sorted ascending ---
            if (attempt > 0) {
                // Lower threshold and reset for retry
                minsup = minsup * 0.25; // Quarter the threshold
                maximalItemsets.clear();
                nodesExplored = 0;
                pruneAntiMonotone = 0;
                pruneProgressiveFocus = 0;
                System.out.println("  [UGenMax] Retrying with lower minsup: "
                        + String.format("%.4f", minsup));
            }

            System.out.println("  [UGenMax] Filtering frequent items (minsup="
                    + String.format("%.4f", minsup) + ")...");
            frequentItems = new ArrayList<>();
            for (Map.Entry<Integer, WeightedTidset> entry : itemTidsets.entrySet()) {
                if (entry.getValue().getExpectedSupport() >= minsup) {
                    frequentItems.add(entry.getKey());
                }
            }

            // Sort by expected support ASCENDING (least frequent first)
            frequentItems.sort((a, b) ->
                    Double.compare(itemTidsets.get(a).getExpectedSupport(),
                            itemTidsets.get(b).getExpectedSupport()));

            System.out.println("  [UGenMax] Frequent items: " + frequentItems.size()
                    + " (out of " + database.getItemCount() + " distinct items)");

            if (frequentItems.isEmpty()) {
                System.out.println("  [UGenMax] No frequent items found.");
                attempt++;
                continue;
            }

            // --- Step 4: Depth-first backtracking search ---
            System.out.println("  [UGenMax] Starting depth-first search...");
            genmax(new int[0], new ArrayList<>(frequentItems), null);

            attempt++;

        } while (topK > 0 && maximalItemsets.size() < topK && attempt <= maxRetries);

        // --- Step 5: Print statistics and return results ---
        System.out.println("  [UGenMax] Search complete.");
        System.out.println("  [UGenMax]   Nodes explored: " + nodesExplored);
        System.out.println("  [UGenMax]   Pruned (anti-monotone): " + pruneAntiMonotone);
        System.out.println("  [UGenMax]   Pruned (progressive focus): " + pruneProgressiveFocus);
        System.out.println("  [UGenMax]   Maximal itemsets found: " + maximalItemsets.size());
        if (topK > 0) {
            System.out.println("  [UGenMax]   Final minsup threshold: "
                    + String.format("%.4f", minsup));
        }

        if (topK > 0) {
            // Rebuild heap from scratch using only actual maximal itemsets
            // This eliminates any duplicates from the seeding phase
            TopKHeap finalHeap = new TopKHeap(topK);
            maximalItemsets.sort((a, b) -> Double.compare(
                    b.getExpectedSupport(), a.getExpectedSupport()));
            for (Itemset mfi : maximalItemsets) {
                finalHeap.offer(mfi);
            }
            return finalHeap.getResults();
        }
        return new ArrayList<>(maximalItemsets);
    }

    /**
     * Seeds the Top-K heap with frequent 2-item itemsets to establish a
     * reasonable initial minsup threshold for Top-K mode.
     *
     * Without seeding, minsup starts at 0.0, making ALL items frequent.
     * The search then builds an enormous itemset with near-zero expected
     * support, which subsumes everything and prevents the heap from filling.
     *
     * Strategy:
     *   1. Sort items by expected support descending
     *   2. Take the top items (most frequent ones)
     *   3. Generate all 2-item combinations among them
     *   4. Add each frequent pair to the heap
     *   5. The heap's minimum becomes the initial minsup
     *
     * This is similar to a breadth-first "warm-up" phase before depth-first search.
     */
    private void seedTopKHeap() {
        // Get all items sorted by expSup descending
        List<Integer> allItems = new ArrayList<>(itemTidsets.keySet());
        allItems.sort((a, b) -> Double.compare(
                itemTidsets.get(b).getExpectedSupport(),
                itemTidsets.get(a).getExpectedSupport()));

        // Take top items for seeding (limit to avoid O(n^2) explosion)
        int seedLimit = Math.min(allItems.size(), 50);
        List<Integer> topItems = allItems.subList(0, seedLimit);

        // Collect all seed candidates (singles + pairs), then pick the best
        List<Itemset> seedCandidates = new ArrayList<>();

        // Single items
        for (int item : topItems) {
            seedCandidates.add(new Itemset(new int[]{item},
                    itemTidsets.get(item).getExpectedSupport()));
        }

        // 2-item pairs among top items
        for (int i = 0; i < topItems.size(); i++) {
            int itemA = topItems.get(i);
            WeightedTidset tidA = itemTidsets.get(itemA);

            for (int j = i + 1; j < topItems.size(); j++) {
                int itemB = topItems.get(j);
                WeightedTidset tidAB = tidA.intersect(itemTidsets.get(itemB));
                double expSup = tidAB.getExpectedSupport();
                if (expSup > 0) {
                    seedCandidates.add(new Itemset(new int[]{itemA, itemB}, expSup));
                }
            }
        }

        // Sort by expSup descending, add top ones to heap
        seedCandidates.sort((a, b) -> Double.compare(b.getExpectedSupport(), a.getExpectedSupport()));

        // Use a set to track what's already in the heap (prevent duplicates)
        Set<Itemset> seenItemsets = new HashSet<>();

        for (Itemset candidate : seedCandidates) {
            if (seenItemsets.contains(candidate)) continue;

            if (topKHeap.offer(candidate)) {
                seenItemsets.add(candidate);
            }

            // Once heap is full and threshold is established, stop early for efficiency
            if (topKHeap.isFull() && seedCandidates.indexOf(candidate) > topK * 3) {
                break;
            }
        }

        // IMPORTANT: Do NOT add seeded items to maximalItemsets here.
        // The seeded items are just for establishing the threshold.
        // The main search will find the actual maximal itemsets properly,
        // and they may differ (seeded singles may not be maximal if they
        // have frequent supersets).
    }

    /**
     * Builds weighted tidsets for each single item in the database.
     *
     * Scans all transactions once, and for each item occurrence, records
     * (transactionId, probability) in the item's weighted tidset.
     *
     * Since transactions are processed in order (ID 0, 1, 2, ...),
     * entries are automatically sorted by transaction ID.
     */
    private void buildWeightedTidsets() {
        itemTidsets = new HashMap<>();

        // Scan each transaction
        for (UncertainTransaction t : database.getTransactions()) {
            // For each item in this transaction
            for (int item : t.getItems()) {
                double prob = t.getProbability(item);
                if (prob > 0) {
                    // Add (transactionId, probability) to the item's tidset
                    itemTidsets.computeIfAbsent(item, k -> new WeightedTidset())
                            .add(t.getTransactionId(), prob);
                }
            }
        }
    }

    /**
     * Recursive depth-first backtracking search for maximal frequent itemsets.
     *
     * This is the core of the GenMax algorithm adapted for uncertain data.
     *
     * The enumeration tree is explored depth-first. At each node:
     *   prefix = items already selected
     *   candidates = items that can be added (only items with index < current)
     *   prefixTidset = weighted tidset of the prefix
     *
     * We iterate candidates from end to beginning (most frequent first in our
     * ascending-sorted list), which tends to find large maximal itemsets early.
     *
     * @param prefix the current prefix itemset (items already selected)
     * @param candidates list of candidate items that can extend the prefix
     * @param prefixTidset weighted tidset of the prefix (null if prefix is empty)
     */
    private void genmax(int[] prefix, List<Integer> candidates, WeightedTidset prefixTidset) {
        nodesExplored++;

        // --- PRUNING 2: Progressive Focusing (check before expanding) ---
        // If prefix ∪ all candidates ⊆ some known MFI, prune entire subtree
        if (!maximalItemsets.isEmpty() && prefix.length > 0) {
            int[] potentialFull = buildUnion(prefix, candidates);
            if (isSubsetOfAnyMFI(potentialFull)) {
                pruneProgressiveFocus++;
                return;
            }
        }

        // Track whether any extension was successful (for maximality check)
        boolean hasFrequentExtension = false;

        // Process candidates from end to beginning
        // (most frequent items first → larger maximal itemsets found earlier)
        for (int idx = candidates.size() - 1; idx >= 0; idx--) {
            int item = candidates.get(idx);

            // --- Compute tidset for prefix ∪ {item} ---
            WeightedTidset newTidset;
            if (prefixTidset == null) {
                // Prefix is empty: tidset is just the item's own tidset
                newTidset = itemTidsets.get(item);
            } else {
                // Intersect prefix's tidset with item's tidset
                newTidset = prefixTidset.intersect(itemTidsets.get(item));
            }

            double expSup = newTidset.getExpectedSupport();

            // --- PRUNING 1: Anti-monotonicity ---
            // If expSup < minsup, skip this item and all its extensions
            if (expSup < minsup) {
                pruneAntiMonotone++;
                continue;
            }

            hasFrequentExtension = true;

            // Build the new prefix: prefix ∪ {item}
            int[] newPrefix = new int[prefix.length + 1];
            System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
            newPrefix[prefix.length] = item;

            // Build new candidate list: items before idx that remain frequent
            // when combined with the new prefix
            List<Integer> newCandidates = new ArrayList<>();
            for (int j = 0; j < idx; j++) {
                int candItem = candidates.get(j);

                // Quick upper bound check before expensive intersection
                WeightedTidset candTidset = itemTidsets.get(candItem);
                if (newTidset.upperBoundIntersect(candTidset) < minsup) {
                    continue; // Upper bound already below threshold
                }

                // Full intersection to check actual expected support
                WeightedTidset testTidset = newTidset.intersect(candTidset);
                if (testTidset.getExpectedSupport() >= minsup) {
                    newCandidates.add(candItem);
                }
            }

            if (newCandidates.isEmpty()) {
                // No more extensions possible → newPrefix is a leaf node
                // It may be maximal if not subsumed by any known MFI
                addMaximalIfNew(newPrefix, expSup);
            } else {
                // Recurse deeper into the enumeration tree
                genmax(newPrefix, newCandidates, newTidset);
            }
        }

        // If no frequent extension was found for a non-empty prefix,
        // the prefix itself might be maximal
        if (!hasFrequentExtension && prefix.length > 0 && prefixTidset != null) {
            addMaximalIfNew(prefix, prefixTidset.getExpectedSupport());
        }
    }

    /**
     * Adds an itemset to the maximal set if it is not a subset of any existing MFI.
     * Also removes existing MFIs that are subsets of this new one.
     * In Top-K mode, updates the heap and potentially raises the threshold.
     *
     * @param items the itemset to potentially add
     * @param expSup expected support of the itemset
     */
    private void addMaximalIfNew(int[] items, double expSup) {
        // Sort items for consistent comparison
        int[] sortedItems = items.clone();
        Arrays.sort(sortedItems);
        Itemset candidate = new Itemset(sortedItems, expSup);

        // Check if this itemset is subsumed by any existing MFI
        for (Itemset mfi : maximalItemsets) {
            if (candidate.isSubsetOf(mfi)) {
                return; // Already covered by a known maximal itemset
            }
        }

        // Remove any existing MFIs that are subsets of this new one
        // (they are no longer maximal)
        maximalItemsets.removeIf(mfi -> mfi.isSubsetOf(candidate));

        // Add to maximal set
        maximalItemsets.add(candidate);

        // Top-K mode: update heap and potentially raise minsup
        if (topK > 0 && topKHeap != null) {
            topKHeap.offer(candidate);
            double newThreshold = topKHeap.getCurrentThreshold();
            if (newThreshold > minsup) {
                minsup = newThreshold;
                // PRUNING 3: Dynamic threshold raising
                // From now on, Pruning 1 will be more aggressive
                // because minsup has increased
            }
        }
    }

    /**
     * Checks if a given itemset is a subset of any known maximal frequent itemset.
     * Used for progressive focusing pruning.
     *
     * @param items the itemset to check (as sorted int array)
     * @return true if it is a subset of some existing MFI
     */
    private boolean isSubsetOfAnyMFI(int[] items) {
        int[] sorted = items.clone();
        Arrays.sort(sorted);
        Itemset candidate = new Itemset(sorted);
        for (Itemset mfi : maximalItemsets) {
            if (candidate.isSubsetOf(mfi)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the sorted union of a prefix (int array) and a list of candidate items.
     *
     * @param prefix the current prefix
     * @param candidates the candidate items
     * @return sorted array containing all items from prefix and candidates
     */
    private int[] buildUnion(int[] prefix, List<Integer> candidates) {
        int[] union = new int[prefix.length + candidates.size()];
        System.arraycopy(prefix, 0, union, 0, prefix.length);
        for (int i = 0; i < candidates.size(); i++) {
            union[prefix.length + i] = candidates.get(i);
        }
        Arrays.sort(union);
        return union;
    }
}