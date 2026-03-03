import model.UncertainDatabase;
import model.Itemset;
import model.TopKHeap;
import algorithm.UFPMax;
import algorithm.UGenMax;
import util.PerformanceTracker;

import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Main entry point for the Top-K Frequent Maximal Itemset Mining program.
 * 
 * Supports two algorithms (UFPMax and UGenMax) for mining frequent maximal
 * itemsets from uncertain transaction databases, with both static minsup
 * and dynamic Top-K threshold modes.
 * 
 * Usage:
 *   java Main -algorithm <UFPMax|UGenMax> -input <file> -output <file> [-minsup <value> | -topk <value>]
 * 
 * Examples:
 *   java Main -algorithm UFPMax -input data.txt -output result.txt -minsup 1.5
 *   java Main -algorithm UGenMax -input data.txt -output result.txt -topk 10
 * 
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class Main {

    /**
     * Parses command-line arguments and runs the selected algorithm.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        // --- Step 1: Parse command-line arguments ---
        String algorithm = null;
        String inputFile = null;
        String outputFile = null;
        double minsup = -1;
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
                case "-topk":
                    topK = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    return;
            }
        }

        // --- Step 2: Validate arguments ---
        if (algorithm == null || inputFile == null || outputFile == null) {
            System.err.println("Error: -algorithm, -input, and -output are required.");
            printUsage();
            return;
        }

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

        // --- Step 3: Load the uncertain database ---
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

        // --- Step 4: Run the selected algorithm ---
        PerformanceTracker tracker = new PerformanceTracker();
        List<Itemset> results;

        tracker.start();

        boolean useTopK = (topK > 0);
        double effectiveMinsup = useTopK ? 0.0 : minsup;

        switch (algorithm.toUpperCase()) {
            case "UFPMAX":
                System.out.println("Running UFPMax" + (useTopK ? " (Top-" + topK + ")" : " (minsup=" + minsup + ")"));
                UFPMax ufpmax = new UFPMax(database, effectiveMinsup, useTopK ? topK : -1);
                results = ufpmax.run();
                break;
            case "UGENMAX":
                System.out.println("Running UGenMax" + (useTopK ? " (Top-" + topK + ")" : " (minsup=" + minsup + ")"));
                UGenMax ugenmax = new UGenMax(database, effectiveMinsup, useTopK ? topK : -1);
                results = ugenmax.run();
                break;
            default:
                System.err.println("Unknown algorithm: " + algorithm);
                System.err.println("Supported: UFPMax, UGenMax");
                return;
        }

        tracker.stop();

        // --- Step 5: Write results ---
        System.out.println("Found " + results.size() + " maximal frequent itemsets.");
        System.out.println("Time: " + tracker.getElapsedTimeMs() + " ms");
        System.out.println("Memory: " + tracker.getMemoryUsageMB() + " MB");

        try {
            writeResults(results, outputFile);
            System.out.println("Results written to: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }

    /**
     * Writes the mining results to a file in SPMF-compatible format.
     * Each line: item1 item2 ... itemN #SUP: expectedSupport
     *
     * @param results list of maximal frequent itemsets
     * @param outputFile path to the output file
     * @throws IOException if writing fails
     */
    private static void writeResults(List<Itemset> results, String outputFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (Itemset itemset : results) {
                StringBuilder sb = new StringBuilder();
                for (int item : itemset.getItems()) {
                    sb.append(item).append(" ");
                }
                sb.append("#SUP: ").append(String.format("%.4f", itemset.getExpectedSupport()));
                writer.println(sb.toString());
            }
        }
    }

    /**
     * Prints usage instructions to standard error.
     */
    private static void printUsage() {
        System.err.println();
        System.err.println("Usage: java Main -algorithm <UFPMax|UGenMax> -input <file> -output <file> [-minsup <value> | -topk <value>]");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  -algorithm   Algorithm to use: UFPMax or UGenMax");
        System.err.println("  -input       Path to uncertain transaction database file");
        System.err.println("  -output      Path to output file for results");
        System.err.println("  -minsup      Minimum expected support threshold (e.g., 1.5)");
        System.err.println("  -topk        Number of top-K maximal frequent itemsets to find");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java Main -algorithm UFPMax -input data.txt -output result.txt -minsup 1.5");
        System.err.println("  java Main -algorithm UGenMax -input data.txt -output result.txt -topk 10");
    }
}