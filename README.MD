# Mining Top-K Frequent Maximal Itemsets in Uncertain Transaction Databases

A comparative study of **UFPMax** and **UGenMax** — two algorithms for discovering
frequent maximal itemsets (MFIs) from uncertain transaction databases, supporting
both static minimum expected support and dynamic Top-K threshold modes.

In an uncertain database, each item in a transaction has an existential probability
`P(item, t) in (0, 1]`. The **expected support** of an itemset X is:

```
expSup(X) = Sum over all transactions t of: Product of P(i, t) for each item i in X
```

An itemset is **maximal frequent** if its expected support >= minsup and no proper
superset is also frequent.

---

## Project Structure

```
duan/
├── src/
│   ├── Main.java                          # CLI entry point
│   ├── algorithm/
│   │   ├── UFPMax.java                    # FPMax adapted for uncertain data
│   │   ├── UGenMax.java                   # GenMax adapted for uncertain data
│   │   └── genmax/
│   │       └── WeightedTidset.java        # Weighted tidset for UGenMax
│   ├── model/
│   │   ├── Itemset.java                   # Itemset with expected support
│   │   ├── TopKHeap.java                  # Min-heap for Top-K tracking
│   │   ├── UncertainDatabase.java         # Database loader and manager
│   │   └── UncertainTransaction.java      # Single transaction with probabilities
│   └── util/
│       └── PerformanceTracker.java        # Time and memory measurement
├── data/
│   ├── contextUncertain.txt               # Small test dataset (5 transactions, 5 items)
│   ├── mushroom_medium.txt                # Medium benchmark (8416 transactions, 120 items)
│   ├── mushroom_low.txt                   # Low-uncertainty mushroom variant
│   └── mushroom_high.txt                  # High-uncertainty mushroom variant
└── README.MD
```

---

## Requirements

| Requirement       | Details                                      |
|-------------------|----------------------------------------------|
| **Java**          | SE 21+                                       |
| **External libs** | None                                         |
| **Build tools**   | None (no Maven / Gradle)                     |

---

## Build

```bash
mkdir -p bin
javac -d bin src/util/*.java src/model/*.java src/algorithm/genmax/*.java src/algorithm/*.java src/Main.java
```

---

## Run

### Static minimum expected support

```bash
# UFPMax with minsup = 1.5
java -cp bin Main -algorithm UFPMax -input data/contextUncertain.txt -output output.txt -minsup 1.5

# UGenMax with minsup = 1.5
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output output.txt -minsup 1.5
```

### Top-K mode (dynamic threshold)

```bash
# Find the Top-10 maximal frequent itemsets with highest expected support
java -cp bin Main -algorithm UFPMax -input data/mushroom_medium.txt -output output.txt -topk 10
java -cp bin Main -algorithm UGenMax -input data/mushroom_medium.txt -output output.txt -topk 10
```

### CLI options

| Option       | Description                                                 |
|--------------|-------------------------------------------------------------|
| `-algorithm` | Algorithm to use: `UFPMax` or `UGenMax`                     |
| `-input`     | Path to the uncertain transaction database file             |
| `-output`    | Path to write results (SPMF-compatible format)              |
| `-minsup`    | Minimum expected support threshold (e.g., `1.5`, `500`)     |
| `-topk`      | Number of top maximal frequent itemsets to return            |

Either `-minsup` or `-topk` must be specified (not both).

---

## Input File Format

Each line is one transaction. Items are separated by spaces. Each item is
optionally followed by its existential probability in parentheses.
Lines starting with `#` or `@` are ignored (comments/metadata).

**Uncertain transactions:**
```
1(0.8) 3(0.6) 4(0.9)
2(0.7) 3(0.5) 5(0.9)
1(0.6) 2(0.8) 3(0.7) 5(0.85)
2(0.5) 5(0.6)
1(0.9) 2(0.75) 3(0.8) 5(0.7)
```

**Standard (deterministic) transactions** — items without probabilities are
treated as having probability 1.0:
```
1 3 4
2 3 5
1 2 3 5
```

---

## Output File Format

SPMF-compatible format. Each line is one maximal frequent itemset:

```
item1 item2 ... itemN #SUP: expectedSupport
```

Example:
```
1 2 3 5 #SUP: 1.6200
1 3 4 #SUP: 1.6020
```

---

## Algorithms

Both algorithms find the same set of maximal frequent itemsets for a given
minsup threshold. They differ in enumeration strategy and data structures.

### UGenMax — GenMax for Uncertain Data

Based on **GenMax** (Gouda & Zaki, 2005). Bottom-up enumeration using weighted
tidset intersections.

**Key characteristics:**
- **Data structure:** Weighted tidsets — each item maps to a list of
  `(transactionId, probability)` pairs
- **Enumeration:** Depth-first backtracking through the itemset lattice.
  Items sorted by expected support ascending (least frequent first).
  Candidates processed most-frequent-first for earlier discovery of large MFIs.
- **Expected support:** Computed via tidset intersection — intersecting two
  weighted tidsets multiplies per-transaction probabilities and sums the products.
- **Top-K mode:** Dynamic threshold raising — as new MFIs enter the Top-K heap,
  minsup increases during search, strengthening anti-monotone pruning.

### UFPMax — FPMax for Uncertain Data

Based on **FPMax** (Grahne & Zhu, 2003). Top-down enumeration using conditional
database projection.

**Key characteristics:**
- **Data structure:** Projected transactions — each carries a weight
  (accumulated probability product of conditioning items) plus remaining items
  with their per-transaction probabilities.
- **Enumeration:** Recursive conditional database construction. Items sorted by
  expected support descending (FP-tree ordering). Items processed from least to
  most frequent in each recursive call.
- **Expected support:** Computed by summing `weight * P(item, t)` across all
  projected transactions containing the item.
- **Top-K mode:** Fixed threshold during search, Top-K selection after search
  completes. FPMax's processing order (least frequent first) discovers
  single-item MFIs early, which would raise the threshold prematurely if
  dynamic raising were used.

### Shared Pruning Strategies

Both algorithms use three pruning rules:

1. **Anti-monotonicity:** If `expSup(prefix U {item}) < minsup`, prune the
   subtree. Adding items can only decrease expected support since each
   probability is in (0, 1].

2. **Progressive Focusing:** If `prefix U {all remaining candidates}` is a
   subset of a known MFI, prune the subtree — no new maximal itemset can be
   discovered.

3. **Top-K Threshold Raising:** In Top-K mode, the K-th best expected support
   in the heap serves as a dynamic minsup. As better MFIs are found, minsup
   increases, strengthening pruning rule 1. (UGenMax applies this during
   search; UFPMax applies it post-search.)

### Top-K Threshold Seeding

Both algorithms seed the Top-K heap with single-item and 2-item expected
supports before the main search. This establishes a reasonable initial minsup
(set to 50% of the seeded threshold) so the search space is tractable from the
start. The heap is then reset before the main search begins.

If fewer than K results are found, the algorithm retries with progressively
lower thresholds (25% of previous).

---

## Example Results

### contextUncertain.txt (5 transactions, 5 items) with minsup = 1.0

Both algorithms produce the same 4 maximal frequent itemsets:

```
{1, 3, 4} (expSup=1.6020)
{2, 5}    (expSup=1.8575)
{1, 2, 3, 5} (expSup=1.6200)
{2, 3, 5}    (expSup=1.7390)
```
