package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Represents an uncertain transaction database loaded from a text file.
 * 
 * Each transaction contains items with existential probabilities.
 * The database provides methods to compute expected support for
 * individual items and itemsets across all transactions.
 * 
 * File format:
 *   Each line is a transaction. Items are separated by spaces.
 *   Each item is followed by its probability in parentheses.
 *   Lines starting with '#' or '@' are comments/metadata and are ignored.
 * 
 * Example file:
 *   1(0.8) 3(0.6) 4(0.9)
 *   2(0.7) 3(0.5) 5(0.9)
 *   1(0.6) 2(0.8) 3(0.7) 5(0.85)
 *   2(0.5) 5(0.6)
 *   1(0.9) 2(0.75) 3(0.8) 5(0.7)
 * 
 * @author Mã Quốc Cường, Nguyễn Cao Phi
 */
public class UncertainDatabase {

    /** List of all transactions in the database */
    private final List<UncertainTransaction> transactions;

    /** Set of all distinct items in the database */
    private final Set<Integer> allItems;

    /** Cache: expected support of each single item (item → expSup) */
    private final Map<Integer, Double> singleItemExpSupport;

    /**
     * Creates an UncertainDatabase from a list of transactions.
     *
     * @param transactions list of uncertain transactions
     */
    public UncertainDatabase(List<UncertainTransaction> transactions) {
        this.transactions = new ArrayList<>(transactions);
        this.allItems = new TreeSet<>();
        this.singleItemExpSupport = new HashMap<>();

        // Collect all distinct items and compute single-item expected supports
        for (UncertainTransaction t : transactions) {
            for (int item : t.getItems()) {
                allItems.add(item);
                singleItemExpSupport.merge(item, t.getProbability(item), Double::sum);
            }
        }
    }

    /**
     * Loads an uncertain transaction database from a text file.
     * 
     * Parsing rules:
     *   - Lines starting with '#' or '@' are skipped (comments/metadata)
     *   - Empty lines are skipped
     *   - Each item is in the format: itemId(probability)
     *   - Items are separated by one or more spaces
     *
     * @param filePath path to the input file
     * @return the loaded UncertainDatabase
     * @throws IOException if the file cannot be read
     * @throws NumberFormatException if the file format is invalid
     */
    public static UncertainDatabase loadFromFile(String filePath) throws IOException {
        List<UncertainTransaction> transactions = new ArrayList<>();
        int transactionId = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) {
                    continue;
                }

                // Parse the transaction
                Map<Integer, Double> itemProbs = parseTransaction(line, transactionId);

                if (!itemProbs.isEmpty()) {
                    transactions.add(new UncertainTransaction(transactionId, itemProbs));
                    transactionId++;
                }
            }
        }

        return new UncertainDatabase(transactions);
    }

    /**
     * Parses a single line of the input file into a map of item → probability.
     * 
     * Supports two formats:
     *   Format 1 (uncertain): "1(0.8) 3(0.6) 4(0.9)"
     *   Format 2 (standard, treated as probability 1.0): "1 3 4"
     *
     * @param line the line to parse
     * @param transactionId ID for error messages
     * @return map of item → probability
     */
    private static Map<Integer, Double> parseTransaction(String line, int transactionId) {
        Map<Integer, Double> itemProbs = new LinkedHashMap<>();
        String[] tokens = line.split("\\s+");

        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) continue;

            int parenOpen = token.indexOf('(');
            if (parenOpen != -1) {
                // Format: item(probability)
                int parenClose = token.indexOf(')');
                if (parenClose == -1) {
                    throw new NumberFormatException(
                        "Invalid format at transaction " + transactionId + ": " + token
                        + " (missing closing parenthesis)");
                }

                int item = Integer.parseInt(token.substring(0, parenOpen));
                double prob = Double.parseDouble(token.substring(parenOpen + 1, parenClose));

                // Validate probability
                if (prob <= 0.0 || prob > 1.0) {
                    throw new IllegalArgumentException(
                        "Probability must be in (0, 1] at transaction " + transactionId
                        + ", item " + item + ": " + prob);
                }

                itemProbs.put(item, prob);
            } else {
                // Format: plain item (no probability → assume 1.0, standard database)
                int item = Integer.parseInt(token);
                itemProbs.put(item, 1.0);
            }
        }

        return itemProbs;
    }

    /**
     * Returns the number of transactions in the database.
     *
     * @return number of transactions
     */
    public int size() {
        return transactions.size();
    }

    /**
     * Returns the number of distinct items across all transactions.
     *
     * @return number of distinct items
     */
    public int getItemCount() {
        return allItems.size();
    }

    /**
     * Returns all distinct items in the database, sorted.
     *
     * @return sorted set of all item IDs
     */
    public Set<Integer> getAllItems() {
        return Collections.unmodifiableSet(allItems);
    }

    /**
     * Returns the list of all transactions.
     *
     * @return unmodifiable list of transactions
     */
    public List<UncertainTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Returns a specific transaction by ID.
     *
     * @param transactionId the transaction ID (0-indexed)
     * @return the transaction
     */
    public UncertainTransaction getTransaction(int transactionId) {
        return transactions.get(transactionId);
    }

    /**
     * Returns the expected support of a single item across the entire database.
     * This is precomputed during construction for efficiency.
     * 
     * expSup(item) = Σ P(item, ti) for all transactions ti
     *
     * @param item the item ID
     * @return expected support, or 0.0 if item doesn't exist
     */
    public double getItemExpectedSupport(int item) {
        return singleItemExpSupport.getOrDefault(item, 0.0);
    }

    /**
     * Computes the expected support of an itemset across the entire database.
     * 
     * expSup(X) = Σ [ Π P(item, ti) for each item in X ] for all transactions ti
     * 
     * For each transaction, the expected support contribution is the product
     * of probabilities of all items in the itemset. The total expected support
     * is the sum across all transactions.
     *
     * @param itemset array of item IDs
     * @return expected support of the itemset
     */
    public double computeExpectedSupport(int[] itemset) {
        double totalExpSup = 0.0;

        for (UncertainTransaction t : transactions) {
            double contribution = t.computeExpectedSupport(itemset);
            totalExpSup += contribution;
        }

        return totalExpSup;
    }

    /**
     * Returns all items whose single-item expected support >= minsup,
     * sorted by expected support in descending order.
     * 
     * This is used as the first pruning step: items that are infrequent
     * by themselves cannot be part of any frequent itemset.
     *
     * @param minsup minimum expected support threshold
     * @return list of frequent items, sorted by descending expected support
     */
    public List<Integer> getFrequentItemsSorted(double minsup) {
        List<Integer> frequentItems = new ArrayList<>();

        for (Map.Entry<Integer, Double> entry : singleItemExpSupport.entrySet()) {
            if (entry.getValue() >= minsup) {
                frequentItems.add(entry.getKey());
            }
        }

        // Sort by expected support descending (most frequent first)
        frequentItems.sort((a, b) -> 
            Double.compare(singleItemExpSupport.get(b), singleItemExpSupport.get(a)));

        return frequentItems;
    }

    /**
     * Returns a string representation showing database statistics.
     *
     * @return string with database info
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UncertainDatabase: ").append(transactions.size()).append(" transactions, ");
        sb.append(allItems.size()).append(" items\n");
        for (UncertainTransaction t : transactions) {
            sb.append("  ").append(t).append("\n");
        }
        return sb.toString();
    }
}