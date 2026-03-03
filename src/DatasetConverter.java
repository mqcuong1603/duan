import java.io.*;
import java.util.Random;

/**
 * Converts a standard SPMF transaction database to an uncertain transaction database
 * by assigning random existential probabilities to each item in each transaction.
 *
 * This is the standard approach used in uncertain data mining research papers
 * (Chui et al., 2007; Leung et al., 2008; Li & Zhang, 2019) to create
 * uncertain benchmark datasets from well-known standard datasets.
 *
 * Probability distribution options:
 *   - "high"    : Normal(mean=0.8, std=0.1), clamped to (0.1, 1.0)
 *                 Simulates data with low uncertainty (most items likely exist)
 *   - "medium"  : Normal(mean=0.5, std=0.2), clamped to (0.1, 1.0)
 *                 Simulates moderate uncertainty
 *   - "low"     : Normal(mean=0.3, std=0.15), clamped to (0.1, 1.0)
 *                 Simulates high uncertainty (many items may not exist)
 *   - "uniform" : Uniform(0.1, 1.0)
 *                 Equal chance of any probability
 *
 * Usage:
 *   java DatasetConverter <input> <output> [distribution] [seed]
 *
 * Examples:
 *   java DatasetConverter mushroom.txt mushroom_uncertain.txt high
 *   java DatasetConverter retail.txt retail_uncertain.txt medium 42
 *   java DatasetConverter chess.txt chess_uncertain.txt uniform
 *
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class DatasetConverter {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java DatasetConverter <input> <output> [distribution] [seed]");
            System.err.println();
            System.err.println("Distributions: high (default), medium, low, uniform");
            System.err.println("Seed: integer for reproducible results (default: random)");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  java DatasetConverter mushroom.txt mushroom_uncertain_high.txt high");
            System.err.println("  java DatasetConverter retail.txt retail_uncertain_medium.txt medium 42");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        String distribution = args.length >= 3 ? args[2].toLowerCase() : "high";
        long seed = args.length >= 4 ? Long.parseLong(args[3]) : System.currentTimeMillis();

        Random random = new Random(seed);

        int transactionCount = 0;
        int itemCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {

            // Write header comment
            writer.println("# Uncertain transaction database");
            writer.println("# Converted from: " + inputFile);
            writer.println("# Distribution: " + distribution);
            writer.println("# Seed: " + seed);

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) {
                    continue;
                }

                // Parse items from the standard format
                String[] tokens = line.split("\\s+");
                StringBuilder sb = new StringBuilder();

                for (String token : tokens) {
                    token = token.trim();
                    if (token.isEmpty()) continue;

                    // Skip if already has probability (already uncertain format)
                    if (token.contains("(")) {
                        sb.append(token).append(" ");
                    } else {
                        int item = Integer.parseInt(token);
                        double prob = generateProbability(random, distribution);
                        sb.append(item).append("(").append(String.format("%.2f", prob)).append(") ");
                    }
                    itemCount++;
                }

                writer.println(sb.toString().trim());
                transactionCount++;

                // Progress indicator for large files
                if (transactionCount % 10000 == 0) {
                    System.out.println("  Processed " + transactionCount + " transactions...");
                }
            }
        }

        System.out.println("Conversion complete!");
        System.out.println("  Input:         " + inputFile);
        System.out.println("  Output:        " + outputFile);
        System.out.println("  Distribution:  " + distribution);
        System.out.println("  Transactions:  " + transactionCount);
        System.out.println("  Total items:   " + itemCount);
        System.out.println("  Seed:          " + seed);
    }

    /**
     * Generates a random probability value based on the chosen distribution.
     * All values are clamped to the range [0.10, 1.00].
     *
     * @param random the random number generator
     * @param distribution the distribution type
     * @return a probability value in [0.10, 1.00]
     */
    private static double generateProbability(Random random, String distribution) {
        double prob;

        switch (distribution) {
            case "high":
                // Normal(0.8, 0.1) — low uncertainty
                prob = 0.8 + random.nextGaussian() * 0.1;
                break;
            case "medium":
                // Normal(0.5, 0.2) — moderate uncertainty
                prob = 0.5 + random.nextGaussian() * 0.2;
                break;
            case "low":
                // Normal(0.3, 0.15) — high uncertainty
                prob = 0.3 + random.nextGaussian() * 0.15;
                break;
            case "uniform":
                // Uniform(0.1, 1.0)
                prob = 0.1 + random.nextDouble() * 0.9;
                break;
            default:
                System.err.println("Warning: Unknown distribution '" + distribution + "', using 'high'");
                prob = 0.8 + random.nextGaussian() * 0.1;
        }

        // Clamp to [0.10, 1.00]
        return Math.max(0.10, Math.min(1.00, Math.round(prob * 100.0) / 100.0));
    }
}