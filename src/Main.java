import model.UncertainDatabase;
import model.Itemset;
import model.TopKHeap;
import algorithm.UFPMax;
import algorithm.UGenMax;
import algorithm.TODISMAX;
import algorithm.APFIMAX;
import util.PerformanceTracker;

import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Main entry point for the Frequent Maximal Itemset Mining program.
 *
 * Supports four algorithms for mining frequent maximal itemsets
 * from uncertain transaction databases:
 *
 *   Expected support model (old):
 *     UFPMax  — FPMax adapted for uncertain data (2004)
 *     UGenMax — GenMax adapted for uncertain data (2005)
 *
 *   Probabilistic support model (new):
 *     TODISMAX — Exact probabilistic mining via DP (Sun et al., 2010)
 *     APFIMAX — Approximate probabilistic mining via CLT (Chen et al., 2020)
 *
 * Usage:
 *   # Expected support algorithms:
 *   java Main -algorithm <UFPMax|UGenMax> -input <file> -output <file> [-minsup <value> | -topk <value>]
 *
 *   # Probabilistic support algorithms:
 *   java Main -algorithm <TODISMAX|APFIMAX> -input <file> -output <file> -minsup <int> -minprob <value>
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class Main {

    public static void main(String[] args) {
        // --- Parse arguments ---
        String algorithm = null;
        String inputFile = null;
        String outputFile = null;
        double minsup = -1;
        double minprob = -1;
        int topK = -1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-algorithm":
                    algorithm = args[++i];
                    break;
                case "-input":
                    inputFile = args[++i];
                    break;
                case "-output":
                    outputFile = args[++i];
                    break;
                case "-minsup":
                    minsup = Double.parseDouble(args[++i]);
                    break;
                case "-minprob":
                    minprob = Double.parseDouble(args[++i]);
                    break;
                case "-topk":
                    topK = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    return;
            }
        }

        // --- Validate ---
        if (algorithm == null || inputFile == null || outputFile == null) {
            System.err.println("Error: -algorithm, -input, and -output are required.");
            printUsage();
            return;
        }

        String algoUpper = algorithm.toUpperCase();
        boolean isProbabilistic = algoUpper.equals("TODISMAX") || algoUpper.equals("APFIMAX");

        if (isProbabilistic) {
            if (minsup < 0 || minprob < 0) {
                System.err.println("Error: " + algorithm + " requires both -minsup (integer) and -minprob.");
                printUsage();
                return;
            }
        } else {
            if (minsup < 0 && topK < 0) {
                System.err.println("Error: Either -minsup or -topk must be specified.");
                printUsage();
                return;
            }
            if (minsup >= 0 && topK >= 0) {
                System.err.println("Error: Cannot specify both -minsup and -topk.");
                printUsage();
                return;
            }
        }

        // --- Load database ---
        System.out.println("Loading database from: " + inputFile);
        UncertainDatabase database;
        try {
            database = UncertainDatabase.loadFromFile(inputFile);
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            return;
        }
        System.out.println("Loaded " + database.size() + " transactions, "
                + database.getItemCount() + " distinct items.");

        // --- Run algorithm ---
        PerformanceTracker tracker = new PerformanceTracker();
        List<Itemset> results;

        tracker.start();

        switch (algoUpper) {
            case "UFPMAX": {
                boolean useTopK = (topK > 0);
                double effectiveMinsup = useTopK ? 0.0 : minsup;
                System.out.println("Running UFPMax" + (useTopK ? " (Top-" + topK + ")" : " (minsup=" + minsup + ")"));
                UFPMax ufpmax = new UFPMax(database, effectiveMinsup, useTopK ? topK : -1);
                results = ufpmax.run();
                break;
            }
            case "UGENMAX": {
                boolean useTopK = (topK > 0);
                double effectiveMinsup = useTopK ? 0.0 : minsup;
                System.out.println("Running UGenMax" + (useTopK ? " (Top-" + topK + ")" : " (minsup=" + minsup + ")"));
                UGenMax ugenmax = new UGenMax(database, effectiveMinsup, useTopK ? topK : -1);
                results = ugenmax.run();
                break;
            }
            case "TODISMAX": {
                int minsupInt = (int) minsup;
                System.out.println("Running TODIS-MAX (minsup=" + minsupInt + ", minprob=" + minprob + ")");
                TODISMAX todismax = new TODISMAX(database, minsupInt, minprob);
                results = todismax.run();
                break;
            }
            case "APFIMAX": {
                int minsupInt = (int) minsup;
                System.out.println("Running APFI-MAX (minsup=" + minsupInt + ", minprob=" + minprob + ")");
                APFIMAX apfimax = new APFIMAX(database, minsupInt, minprob);
                results = apfimax.run();
                break;
            }
            default:
                System.err.println("Unknown algorithm: " + algorithm);
                System.err.println("Supported: UFPMax, UGenMax, TODISMAX, APFIMAX");
                return;
        }

        tracker.stop();

        // --- Output ---
        System.out.println("Found " + results.size() + " maximal frequent itemsets.");
        System.out.println("Time: " + tracker.getElapsedTimeMs() + " ms");
        System.out.println("Memory: " + tracker.getMemoryUsageMB() + " MB");

        try {
            writeResults(results, outputFile, isProbabilistic);
            System.out.println("Results written to: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }

    /**
     * Writes results in SPMF-compatible format.
     * For expected support algorithms: item1 item2 ... #SUP: expectedSupport
     * For probabilistic algorithms:    item1 item2 ... #SUP: P(sup>=minsup)
     */
    private static void writeResults(List<Itemset> results, String outputFile,
                                      boolean isProbabilistic) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (Itemset itemset : results) {
                StringBuilder sb = new StringBuilder();
                for (int item : itemset.getItems()) {
                    sb.append(item).append(" ");
                }
                if (isProbabilistic) {
                    sb.append("#PROB: ").append(String.format("%.6f", itemset.getExpectedSupport()));
                } else {
                    sb.append("#SUP: ").append(String.format("%.4f", itemset.getExpectedSupport()));
                }
                writer.println(sb.toString());
            }
        }
    }

    private static void printUsage() {
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  Expected support model:");
        System.err.println("    java Main -algorithm <UFPMax|UGenMax> -input <file> -output <file> [-minsup <value> | -topk <value>]");
        System.err.println();
        System.err.println("  Probabilistic support model:");
        System.err.println("    java Main -algorithm <TODISMAX|APFIMAX> -input <file> -output <file> -minsup <int> -minprob <value>");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  -algorithm   UFPMax, UGenMax, TODISMAX, or APFIMAX");
        System.err.println("  -input       Path to uncertain transaction database file");
        System.err.println("  -output      Path to output file for results");
        System.err.println("  -minsup      Support threshold (double for UFPMax/UGenMax, int for TODISMAX/APFIMAX)");
        System.err.println("  -minprob     Probability threshold for TODISMAX/APFIMAX (e.g., 0.5, 0.9)");
        System.err.println("  -topk        Top-K mode for UFPMax/UGenMax only");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java Main -algorithm UFPMax -input data.txt -output result.txt -minsup 1.5");
        System.err.println("  java Main -algorithm UGenMax -input data.txt -output result.txt -topk 10");
        System.err.println("  java Main -algorithm TODISMAX -input data.txt -output result.txt -minsup 2 -minprob 0.5");
        System.err.println("  java Main -algorithm APFIMAX -input data.txt -output result.txt -minsup 2 -minprob 0.5");
    }
}
