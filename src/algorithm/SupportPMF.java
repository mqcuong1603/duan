package algorithm;

import java.util.List;
import model.UncertainDatabase;
import model.UncertainTransaction;

/**
 * Utility class for computing support probability mass functions (pmf)
 * and related probabilistic measures for uncertain databases.
 *
 * Used by TODIS-MAX (exact pmf computation) and APFI-MAX (CLT approximation).
 *
 * In the probabilistic support model, for an itemset X:
 *   - Each transaction t_i contributes a Bernoulli random variable with
 *     probability p_i^X = product of P(item, t_i) for each item in X
 *   - sup(X) = sum of these Bernoulli variables (Poisson binomial distribution)
 *   - P(sup(X) >= minsup) is computed from the pmf of sup(X)
 *
 * Adapted from: Sun et al., "Mining Uncertain Data with Probabilistic
 * Guarantees", KDD 2010. Algorithms DP and DC.
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class SupportPMF {

    /**
     * Result of computing the probability list for an itemset.
     * Contains the per-transaction probabilities, count, expected support, and variance.
     */
    public static class ProbInfo {
        /** Per-transaction probability p_i^X for transactions containing X */
        public final double[] probs;
        /** Number of transactions containing all items of X */
        public final int count;
        /** Expected support E(X) = sum of p_i */
        public final double expectedSupport;
        /** Variance Var(X) = sum of p_i * (1 - p_i) */
        public final double variance;

        public ProbInfo(double[] probs, int count, double expectedSupport, double variance) {
            this.probs = probs;
            this.count = count;
            this.expectedSupport = expectedSupport;
            this.variance = variance;
        }
    }

    /**
     * Computes the probability list for an itemset X over the database.
     *
     * For each transaction containing all items of X, computes
     * p_i^X = product of P(item, t_i) for each item in X.
     *
     * @param database the uncertain database
     * @param itemset the itemset to compute probabilities for
     * @return ProbInfo with per-transaction probs, count, E(X), and Var(X)
     */
    public static ProbInfo computeProbList(UncertainDatabase database, int[] itemset) {
        List<UncertainTransaction> transactions = database.getTransactions();
        double[] allProbs = new double[transactions.size()];
        int count = 0;
        double esup = 0.0;
        double variance = 0.0;

        for (UncertainTransaction t : transactions) {
            double p = t.computeExpectedSupport(itemset);
            if (p > 0.0) {
                allProbs[count] = p;
                esup += p;
                variance += p * (1.0 - p);
                count++;
            }
        }

        // Trim to actual size
        double[] probs = new double[count];
        System.arraycopy(allProbs, 0, probs, 0, count);
        return new ProbInfo(probs, count, esup, variance);
    }

    /**
     * Computes the support pmf using Dynamic Programming (Algorithm 1 from TODIS paper).
     *
     * Time: O(n^2), Space: O(n)
     *
     * @param probs array of per-transaction probabilities p_i^X
     * @return pmf array where pmf[k] = P(sup(X) = k)
     */
    public static double[] computePMF_DP(double[] probs) {
        int n = probs.length;
        double[] f = new double[n + 1];
        f[0] = 1.0; // Initially, sup(X) = 0 with probability 1

        for (int i = 0; i < n; i++) {
            double pi = probs[i];
            double[] fNew = new double[n + 1];
            fNew[0] = (1.0 - pi) * f[0];
            for (int k = 1; k <= i + 1; k++) {
                fNew[k] = pi * f[k - 1] + (1.0 - pi) * f[k];
            }
            f = fNew;
        }
        return f;
    }

    /**
     * Computes P(sup(X) >= minsup) from the support pmf.
     *
     * @param pmf the support pmf array
     * @param minsup minimum support count threshold
     * @return probability that support >= minsup
     */
    public static double probFrequent(double[] pmf, int minsup) {
        double sum = 0.0;
        for (int k = minsup; k < pmf.length; k++) {
            sum += pmf[k];
        }
        return sum;
    }

    /**
     * Convolves two pmf arrays.
     * If f1 is pmf of X1 and f2 is pmf of X2 (independent),
     * result is pmf of X1 + X2.
     *
     * Time: O(n*m) where n = f1.length, m = f2.length
     *
     * @param f1 first pmf
     * @param f2 second pmf
     * @return convolution of f1 and f2
     */
    public static double[] convolve(double[] f1, double[] f2) {
        int n1 = f1.length;
        int n2 = f2.length;
        double[] result = new double[n1 + n2 - 1];

        for (int i = 0; i < n1; i++) {
            if (f1[i] == 0.0) continue;
            for (int j = 0; j < n2; j++) {
                result[i + j] += f1[i] * f2[j];
            }
        }
        return result;
    }

    // ========== CLT-based approximation (used by APFI-MAX) ==========

    /**
     * Approximates P(sup(X) >= minsup) using the Central Limit Theorem.
     *
     * From APFI-MAX paper Eq.7:
     *   P(sup(X) >= T) ≈ 1 - Φ((T - E(X)) / sqrt(Var(X)))
     *
     * @param expectedSupport E(X) = sum of p_i
     * @param variance Var(X) = sum of p_i * (1 - p_i)
     * @param minsup minimum support count T
     * @return approximate probability
     */
    public static double probFrequentCLT(double expectedSupport, double variance, int minsup) {
        if (variance <= 0.0) {
            // Degenerate case: all probs are 0 or 1
            return expectedSupport >= minsup ? 1.0 : 0.0;
        }
        double z = (minsup - expectedSupport) / Math.sqrt(variance);
        return 1.0 - normalCDF(z);
    }

    /**
     * Computes the lower bound of E(X) for a PMFI candidate.
     * From APFI-MAX paper Theorem 2, Eq.3:
     *   lb(E(X)) = (2T - ln(τ) - sqrt(ln²(τ) - 8T·ln(τ))) / 2
     *
     * @param minsup support threshold T
     * @param minprob probability threshold τ
     * @return lower bound of expected support
     */
    public static double lowerBoundExpectation(int minsup, double minprob) {
        double T = minsup;
        double lnTau = Math.log(minprob);
        double discriminant = lnTau * lnTau - 8.0 * T * lnTau;
        if (discriminant < 0) return T; // Fallback
        return (2.0 * T - lnTau - Math.sqrt(discriminant)) / 2.0;
    }

    /**
     * Computes the upper bound of E(X) for a PMFI.
     * From APFI-MAX paper Theorem 2, Eq.3:
     *   ub(E(X)) = T - ln(1-τ) + sqrt(ln²(1-τ) - 2T·ln(1-τ))
     *
     * @param minsup support threshold T
     * @param minprob probability threshold τ
     * @return upper bound of expected support
     */
    public static double upperBoundExpectation(int minsup, double minprob) {
        double T = minsup;
        double ln1mTau = Math.log(1.0 - minprob);
        double discriminant = ln1mTau * ln1mTau - 2.0 * T * ln1mTau;
        if (discriminant < 0) return T; // Fallback
        return T - ln1mTau + Math.sqrt(discriminant);
    }

    /**
     * Checks if pattern X can be pruned using the Chernoff bound (Lemma 3 from TODIS).
     *
     * @param esup expected support of X
     * @param minsup minimum support threshold
     * @param minprob minimum probability threshold
     * @return true if X can be pruned (definitely not a p-FP)
     */
    public static boolean canPruneChernoff(double esup, int minsup, double minprob) {
        if (esup >= minsup) return false; // Can't prune if expected support >= threshold
        double sigma = (minsup - esup - 1.0) / esup;
        if (sigma <= 0) return false;

        if (sigma >= 2 * Math.E - 1) {
            return Math.pow(2, -sigma * esup) < minprob;
        } else {
            return Math.exp(-sigma * sigma * esup / 4.0) < minprob;
        }
    }

    // ========== Normal distribution CDF ==========

    /**
     * Approximation of the standard normal CDF Φ(x).
     * Uses the Abramowitz & Stegun approximation (error < 7.5e-8).
     *
     * @param x the value
     * @return Φ(x)
     */
    public static double normalCDF(double x) {
        if (x < -8.0) return 0.0;
        if (x > 8.0) return 1.0;

        boolean negative = (x < 0);
        if (negative) x = -x;

        // Abramowitz & Stegun constants
        double p = 0.2316419;
        double b1 = 0.319381530;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;

        double t = 1.0 / (1.0 + p * x);
        double pdf = Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
        double cdf = 1.0 - pdf * t * (b1 + t * (b2 + t * (b3 + t * (b4 + t * b5))));

        return negative ? 1.0 - cdf : cdf;
    }
}
