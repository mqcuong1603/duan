package experiment;

import model.UncertainDatabase;
import model.Itemset;
import algorithm.UGenMax;
import util.PerformanceTracker;

import java.io.*;
import java.util.List;

/**
 * Experiment class for UGenMax algorithm.
 * Reads an uncertain database, runs UGenMax, and outputs results with statistics.
 *
 * Usage:
 *   java experiment.ExpUGenMax <input_file> <minsup> [output_file]
 *
 * @author Ma Quoc Cuong, Nguyen Cao Phi
 */
public class ExpUGenMax {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java experiment.ExpUGenMax <input_file> <minsup> [output_file]");
            return;
        }

        String inputFile = args[0];
        double minsup = Double.parseDouble(args[1]);
        String outputFile = args.length >= 3 ? args[2] : null;

        System.out.println("Dataset: " + inputFile);
        UncertainDatabase db = UncertainDatabase.loadFromFile(inputFile);
        System.out.println("Transactions: " + db.size());
        System.out.println("Distinct items: " + db.getItemCount());
        System.out.println("Algorithm: UGenMax");
        System.out.println("Minsup: " + minsup);
        System.out.println();

        PerformanceTracker tracker = new PerformanceTracker();
        tracker.start();
        UGenMax algorithm = new UGenMax(db, minsup, -1);
        List<Itemset> results = algorithm.run();
        tracker.stop();

        System.out.println();
        System.out.println("=== Results ===");
        System.out.println("Maximal frequent itemsets found: " + results.size());
        System.out.println("Execution time: " + tracker.getElapsedTimeMs() + " ms (" + String.format("%.2f", tracker.getElapsedTimeSec()) + " s)");
        System.out.println("Memory usage: " + String.format("%.2f", tracker.getMemoryUsageMB()) + " MB");

        if (results.size() <= 50) {
            System.out.println();
            System.out.println("--- Itemsets ---");
            for (Itemset is : results) {
                StringBuilder sb = new StringBuilder();
                for (int item : is.getItems()) sb.append(item).append(" ");
                sb.append("#SUP: ").append(String.format("%.4f", is.getExpectedSupport()));
                System.out.println(sb);
            }
        }

        if (outputFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                for (Itemset is : results) {
                    StringBuilder sb = new StringBuilder();
                    for (int item : is.getItems()) sb.append(item).append(" ");
                    sb.append("#SUP: ").append(String.format("%.4f", is.getExpectedSupport()));
                    writer.println(sb);
                }
            }
            System.out.println("Results written to: " + outputFile);
        }
    }
}
