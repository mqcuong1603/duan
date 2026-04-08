package algorithm;

import model.UncertainDatabase;
import model.UncertainTransaction;
import model.Itemset;

import java.util.*;

/**
 * APFI-MAX: Approximation of Probabilistic Frequent Itemset-MAX.
 *
 * An efficient algorithm for mining probabilistic maximal frequent itemsets
 * from uncertain databases using Central Limit Theorem (CLT) approximation.
 *
 * Based on: Chen et al., "Approximation of Probabilistic Maximal Frequent
 * Itemset Mining over Uncertain Sensed Data", IEEE Access, 2020.
 *
 * Uses the PROBABILISTIC SUPPORT model:
 *   An itemset X is a p-FP if P(sup(X) >= minsup) >= minprob
 *
 * Key advantage over TODIS-MAX:
 *   - Frequency estimation via CLT in O(n) instead of exact pmf in O(n²)
 *   - Candidate generation uses expectation bound (Theorem 2) for tighter pruning
 *   - Trades small accuracy loss for significant speed improvement
 *
 * Algorithm:
 *   Step 1 — CGEB (Candidate Generation with Expectation Bound):
 *     - Apriori-based generation with two criteria:
 *       (a) cnt(X) >= minsup
 *       (b) E(X) >= lower_bound(E) from Chernoff bound
 *     - Records E(X) and Var(X) during generation for later use
 *
 *   Step 2 — APFI-MAX (Top-down confirmation):
 *     - Check candidates from longest to shortest
 *     - If candidate is subset of known p-FP, inherit frequency (skip check)
 *     - Otherwise, estimate frequency using FM (Algorithm 3)
 *
 *   Step 3 — FM (Frequency Measurement):
 *     - If E(X) >= upper_bound(E), automatically frequent
 *     - Otherwise, estimate P(sup(X)>=T) ≈ 1 - Φ((T-E(X))/√Var(X))
 *
 * Parameters:
 *   minsup  — integer support count threshold
 *   minprob — probability threshold τ ∈ (0, 1]
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class APFIMAX {

    private final UncertainDatabase database;
    private final int minsup;
    private final double minprob;

    // Precomputed bounds
    private final double lbExpectation;
    private final double ubExpectation;

    // Statistics
    private int candidatesGenerated = 0;
    private int prunedByCnt = 0;
    private int prunedByExpBound = 0;
    private int confirmedByUB = 0;
    private int confirmedByCLT = 0;
    private int inheritedFromSuperset = 0;

    /**
     * Creates a new APFI-MAX instance.
     *
     * @param database the uncertain transaction database
     * @param minsup minimum support count threshold (integer)
     * @param minprob minimum probability threshold τ
     */
    public APFIMAX(UncertainDatabase database, int minsup, double minprob) {
        this.database = database;
        this.minsup = minsup;
        this.minprob = minprob;
        this.lbExpectation = SupportPMF.lowerBoundExpectation(minsup, minprob);
        this.ubExpectation = SupportPMF.upperBoundExpectation(minsup, minprob);
    }

    /**
     * Runs the APFI-MAX algorithm.
     *
     * @return list of maximal probabilistic frequent itemsets (approximate)
     */
    public List<Itemset> run() {
        System.out.println("  [APFI-MAX] minsup=" + minsup + ", minprob=" + minprob);
        System.out.println("  [APFI-MAX] Expectation bounds: lb=" + String.format("%.4f", lbExpectation)
                + ", ub=" + String.format("%.4f", ubExpectation));

        // === Step 1: CGEB — Candidate Generation with Expectation Bound ===
        System.out.println("  [APFI-MAX] Step 1: CGEB candidate generation...");

        // Store candidate info: key -> [E(X), Var(X)]
        Map<String, double[]> candidateStats = new LinkedHashMap<>();
        List<int[]> allCandidates = new ArrayList<>();

        // Level 1: single items
        List<int[]> prevLevel = new ArrayList<>();
        List<Integer> allItems = new ArrayList<>(database.getAllItems());
        Collections.sort(allItems);

        for (int item : allItems) {
            int[] itemset = {item};
            SupportPMF.ProbInfo info = SupportPMF.computeProbList(database, itemset);
            candidatesGenerated++;

            // Criterion 1: cnt(X) >= minsup
            if (info.count < minsup) {
                prunedByCnt++;
                continue;
            }

            // Criterion 2: E(X) >= lb(E(X)) — from Theorem 1 & 2
            if (info.expectedSupport < lbExpectation) {
                prunedByExpBound++;
                continue;
            }

            prevLevel.add(itemset);
            candidateStats.put(Arrays.toString(itemset),
                    new double[]{info.expectedSupport, info.variance});
        }

        System.out.println("  [APFI-MAX] Level 1: " + prevLevel.size() + " candidates");
        allCandidates.addAll(prevLevel);

        // Level k+1: join and prune
        int level = 1;
        while (!prevLevel.isEmpty()) {
            level++;
            List<int[]> nextLevel = new ArrayList<>();

            for (int i = 0; i < prevLevel.size(); i++) {
                for (int j = i + 1; j < prevLevel.size(); j++) {
                    int[] joined = aprioriJoin(prevLevel.get(i), prevLevel.get(j));
                    if (joined == null) continue;

                    // Check all (k-1)-subsets exist
                    if (!allSubsetsExist(joined, candidateStats)) continue;

                    candidatesGenerated++;
                    SupportPMF.ProbInfo info = SupportPMF.computeProbList(database, joined);

                    // Criterion 1
                    if (info.count < minsup) {
                        prunedByCnt++;
                        continue;
                    }

                    // Criterion 2
                    if (info.expectedSupport < lbExpectation) {
                        prunedByExpBound++;
                        continue;
                    }

                    nextLevel.add(joined);
                    candidateStats.put(Arrays.toString(joined),
                            new double[]{info.expectedSupport, info.variance});
                }
            }

            if (!nextLevel.isEmpty()) {
                System.out.println("  [APFI-MAX] Level " + level + ": " + nextLevel.size() + " candidates");
                allCandidates.addAll(nextLevel);
            }
            prevLevel = nextLevel;
        }

        System.out.println("  [APFI-MAX] Total candidates: " + allCandidates.size());
        System.out.println("  [APFI-MAX] Pruned by cnt: " + prunedByCnt
                + ", by expectation bound: " + prunedByExpBound);

        // === Step 2: APFI-MAX — Top-down confirmation ===
        System.out.println("  [APFI-MAX] Step 2: Top-down confirmation...");

        // Sort candidates by length descending
        allCandidates.sort((a, b) -> {
            if (a.length != b.length) return Integer.compare(b.length, a.length);
            double esupA = candidateStats.getOrDefault(Arrays.toString(a), new double[]{0, 0})[0];
            double esupB = candidateStats.getOrDefault(Arrays.toString(b), new double[]{0, 0})[0];
            return Double.compare(esupB, esupA);
        });

        // Fre_Pre: confirmed p-FPs from the previous (longer) level
        Set<String> confirmedPFPs = new HashSet<>();
        List<Itemset> maximalPFPs = new ArrayList<>();

        int currentLength = allCandidates.isEmpty() ? 0 : allCandidates.get(0).length;
        Set<String> freCurrentLevel = new HashSet<>();

        for (int[] candidate : allCandidates) {
            // Detect level change: update Fre_Pre
            if (candidate.length < currentLength) {
                confirmedPFPs.addAll(freCurrentLevel);
                freCurrentLevel.clear();
                currentLength = candidate.length;
            }

            String key = Arrays.toString(candidate);
            double[] stats = candidateStats.get(key);
            double esup = stats[0];
            double variance = stats[1];

            // Inheritance check: if candidate is subset of any confirmed p-FP, it's frequent
            boolean inherited = false;
            if (isSubsetOfAnyConfirmed(candidate, confirmedPFPs, allCandidates)) {
                inherited = true;
                inheritedFromSuperset++;
                freCurrentLevel.add(key);
                // But don't add to maximal (it has a superset that's also frequent)
                continue;
            }

            // FM: Frequency Measurement (Algorithm 3)
            boolean isFrequent = measureFrequency(esup, variance);

            if (isFrequent) {
                freCurrentLevel.add(key);
                // Check maximality: not subset of any known maximal p-FP
                if (!isSubsetOfAny(candidate, maximalPFPs)) {
                    // Compute actual prob for output
                    double probFreq;
                    if (esup >= ubExpectation) {
                        probFreq = SupportPMF.probFrequentCLT(esup, variance, minsup);
                    } else {
                        probFreq = SupportPMF.probFrequentCLT(esup, variance, minsup);
                    }
                    maximalPFPs.add(new Itemset(candidate, probFreq));
                }
            }
        }

        System.out.println("  [APFI-MAX] Confirmed by upper bound: " + confirmedByUB);
        System.out.println("  [APFI-MAX] Confirmed by CLT: " + confirmedByCLT);
        System.out.println("  [APFI-MAX] Inherited from superset: " + inheritedFromSuperset);
        System.out.println("  [APFI-MAX] Maximal p-FPs found: " + maximalPFPs.size());

        return maximalPFPs;
    }

    /**
     * FM: Frequency Measurement (Algorithm 3 from APFI-MAX paper).
     *
     * @param esup expected support E(X)
     * @param variance variance Var(X)
     * @return true if X is estimated to be probabilistically frequent
     */
    private boolean measureFrequency(double esup, double variance) {
        // If E(X) >= upper bound, definitely frequent
        if (esup >= ubExpectation) {
            confirmedByUB++;
            return true;
        }

        // Otherwise, use CLT approximation (Eq. 7)
        double probFreq = SupportPMF.probFrequentCLT(esup, variance, minsup);
        if (probFreq >= minprob) {
            confirmedByCLT++;
            return true;
        }
        return false;
    }

    /**
     * Checks if candidate is a subset of any confirmed p-FP from a longer level.
     */
    private boolean isSubsetOfAnyConfirmed(int[] candidate, Set<String> confirmedPFPs,
                                            List<int[]> allCandidates) {
        for (int[] other : allCandidates) {
            if (other.length <= candidate.length) continue;
            if (confirmedPFPs.contains(Arrays.toString(other))) {
                if (isSubset(candidate, other)) {
                    return true;
                }
            }
        }
        return false;
    }

    // === Utility methods (same as TODISMAX) ===

    private int[] aprioriJoin(int[] a, int[] b) {
        int k = a.length;
        if (k != b.length) return null;
        for (int i = 0; i < k - 1; i++) {
            if (a[i] != b[i]) return null;
        }
        if (a[k - 1] >= b[k - 1]) return null;
        int[] result = new int[k + 1];
        System.arraycopy(a, 0, result, 0, k);
        result[k] = b[k - 1];
        return result;
    }

    private boolean allSubsetsExist(int[] itemset, Map<String, double[]> candidateStats) {
        int k = itemset.length;
        for (int skip = 0; skip < k; skip++) {
            int[] subset = new int[k - 1];
            int idx = 0;
            for (int i = 0; i < k; i++) {
                if (i != skip) subset[idx++] = itemset[i];
            }
            if (!candidateStats.containsKey(Arrays.toString(subset))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSubsetOfAny(int[] candidate, List<Itemset> maximalPFPs) {
        for (Itemset mfi : maximalPFPs) {
            if (isSubset(candidate, mfi.getItems())) return true;
        }
        return false;
    }

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
    public int getPrunedByExpBound() { return prunedByExpBound; }
    public int getConfirmedByUB() { return confirmedByUB; }
    public int getConfirmedByCLT() { return confirmedByCLT; }
    public int getInheritedFromSuperset() { return inheritedFromSuperset; }
}
