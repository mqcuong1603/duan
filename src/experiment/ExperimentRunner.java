package experiment;

import model.UncertainDatabase;
import model.Itemset;
import algorithm.UFPMax;
import algorithm.UGenMax;
import algorithm.TODISMAX;
import algorithm.APFIMAX;
import util.PerformanceTracker;

import java.io.*;
import java.util.*;

/**
 * Experiment runner for benchmarking all four algorithms across
 * multiple datasets with varying parameters.
 *
 * Outputs CSV-formatted results for easy import into charts/tables.
 *
 * Usage:
 *   java experiment.ExperimentRunner [output_dir]
 *
 * @author Ma Quoc Cuong, Nguyen Cao Phi
 */
public class ExperimentRunner {

    /** Number of warm-up runs before measurement */
    private static final int WARMUP_RUNS = 1;

    /** Number of measured runs (results averaged) */
    private static final int MEASURE_RUNS = 3;

    public static void main(String[] args) throws Exception {
        String outputDir = args.length > 0 ? args[0] : "results";
        new File(outputDir).mkdirs();

        System.out.println("=== Experiment Runner ===");
        System.out.println("Output directory: " + outputDir);
        System.out.println();

        // --- Experiment 1: Expected Support algorithms (UFPMax, UGenMax) ---
        runExpectedSupportExperiments(outputDir);

        // --- Experiment 2: Probabilistic Support algorithms (TODISMAX, APFIMAX) ---
        runProbabilisticExperiments(outputDir);

        // --- Experiment 3: Uncertainty level comparison ---
        runUncertaintyLevelExperiments(outputDir);

        // --- Experiment 4: Scalability (dataset size) ---
        runScalabilityExperiments(outputDir);

        System.out.println();
        System.out.println("=== All experiments complete ===");
    }

    /**
     * Experiment 1: Vary minsup for UFPMax and UGenMax on all datasets.
     */
    private static void runExpectedSupportExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 1: Expected Support Model ===");

        String[][] datasets = {
            {"data/mushroom_medium.txt", "mushroom_medium"},
            {"data/retail_medium.txt", "retail_medium"},
            {"data/accidents_medium.txt", "accidents_medium"},
        };

        // minsup values per dataset (tuned to produce meaningful results)
        double[][] minsupValues = {
            {500, 1000, 1500, 2000, 2500, 3000},  // mushroom (dense, 8416 txns)
            {1000, 2000, 3000, 5000, 8000, 10000}, // retail (sparse, 88162 txns)
            {20000, 30000, 40000, 50000, 60000, 70000}, // accidents (very dense, 340183 txns)
        };

        String[] algorithms = {"UFPMax", "UGenMax"};

        try (PrintWriter csv = new PrintWriter(new FileWriter(outputDir + "/exp1_expected_support.csv"))) {
            csv.println("dataset,algorithm,minsup,mfis,time_ms,memory_mb");

            for (int d = 0; d < datasets.length; d++) {
                String dataFile = datasets[d][0];
                String dataName = datasets[d][1];

                if (!new File(dataFile).exists()) {
                    System.out.println("  Skipping " + dataName + " (file not found)");
                    continue;
                }

                System.out.println("  Loading " + dataName + "...");
                UncertainDatabase db = UncertainDatabase.loadFromFile(dataFile);
                System.out.println("  Loaded: " + db.size() + " transactions, " + db.getItemCount() + " items");

                for (String algo : algorithms) {
                    for (double minsup : minsupValues[d]) {
                        System.out.print("    " + algo + " minsup=" + (int)minsup + " ... ");

                        long totalTime = 0;
                        double totalMem = 0;
                        int mfis = 0;

                        for (int run = 0; run < WARMUP_RUNS + MEASURE_RUNS; run++) {
                            PerformanceTracker tracker = new PerformanceTracker();
                            List<Itemset> results;
                            tracker.start();

                            if (algo.equals("UFPMax")) {
                                UFPMax alg = new UFPMax(db, minsup, -1);
                                results = alg.run();
                            } else {
                                UGenMax alg = new UGenMax(db, minsup, -1);
                                results = alg.run();
                            }

                            tracker.stop();

                            if (run >= WARMUP_RUNS) {
                                totalTime += tracker.getElapsedTimeMs();
                                totalMem += tracker.getMemoryUsageMB();
                                mfis = results.size();
                            }
                        }

                        long avgTime = totalTime / MEASURE_RUNS;
                        double avgMem = totalMem / MEASURE_RUNS;

                        System.out.println(mfis + " MFIs, " + avgTime + " ms, " + String.format("%.1f", avgMem) + " MB");
                        csv.println(dataName + "," + algo + "," + (int)minsup + "," + mfis + "," + avgTime + "," + String.format("%.1f", avgMem));
                        csv.flush();
                    }
                }
            }
        }
    }

    /**
     * Experiment 2: Vary minsup and minprob for TODISMAX and APFIMAX.
     */
    private static void runProbabilisticExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 2: Probabilistic Support Model ===");

        String[][] datasets = {
            {"data/mushroom_medium.txt", "mushroom_medium"},
            {"data/retail_medium.txt", "retail_medium"},
        };

        // Integer minsup values for probabilistic algorithms
        int[][] minsupValues = {
            {3000, 4000, 5000, 6000, 7000},  // mushroom
            {5000, 10000, 15000, 20000, 30000}, // retail
        };

        double[] minprobValues = {0.5, 0.7, 0.9};
        String[] algorithms = {"TODISMAX", "APFIMAX"};

        try (PrintWriter csv = new PrintWriter(new FileWriter(outputDir + "/exp2_probabilistic.csv"))) {
            csv.println("dataset,algorithm,minsup,minprob,mfis,time_ms,memory_mb");

            for (int d = 0; d < datasets.length; d++) {
                String dataFile = datasets[d][0];
                String dataName = datasets[d][1];

                if (!new File(dataFile).exists()) {
                    System.out.println("  Skipping " + dataName + " (file not found)");
                    continue;
                }

                System.out.println("  Loading " + dataName + "...");
                UncertainDatabase db = UncertainDatabase.loadFromFile(dataFile);

                for (String algo : algorithms) {
                    for (int minsup : minsupValues[d]) {
                        for (double minprob : minprobValues) {
                            System.out.print("    " + algo + " minsup=" + minsup + " minprob=" + minprob + " ... ");

                            PerformanceTracker tracker = new PerformanceTracker();
                            List<Itemset> results;
                            tracker.start();

                            if (algo.equals("TODISMAX")) {
                                TODISMAX alg = new TODISMAX(db, minsup, minprob);
                                results = alg.run();
                            } else {
                                APFIMAX alg = new APFIMAX(db, minsup, minprob);
                                results = alg.run();
                            }

                            tracker.stop();

                            System.out.println(results.size() + " MFIs, " + tracker.getElapsedTimeMs() + " ms");
                            csv.println(dataName + "," + algo + "," + minsup + "," + minprob + ","
                                    + results.size() + "," + tracker.getElapsedTimeMs() + ","
                                    + String.format("%.1f", tracker.getMemoryUsageMB()));
                            csv.flush();
                        }
                    }
                }
            }
        }
    }

    /**
     * Experiment 3: Compare performance across uncertainty levels (high/medium/low).
     */
    private static void runUncertaintyLevelExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 3: Uncertainty Level Comparison ===");

        String[] levels = {"high", "medium", "low"};
        String[] algorithms = {"UFPMax", "UGenMax"};
        double minsup = 1500; // fixed minsup for mushroom

        try (PrintWriter csv = new PrintWriter(new FileWriter(outputDir + "/exp3_uncertainty.csv"))) {
            csv.println("dataset,uncertainty,algorithm,minsup,mfis,time_ms,memory_mb");

            for (String level : levels) {
                String dataFile = "data/mushroom_" + level + ".txt";
                if (!new File(dataFile).exists()) {
                    System.out.println("  Skipping mushroom_" + level + " (file not found)");
                    continue;
                }

                System.out.println("  Loading mushroom_" + level + "...");
                UncertainDatabase db = UncertainDatabase.loadFromFile(dataFile);

                for (String algo : algorithms) {
                    System.out.print("    " + algo + " minsup=" + (int)minsup + " ... ");

                    PerformanceTracker tracker = new PerformanceTracker();
                    List<Itemset> results;
                    tracker.start();

                    if (algo.equals("UFPMax")) {
                        UFPMax alg = new UFPMax(db, minsup, -1);
                        results = alg.run();
                    } else {
                        UGenMax alg = new UGenMax(db, minsup, -1);
                        results = alg.run();
                    }

                    tracker.stop();

                    System.out.println(results.size() + " MFIs, " + tracker.getElapsedTimeMs() + " ms");
                    csv.println("mushroom," + level + "," + algo + "," + (int)minsup + ","
                            + results.size() + "," + tracker.getElapsedTimeMs() + ","
                            + String.format("%.1f", tracker.getMemoryUsageMB()));
                    csv.flush();
                }
            }
        }
    }

    /**
     * Experiment 4: Scalability across different dataset sizes.
     */
    private static void runScalabilityExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 4: Scalability ===");

        // Dataset, name, minsup (tuned per dataset)
        Object[][] configs = {
            {"data/mushroom_medium.txt", "mushroom", 2000.0},
            {"data/retail_medium.txt", "retail", 5000.0},
            {"data/accidents_medium.txt", "accidents", 50000.0},
        };

        String[] algorithms = {"UFPMax", "UGenMax"};

        try (PrintWriter csv = new PrintWriter(new FileWriter(outputDir + "/exp4_scalability.csv"))) {
            csv.println("dataset,transactions,items,algorithm,minsup,mfis,time_ms,memory_mb");

            for (Object[] config : configs) {
                String dataFile = (String) config[0];
                String dataName = (String) config[1];
                double minsup = (Double) config[2];

                if (!new File(dataFile).exists()) {
                    System.out.println("  Skipping " + dataName + " (file not found)");
                    continue;
                }

                System.out.println("  Loading " + dataName + "...");
                UncertainDatabase db = UncertainDatabase.loadFromFile(dataFile);

                for (String algo : algorithms) {
                    System.out.print("    " + algo + " minsup=" + (int)minsup + " ... ");

                    PerformanceTracker tracker = new PerformanceTracker();
                    List<Itemset> results;
                    tracker.start();

                    if (algo.equals("UFPMax")) {
                        UFPMax alg = new UFPMax(db, minsup, -1);
                        results = alg.run();
                    } else {
                        UGenMax alg = new UGenMax(db, minsup, -1);
                        results = alg.run();
                    }

                    tracker.stop();

                    System.out.println(results.size() + " MFIs, " + tracker.getElapsedTimeMs() + " ms");
                    csv.println(dataName + "," + db.size() + "," + db.getItemCount() + ","
                            + algo + "," + (int)minsup + "," + results.size() + ","
                            + tracker.getElapsedTimeMs() + ","
                            + String.format("%.1f", tracker.getMemoryUsageMB()));
                    csv.flush();
                }
            }
        }
    }
}
