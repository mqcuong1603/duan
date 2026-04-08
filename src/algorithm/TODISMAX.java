package algorithm;

import model.UncertainDatabase;
import model.UncertainTransaction;
import model.Itemset;

import java.util.*;

/**
 * TODIS-MAX: Top-Down Inheritance of Support for mining maximal
 * probabilistic frequent itemsets (p-FPs) from uncertain databases.
 *
 * Based on: Sun et al., "Mining Uncertain Data with Probabilistic
 * Guarantees", KDD 2010, Section 4.2.
 *
 * Uses the PROBABILISTIC SUPPORT model:
 *   An itemset X is a p-FP if P(sup(X) >= minsup) >= minprob
 *   where sup(X) is a random variable under Possible World Semantics.
 *
 * Adapted for ATTRIBUTE UNCERTAINTY: each item has its own probability
 * per transaction (not tuple-level uncertainty). For itemset X in
 * transaction t, the effective probability is p_t^X = Π P(i,t) for i∈X.
 *
 * Algorithm:
 *   Phase 1 — Apriori-based candidate generation with pruning:
 *     - Lemma 2: prune if cnt(X) < minsup
 *     - Lemma 3: prune using Chernoff bound on expected support
 *   Phase 2 — Top-down maximal verification:
 *     - Check candidates from longest to shortest
 *     - Compute exact pmf via Dynamic Programming
 *     - Skip subsets of confirmed maximal p-FPs
 *
 * Parameters:
 *   minsup  — integer support count threshold
 *   minprob — probability threshold τ ∈ (0, 1]
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class TODISMAX {

    private final UncertainDatabase database;
    private final int minsup;
    private final double minprob;

    // Statistics
    private int candidatesGenerated = 0;
    private int prunedByCnt = 0;
    private int prunedByChernoff = 0;
    private int pmfsComputed = 0;
    private int skippedAsSubset = 0;

    /**
     * Creates a new TODIS-MAX instance.
     *
     * @param database the uncertain transaction database
     * @param minsup minimum support count threshold (integer)
     * @param minprob minimum probability threshold τ
     */
    public TODISMAX(UncertainDatabase database, int minsup, double minprob) {
        this.database = database;
        this.minsup = minsup;
        this.minprob = minprob;
    }

    /**
     * Runs the TODIS-MAX algorithm.
     *
     * @return list of maximal probabilistic frequent itemsets
     */
    public List<Itemset> run() {
        System.out.println("  [TODIS-MAX] Phase 1: Generating candidates...");
        System.out.println("  [TODIS-MAX] minsup=" + minsup + ", minprob=" + minprob);

        // === Phase 1: Apriori-based candidate generation ===
        // Level 1: find frequent single items
        List<int[]> prevLevel = new ArrayList<>();
        Map<String, double[]> candidateInfo = new LinkedHashMap<>(); // key -> [cnt, esup]

        List<Integer> allItems = new ArrayList<>(database.getAllItems());
        Collections.sort(allItems);

        for (int item : allItems) {
            int[] itemset = {item};
            SupportPMF.ProbInfo info = SupportPMF.computeProbList(database, itemset);
            candidatesGenerated++;

            if (info.count < minsup) {
                prunedByCnt++;
                continue;
            }
            if (SupportPMF.canPruneChernoff(info.expectedSupport, minsup, minprob)) {
                prunedByChernoff++;
                continue;
            }
            prevLevel.add(itemset);
            candidateInfo.put(Arrays.toString(itemset), new double[]{info.count, info.expectedSupport});
        }

        System.out.println("  [TODIS-MAX] Level 1: " + prevLevel.size() + " candidates");

        // Collect all candidates across levels
        List<int[]> allCandidates = new ArrayList<>(prevLevel);

        // Level k+1: join and prune
        int level = 1;
        while (!prevLevel.isEmpty()) {
            level++;
            List<int[]> nextLevel = new ArrayList<>();

            for (int i = 0; i < prevLevel.size(); i++) {
                for (int j = i + 1; j < prevLevel.size(); j++) {
                    int[] joined = aprioriJoin(prevLevel.get(i), prevLevel.get(j));
                    if (joined == null) continue;

                    // Check all (k-1)-subsets are candidates
                    if (!allSubsetsExist(joined, candidateInfo)) continue;

                    candidatesGenerated++;
                    SupportPMF.ProbInfo info = SupportPMF.computeProbList(database, joined);

                    if (info.count < minsup) {
                        prunedByCnt++;
                        continue;
                    }
                    if (SupportPMF.canPruneChernoff(info.expectedSupport, minsup, minprob)) {
                        prunedByChernoff++;
                        continue;
                    }

                    nextLevel.add(joined);
                    candidateInfo.put(Arrays.toString(joined), new double[]{info.count, info.expectedSupport});
                }
            }

            if (!nextLevel.isEmpty()) {
                System.out.println("  [TODIS-MAX] Level " + level + ": " + nextLevel.size() + " candidates");
                allCandidates.addAll(nextLevel);
            }
            prevLevel = nextLevel;
        }

        System.out.println("  [TODIS-MAX] Total candidates: " + allCandidates.size());
        System.out.println("  [TODIS-MAX] Pruned by cnt: " + prunedByCnt
                + ", by Chernoff: " + prunedByChernoff);

        // === Phase 2: Top-down maximal verification ===
        System.out.println("  [TODIS-MAX] Phase 2: Top-down verification...");

        // Sort candidates by length descending, then by esup descending
        allCandidates.sort((a, b) -> {
            if (a.length != b.length) return Integer.compare(b.length, a.length);
            double esupA = candidateInfo.getOrDefault(Arrays.toString(a), new double[]{0, 0})[1];
            double esupB = candidateInfo.getOrDefault(Arrays.toString(b), new double[]{0, 0})[1];
            return Double.compare(esupB, esupA);
        });

        List<Itemset> maximalPFPs = new ArrayList<>();

        for (int[] candidate : allCandidates) {
            // Check if candidate is subset of any known maximal p-FP
            if (isSubsetOfAny(candidate, maximalPFPs)) {
                skippedAsSubset++;
                continue;
            }

            // Compute exact pmf
            SupportPMF.ProbInfo info = SupportPMF.computeProbList(database, candidate);
            double[] pmf = SupportPMF.computePMF_DP(info.probs);
            double probFreq = SupportPMF.probFrequent(pmf, minsup);
            pmfsComputed++;

            // Check if it's a p-FP
            if (probFreq >= minprob) {
                maximalPFPs.add(new Itemset(candidate, probFreq));
            }
        }

        System.out.println("  [TODIS-MAX] PMFs computed: " + pmfsComputed);
        System.out.println("  [TODIS-MAX] Skipped as subsets: " + skippedAsSubset);
        System.out.println("  [TODIS-MAX] Maximal p-FPs found: " + maximalPFPs.size());

        return maximalPFPs;
    }

    /**
     * Apriori join: joins two sorted itemsets of size k that share k-1 items.
     * Returns null if they cannot be joined.
     */
    private int[] aprioriJoin(int[] a, int[] b) {
        int k = a.length;
        if (k != b.length) return null;

        // Check first k-1 items are identical
        for (int i = 0; i < k - 1; i++) {
            if (a[i] != b[i]) return null;
        }

        // Last items must differ
        if (a[k - 1] >= b[k - 1]) return null;

        // Join
        int[] result = new int[k + 1];
        System.arraycopy(a, 0, result, 0, k);
        result[k] = b[k - 1];
        return result;
    }

    /**
     * Checks that all (k-1)-subsets of the given itemset exist in the candidate map.
     */
    private boolean allSubsetsExist(int[] itemset, Map<String, double[]> candidateInfo) {
        int k = itemset.length;
        for (int skip = 0; skip < k; skip++) {
            int[] subset = new int[k - 1];
            int idx = 0;
            for (int i = 0; i < k; i++) {
                if (i != skip) subset[idx++] = i < k ? itemset[i] : 0;
            }
            // Fix: properly build subset
            idx = 0;
            for (int i = 0; i < k; i++) {
                if (i != skip) subset[idx++] = itemset[i];
            }
            if (!candidateInfo.containsKey(Arrays.toString(subset))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if candidate is a subset of any itemset in the list.
     */
    private boolean isSubsetOfAny(int[] candidate, List<Itemset> maximalPFPs) {
        for (Itemset mfi : maximalPFPs) {
            if (isSubset(candidate, mfi.getItems())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if sorted array a is a subset of sorted array b.
     */
    private boolean isSubset(int[] a, int[] b) {
        if (a.length > b.length) return false;
        int j = 0;
        for (int i = 0; i < a.length; i++) {
            while (j < b.length && b[j] < a[i]) j++;
            if (j >= b.length || b[j] != a[i]) return false;
            j++;
        }
        return true;
    }

    // --- Getters for statistics ---
    public int getCandidatesGenerated() { return candidatesGenerated; }
    public int getPrunedByCnt() { return prunedByCnt; }
    public int getPrunedByChernoff() { return prunedByChernoff; }
    public int getPmfsComputed() { return pmfsComputed; }
    public int getSkippedAsSubset() { return skippedAsSubset; }
}
