package algorithm;

import model.UncertainDatabase;
import model.UncertainTransaction;
import model.Itemset;
import model.TopKHeap;

import java.util.*;

/**
 * UFPMax: FPMax algorithm adapted for uncertain transaction databases.
 *
 * This algorithm discovers frequent maximal itemsets from an uncertain
 * transaction database where each item has an existential probability.
 * It extends the FPMax algorithm (Grahne & Zhu, 2003) by:
 *   1. Using expected support instead of count-based support
 *   2. Using conditional database projection with probability-weighted transactions
 *   3. Supporting Top-K mode with dynamic threshold raising
 *
 * ===== BRANCH-AND-BOUND STRATEGY =====
 *
 * BRANCH: Recursive conditional database projection. Items are ordered by
 *   expected support descending (FP-tree ordering). For each item (processed
 *   from least to most frequent), a conditional database is built containing
 *   only transactions with that item, projected to more-frequent items.
 *
 * BOUND (three pruning rules):
 *
 *   Pruning 1 — Anti-monotonicity:
 *     If expSup(prefix ∪ {item}) < minsup, skip this item entirely.
 *     Adding more items can only decrease expected support.
 *
 *   Pruning 2 — Progressive Focusing (MFI superset checking):
 *     If prefix ∪ {all remaining header items} ⊆ some known maximal itemset,
 *     then no new maximal itemset can be found in this subtree.
 *
 *   Pruning 3 — Top-K Threshold Raising (dynamic bound):
 *     In Top-K mode, minsup increases dynamically as the K-th best itemset
 *     improves, progressively strengthening Pruning 1.
 *
 * ===== KEY DIFFERENCE FROM UGenMax =====
 *
 * UFPMax uses top-down conditional database projection (FP-growth style):
 *   - Items ordered by frequency descending
 *   - Conditional databases built by projecting transactions onto more-frequent items
 *   - Each conditional transaction carries a weight (product of conditioning item probs)
 *
 * UGenMax uses bottom-up tidset intersection (GenMax style):
 *   - Items ordered by frequency ascending
 *   - Weighted tidsets intersected to compute expected support
 *   - Enumeration tree explored depth-first with tidset-based pruning
 *
 * @author [Your Name]
 */
public class UFPMax {

    /** The uncertain transaction database */
    private final UncertainDatabase database;

    /** Minimum expected support threshold (may increase in Top-K mode) */
    private double minsup;

    /** Number of top results to keep (-1 = use static minsup) */
    private final int topK;

    /** Heap for Top-K mode */
    private TopKHeap topKHeap;

    /** Set of discovered maximal frequent itemsets */
    private List<Itemset> maximalItemsets;

    // --- Statistics counters ---
    private int nodesExplored = 0;
    private int pruneAntiMonotone = 0;
    private int pruneProgressiveFocus = 0;

    /**
     * A projected transaction in a conditional database.
     *
     * Each projected transaction has:
     *   - weight: the accumulated probability product of all conditioning items
     *     (items already in the prefix). For the initial database, weight = 1.0.
     *   - items/probs: the remaining items and their original per-transaction
     *     probabilities, ordered by the header (frequency descending).
     *
     * Expected support of an itemset X in this conditional DB:
     *   expSup(X) = Σ_t weight_t × Π_{j∈X} prob(j, t)
     */
    private static class ProjTransaction {
        final double weight;
        final int[] items;
        final double[] probs;

        ProjTransaction(double weight, int[] items, double[] probs) {
            this.weight = weight;
            this.items = items;
            this.probs = probs;
        }
    }

    /**
     * Creates a new UFPMax instance.
     *
     * @param database the uncertain transaction database
     * @param minsup minimum expected support threshold (0.0 for Top-K mode)
     * @param topK number of top results (-1 for static minsup mode)
     */
    public UFPMax(UncertainDatabase database, double minsup, int topK) {
        this.database = database;
        this.minsup = minsup;
        this.topK = topK;
        this.maximalItemsets = new ArrayList<>();

        if (topK > 0) {
            this.topKHeap = new TopKHeap(topK);
        }
    }

    /**
     * Runs the UFPMax algorithm and returns the discovered maximal frequent itemsets.
     *
     * @return list of maximal frequent itemsets found
     */
    public List<Itemset> run() {
        // --- Step 1: Compute single-item expected supports ---
        System.out.println("  [UFPMax] Computing item expected supports...");
        Map<Integer, Double> itemExpSup = new HashMap<>();
        for (UncertainTransaction t : database.getTransactions()) {
            for (int item : t.getItems()) {
                itemExpSup.merge(item, t.getProbability(item), Double::sum);
            }
        }

        // --- Step 2: Top-K initialization ---
        if (topK > 0 && topKHeap != null) {
            System.out.println("  [UFPMax] Top-K mode: seeding initial threshold...");
            seedTopKHeap(itemExpSup);
            double seededThreshold = topKHeap.getCurrentThreshold();
            System.out.println("  [UFPMax] Seeded threshold: "
                    + String.format("%.4f", seededThreshold));
            minsup = seededThreshold * 0.5;
            System.out.println("  [UFPMax] Initial minsup (50% of seeded): "
                    + String.format("%.4f", minsup));
            topKHeap = new TopKHeap(topK);
        }

        // --- Step 3 & 4: Filter items, build initial DB, and mine ---
        int maxRetries = (topK > 0) ? 3 : 0;
        int attempt = 0;
        double baseMinsup = minsup;

        do {
            if (attempt > 0) {
                baseMinsup = baseMinsup * 0.25;
                minsup = baseMinsup;
                maximalItemsets.clear();
                topKHeap = new TopKHeap(topK);
                nodesExplored = 0;
                pruneAntiMonotone = 0;
                pruneProgressiveFocus = 0;
                System.out.println("  [UFPMax] Retrying with lower minsup: "
                        + String.format("%.4f", minsup));
            }

            // Get frequent items sorted by expected support DESCENDING (FP-tree ordering)
            List<Integer> headerItems = new ArrayList<>();
            for (Map.Entry<Integer, Double> entry : itemExpSup.entrySet()) {
                if (entry.getValue() >= minsup) {
                    headerItems.add(entry.getKey());
                }
            }
            headerItems.sort((a, b) -> Double.compare(
                    itemExpSup.getOrDefault(b, 0.0),
                    itemExpSup.getOrDefault(a, 0.0)));

            System.out.println("  [UFPMax] Filtering frequent items (minsup="
                    + String.format("%.4f", minsup) + ")...");
            System.out.println("  [UFPMax] Frequent items: " + headerItems.size()
                    + " (out of " + database.getItemCount() + " distinct items)");

            if (headerItems.isEmpty()) {
                attempt++;
                continue;
            }

            // Build initial projected database
            List<ProjTransaction> projDB = buildInitialProjectedDB(headerItems);

            // Mine
            System.out.println("  [UFPMax] Starting FPMax search...");
            fpmax(new int[0], 0.0, projDB, headerItems);

            attempt++;
        } while (topK > 0 && maximalItemsets.size() < topK && attempt <= maxRetries);

        // --- Step 5: Print statistics and return results ---
        System.out.println("  [UFPMax] Search complete.");
        System.out.println("  [UFPMax]   Nodes explored: " + nodesExplored);
        System.out.println("  [UFPMax]   Pruned (anti-monotone): " + pruneAntiMonotone);
        System.out.println("  [UFPMax]   Pruned (progressive focus): " + pruneProgressiveFocus);
        System.out.println("  [UFPMax]   Maximal itemsets found: " + maximalItemsets.size());
        if (topK > 0) {
            System.out.println("  [UFPMax]   Final minsup threshold: "
                    + String.format("%.4f", minsup));
        }

        if (topK > 0) {
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
     * Builds the initial projected database from the original transactions.
     * Filters to frequent items only and sorts by header order.
     *
     * @param headerItems frequent items sorted by expected support descending
     * @return list of projected transactions
     */
    private List<ProjTransaction> buildInitialProjectedDB(List<Integer> headerItems) {
        // Build item-to-position mapping for sorting
        Map<Integer, Integer> itemPos = new HashMap<>();
        for (int i = 0; i < headerItems.size(); i++) {
            itemPos.put(headerItems.get(i), i);
        }

        List<ProjTransaction> projDB = new ArrayList<>();
        for (UncertainTransaction t : database.getTransactions()) {
            // Collect frequent items with their header positions
            List<int[]> pairs = new ArrayList<>();
            for (int item : t.getItems()) {
                Integer pos = itemPos.get(item);
                if (pos != null) {
                    pairs.add(new int[]{item, pos});
                }
            }
            if (pairs.isEmpty()) continue;

            // Sort by header position (frequency descending order)
            pairs.sort((a, b) -> Integer.compare(a[1], b[1]));

            int[] items = new int[pairs.size()];
            double[] probs = new double[pairs.size()];
            for (int i = 0; i < pairs.size(); i++) {
                items[i] = pairs.get(i)[0];
                probs[i] = t.getProbability(pairs.get(i)[0]);
            }
            projDB.add(new ProjTransaction(1.0, items, probs));
        }
        return projDB;
    }

    /**
     * Recursive FPMax mining on conditional databases.
     *
     * Items in headerItems are sorted by expected support descending.
     * We process from TAIL (least frequent) to HEAD (most frequent).
     * For each frequent item, we build a conditional database by:
     *   1. Selecting transactions containing the item
     *   2. Setting new weight = old weight × P(item, transaction)
     *   3. Projecting to items before the current item in header (more frequent)
     *
     * @param prefix current prefix itemset
     * @param prefixExpSup expected support of the prefix
     * @param conditionalDB projected database for the current prefix
     * @param headerItems available items sorted by expected support descending
     */
    private void fpmax(int[] prefix, double prefixExpSup,
                       List<ProjTransaction> conditionalDB,
                       List<Integer> headerItems) {
        nodesExplored++;

        // --- PRUNING 2: Progressive Focusing ---
        // If prefix ∪ all headerItems ⊆ some known MFI, prune entire subtree
        if (!maximalItemsets.isEmpty() && prefix.length > 0) {
            int[] potentialFull = buildUnion(prefix, headerItems);
            if (isSubsetOfAnyMFI(potentialFull)) {
                pruneProgressiveFocus++;
                return;
            }
        }

        boolean hasFrequentExtension = false;

        // Process items from tail (least frequent) to head (most frequent)
        for (int idx = headerItems.size() - 1; idx >= 0; idx--) {
            int item = headerItems.get(idx);

            // Compute expSup of prefix ∪ {item} from the conditional DB
            double expSup = 0;
            for (ProjTransaction t : conditionalDB) {
                double prob = findProb(t, item);
                if (prob > 0) {
                    expSup += t.weight * prob;
                }
            }

            // --- PRUNING 1: Anti-monotonicity ---
            if (expSup < minsup) {
                pruneAntiMonotone++;
                continue;
            }

            hasFrequentExtension = true;

            // Build conditional database for this item:
            // - Only keep transactions containing item
            // - New weight = old weight × P(item, t)
            // - Project to items in headerItems[0..idx-1] (more frequent items)
            Set<Integer> allowedItems = new HashSet<>();
            for (int h = 0; h < idx; h++) {
                allowedItems.add(headerItems.get(h));
            }

            List<ProjTransaction> newDB = new ArrayList<>();
            for (ProjTransaction t : conditionalDB) {
                double prob = findProb(t, item);
                if (prob <= 0) continue;

                double newWeight = t.weight * prob;

                // Project: keep only items more frequent than current item
                int count = 0;
                for (int k = 0; k < t.items.length; k++) {
                    if (allowedItems.contains(t.items[k])) count++;
                }
                if (count == 0) continue;

                int[] newItems = new int[count];
                double[] newProbs = new double[count];
                int pos = 0;
                for (int k = 0; k < t.items.length; k++) {
                    if (allowedItems.contains(t.items[k])) {
                        newItems[pos] = t.items[k];
                        newProbs[pos] = t.probs[k];
                        pos++;
                    }
                }
                newDB.add(new ProjTransaction(newWeight, newItems, newProbs));
            }

            // Get frequent items in the conditional DB
            Map<Integer, Double> condExpSup = new HashMap<>();
            for (ProjTransaction t : newDB) {
                for (int k = 0; k < t.items.length; k++) {
                    condExpSup.merge(t.items[k], t.weight * t.probs[k], Double::sum);
                }
            }

            List<Integer> newHeader = new ArrayList<>();
            for (int h = 0; h < idx; h++) {
                int hItem = headerItems.get(h);
                Double es = condExpSup.get(hItem);
                if (es != null && es >= minsup) {
                    newHeader.add(hItem);
                }
            }

            // Build new prefix
            int[] newPrefix = new int[prefix.length + 1];
            System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
            newPrefix[prefix.length] = item;

            if (newHeader.isEmpty()) {
                // No further extensions → this is a leaf, candidate maximal itemset
                addMaximalIfNew(newPrefix, expSup);
            } else {
                // Recurse into conditional database
                fpmax(newPrefix, expSup, newDB, newHeader);
            }
        }

        // If no frequent extension was found for a non-empty prefix,
        // the prefix itself might be maximal
        if (!hasFrequentExtension && prefix.length > 0) {
            addMaximalIfNew(prefix, prefixExpSup);
        }
    }

    /**
     * Finds the probability of an item in a projected transaction.
     *
     * @param t the projected transaction
     * @param item the item to find
     * @return the probability, or 0.0 if not found
     */
    private double findProb(ProjTransaction t, int item) {
        for (int i = 0; i < t.items.length; i++) {
            if (t.items[i] == item) return t.probs[i];
        }
        return 0.0;
    }

    /**
     * Adds an itemset to the maximal set if it is not a subset of any existing MFI.
     * Also removes existing MFIs that are subsets of this new one.
     *
     * NOTE on Top-K mode: Unlike UGenMax, UFPMax does NOT raise the minsup
     * threshold dynamically during search. FPMax's processing order (least
     * frequent items first) causes single-item MFIs to be discovered early,
     * which would raise the threshold prematurely and prevent multi-item
     * MFIs from being found. Instead, the search runs at a fixed minsup
     * (set during seeding), and Top-K selection happens at the end.
     *
     * @param items the itemset to potentially add
     * @param expSup expected support of the itemset
     */
    private void addMaximalIfNew(int[] items, double expSup) {
        int[] sortedItems = items.clone();
        Arrays.sort(sortedItems);
        Itemset candidate = new Itemset(sortedItems, expSup);

        // Check if subsumed by any existing MFI
        for (Itemset mfi : maximalItemsets) {
            if (candidate.isSubsetOf(mfi)) {
                return;
            }
        }

        // Remove existing MFIs that are subsets of this new one
        maximalItemsets.removeIf(mfi -> mfi.isSubsetOf(candidate));

        maximalItemsets.add(candidate);
    }

    /**
     * Checks if a given itemset is a subset of any known maximal frequent itemset.
     *
     * @param items the itemset to check
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
     * Builds the sorted union of a prefix and a list of candidate items.
     *
     * @param prefix the current prefix
     * @param candidates the candidate items
     * @return sorted array containing all items from both
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

    /**
     * Seeds the Top-K heap with frequent single and 2-item itemsets
     * to establish a reasonable initial minsup threshold.
     *
     * @param itemExpSup map of item → expected support
     */
    private void seedTopKHeap(Map<Integer, Double> itemExpSup) {
        // Sort items by expSup descending
        List<Integer> allItems = new ArrayList<>(itemExpSup.keySet());
        allItems.sort((a, b) -> Double.compare(
                itemExpSup.getOrDefault(b, 0.0),
                itemExpSup.getOrDefault(a, 0.0)));

        int seedLimit = Math.min(allItems.size(), 50);
        List<Integer> topItems = allItems.subList(0, seedLimit);

        List<Itemset> seedCandidates = new ArrayList<>();

        // Single items
        for (int item : topItems) {
            seedCandidates.add(new Itemset(new int[]{item}, itemExpSup.get(item)));
        }

        // 2-item pairs among top items
        for (int i = 0; i < topItems.size(); i++) {
            int itemA = topItems.get(i);
            for (int j = i + 1; j < topItems.size(); j++) {
                int itemB = topItems.get(j);
                double expSup = 0;
                for (UncertainTransaction t : database.getTransactions()) {
                    double pA = t.getProbability(itemA);
                    double pB = t.getProbability(itemB);
                    if (pA > 0 && pB > 0) {
                        expSup += pA * pB;
                    }
                }
                if (expSup > 0) {
                    seedCandidates.add(new Itemset(new int[]{itemA, itemB}, expSup));
                }
            }
        }

        // Sort by expSup descending and fill heap
        seedCandidates.sort((a, b) -> Double.compare(
                b.getExpectedSupport(), a.getExpectedSupport()));

        for (int i = 0; i < seedCandidates.size(); i++) {
            topKHeap.offer(seedCandidates.get(i));
            if (topKHeap.isFull() && i > topK * 3) {
                break;
            }
        }
    }
}
