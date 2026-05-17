package experiment;

import model.UncertainDatabase;
import model.Itemset;
import algorithm.UFPMax;
import algorithm.UGenMax;
import util.PerformanceTracker;

import java.io.*;
import java.util.*;

/**
 * Experiment runner for benchmarking UFPMax and UGenMax across
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

        // --- Experiment 2: Uncertainty level comparison ---
        runUncertaintyLevelExperiments(outputDir);

        // --- Experiment 3: Scalability (dataset size) ---
        runScalabilityExperiments(outputDir);

        // --- Experiment 4: Top-K mode ---
        runTopKExperiments(outputDir);

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

        double[][] minsupValues = {
            {500, 1000, 1500, 2000, 2500, 3000},
            {1000, 2000, 3000, 5000, 8000, 10000},
            {20000, 30000, 40000, 50000, 60000, 70000},
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
     * Experiment 2: Compare performance across uncertainty levels (high/medium/low).
     */
    private static void runUncertaintyLevelExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 2: Uncertainty Level Comparison ===");

        String[] levels = {"high", "medium", "low"};
        String[] algorithms = {"UFPMax", "UGenMax"};
        double minsup = 1500;

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
     * Experiment 3: Scalability across different dataset sizes.
     */
    private static void runScalabilityExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 3: Scalability ===");

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

    /**
     * Experiment 4: Top-K mode benchmark for UFPMax and UGenMax across K values.
     * Reports the dynamically-raised final minsup so the threshold-raising
     * behaviour of the two algorithms can be compared.
     */
    private static void runTopKExperiments(String outputDir) throws Exception {
        System.out.println("=== Experiment 4: Top-K Mode ===");

        String[][] datasets = {
            {"data/mushroom_medium.txt", "mushroom_medium"},
            {"data/retail_medium.txt", "retail_medium"},
            {"data/accidents_medium.txt", "accidents_medium"},
        };
        int[] kValues = {5, 10, 20, 50, 100};
        String[] algorithms = {"UFPMax", "UGenMax"};

        try (PrintWriter csv = new PrintWriter(new FileWriter(outputDir + "/exp_topk.csv"))) {
            csv.println("dataset,algorithm,k,mfis_found,time_ms,memory_mb,final_minsup");

            for (String[] ds : datasets) {
                String dataFile = ds[0];
                String dataName = ds[1];

                if (!new File(dataFile).exists()) {
                    System.out.println("  Skipping " + dataName + " (file not found)");
                    continue;
                }

                System.out.println("  Loading " + dataName + "...");
                UncertainDatabase db = UncertainDatabase.loadFromFile(dataFile);
                System.out.println("  Loaded: " + db.size() + " transactions, " + db.getItemCount() + " items");

                for (String algo : algorithms) {
                    for (int k : kValues) {
                        System.out.print("    " + algo + " k=" + k + " ... ");

                        long totalTime = 0;
                        double totalMem = 0;
                        int mfis = 0;
                        double finalMinsup = 0.0;

                        for (int run = 0; run < WARMUP_RUNS + MEASURE_RUNS; run++) {
                            PerformanceTracker tracker = new PerformanceTracker();
                            List<Itemset> results;
                            tracker.start();

                            if (algo.equals("UFPMax")) {
                                UFPMax alg = new UFPMax(db, 0.0, k);
                                results = alg.run();
                                if (run >= WARMUP_RUNS) {
                                    finalMinsup = alg.getMinsup();
                                }
                            } else {
                                UGenMax alg = new UGenMax(db, 0.0, k);
                                results = alg.run();
                                if (run >= WARMUP_RUNS) {
                                    finalMinsup = alg.getMinsup();
                                }
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

                        System.out.println(mfis + " MFIs, " + avgTime + " ms, "
                                + String.format("%.1f", avgMem) + " MB, final_minsup="
                                + String.format("%.4f", finalMinsup));
                        csv.println(dataName + "," + algo + "," + k + "," + mfis + ","
                                + avgTime + "," + String.format("%.1f", avgMem) + ","
                                + String.format("%.4f", finalMinsup));
                        csv.flush();
                    }
                }
            }
        }
    }
}
