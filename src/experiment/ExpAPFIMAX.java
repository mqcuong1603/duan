package experiment;

import model.UncertainDatabase;
import model.Itemset;
import algorithm.APFIMAX;
import util.PerformanceTracker;

import java.io.*;
import java.util.List;

/**
 * Experiment class for APFI-MAX algorithm.
 * Reads an uncertain database, runs APFI-MAX with CLT-based probabilistic support,
 * and outputs results with statistics.
 *
 * Usage:
 *   java experiment.ExpAPFIMAX <input_file> <minsup> <minprob> [output_file]
 *
 * @author Ma Quoc Cuong, Nguyen Cao Phi
 */
public class ExpAPFIMAX {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: java experiment.ExpAPFIMAX <input_file> <minsup> <minprob> [output_file]");
            return;
        }

        String inputFile = args[0];
        int minsup = Integer.parseInt(args[1]);
        double minprob = Double.parseDouble(args[2]);
        String outputFile = args.length >= 4 ? args[3] : null;

        System.out.println("Dataset: " + inputFile);
        UncertainDatabase db = UncertainDatabase.loadFromFile(inputFile);
        System.out.println("Transactions: " + db.size());
        System.out.println("Distinct items: " + db.getItemCount());
        System.out.println("Algorithm: APFI-MAX");
        System.out.println("Minsup: " + minsup);
        System.out.println("Minprob: " + minprob);
        System.out.println();

        PerformanceTracker tracker = new PerformanceTracker();
        tracker.start();
        APFIMAX algorithm = new APFIMAX(db, minsup, minprob);
        List<Itemset> results = algorithm.run();
        tracker.stop();

        System.out.println();
        System.out.println("=== Results ===");
        System.out.println("Maximal p-frequent itemsets found: " + results.size());
        System.out.println("Execution time: " + tracker.getElapsedTimeMs() + " ms (" + String.format("%.2f", tracker.getElapsedTimeSec()) + " s)");
        System.out.println("Memory usage: " + String.format("%.2f", tracker.getMemoryUsageMB()) + " MB");
        System.out.println("Candidates generated: " + algorithm.getCandidatesGenerated());
        System.out.println("Pruned by count: " + algorithm.getPrunedByCnt());
        System.out.println("Pruned by expectation bound: " + algorithm.getPrunedByExpBound());
        System.out.println("Confirmed by upper bound: " + algorithm.getConfirmedByUB());
        System.out.println("Confirmed by CLT: " + algorithm.getConfirmedByCLT());

        if (results.size() <= 50) {
            System.out.println();
            System.out.println("--- Itemsets ---");
            for (Itemset is : results) {
                StringBuilder sb = new StringBuilder();
                for (int item : is.getItems()) sb.append(item).append(" ");
                sb.append("#PROB: ").append(String.format("%.6f", is.getExpectedSupport()));
                System.out.println(sb);
            }
        }

        if (outputFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                for (Itemset is : results) {
                    StringBuilder sb = new StringBuilder();
                    for (int item : is.getItems()) sb.append(item).append(" ");
                    sb.append("#PROB: ").append(String.format("%.6f", is.getExpectedSupport()));
                    writer.println(sb);
                }
            }
            System.out.println("Results written to: " + outputFile);
        }
    }
}
