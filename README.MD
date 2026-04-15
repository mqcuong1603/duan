8# Mining Frequent Maximal Itemsets from Uncertain Transaction Databases

Four algorithms for discovering maximal frequent itemsets (MFIs) from uncertain
transaction databases, covering two support models:

- **Expected support model:** UGenMax, UFPMax
- **Probabilistic support model:** TODIS-MAX, APFI-MAX

## Requirements

- **Java** SE 21+ (developed on SE 25)
- No external libraries, no Maven/Gradle

## Project Structure

```
duan/
├── src/
│   ├── Main.java                       # CLI entry point (all 4 algorithms)
│   ├── DatasetConverter.java           # Convert standard datasets to uncertain format
│   ├── algorithm/
│   │   ├── UFPMax.java                 # FPMax for uncertain data (expected support)
│   │   ├── UGenMax.java                # GenMax for uncertain data (expected support)
│   │   ├── TODISMAX.java               # Exact probabilistic mining via DP
│   │   ├── APFIMAX.java                # Approximate probabilistic mining via CLT
│   │   ├── SupportPMF.java             # PMF/CLT utilities for probabilistic support
│   │   └── genmax/
│   │       └── WeightedTidset.java     # Weighted tidset data structure
│   ├── model/
│   │   ├── Itemset.java                # Itemset with support value
│   │   ├── TopKHeap.java               # Min-heap for Top-K tracking
│   │   ├── UncertainDatabase.java      # Database loader
│   │   └── UncertainTransaction.java   # Single transaction with probabilities
│   ├── experiment/
│   │   ├── ExperimentRunner.java       # Automated benchmark runner (all experiments)
│   │   ├── ExpUFPMax.java              # Individual UFPMax experiment
│   │   ├── ExpUGenMax.java             # Individual UGenMax experiment
│   │   ├── ExpTODISMAX.java            # Individual TODIS-MAX experiment
│   │   └── ExpAPFIMAX.java             # Individual APFI-MAX experiment
│   └── util/
│       └── PerformanceTracker.java     # Time and memory measurement
├── data/
│   ├── contextUncertain.txt            # Small test (5 txns, 5 items)
│   ├── mushroom.txt                    # Standard mushroom (8,416 txns)
│   ├── mushroom_high.txt               # Mushroom, high certainty (mu=0.8)
│   ├── mushroom_medium.txt             # Mushroom, medium certainty (mu=0.5)
│   ├── mushroom_low.txt                # Mushroom, low certainty (mu=0.3)
│   ├── retail.txt                      # Standard retail (88,162 txns)
│   └── accidents.txt                   # Standard accidents (340,183 txns)
├── main.tex                            # LaTeX report
└── README.MD
```

## Quick Start

### 1. Build

```bash
mkdir -p bin
javac -d bin src/util/*.java src/model/*.java src/algorithm/genmax/*.java src/algorithm/*.java src/Main.java src/DatasetConverter.java src/experiment/*.java

```

### 2. Generate uncertain datasets

The repo includes `mushroom_high/medium/low.txt` already. To generate retail
and accidents variants:

```bash
# Retail (88,162 transactions)
java -cp bin DatasetConverter data/retail.txt data/retail_high.txt high 42
java -cp bin DatasetConverter data/retail.txt data/retail_medium.txt medium 42
java -cp bin DatasetConverter data/retail.txt data/retail_low.txt low 42

# Accidents (340,183 transactions) — takes a minute
java -cp bin DatasetConverter data/accidents.txt data/accidents_high.txt high 42
java -cp bin DatasetConverter data/accidents.txt data/accidents_medium.txt medium 42
java -cp bin DatasetConverter data/accidents.txt data/accidents_low.txt low 42
```

Distribution options: `high` (N(0.8, 0.1)), `medium` (N(0.5, 0.2)), `low` (N(0.3, 0.15)), `uniform`.

### 3. Run algorithms via Main CLI

```bash
# --- Expected support model (UFPMax / UGenMax) ---

# Static minsup
java -cp bin Main -algorithm UFPMax -input data/contextUncertain.txt -output output.txt -minsup 1.5
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output output.txt -minsup 1.5

# Top-K mode
java -cp bin Main -algorithm UGenMax -input data/mushroom_medium.txt -output output.txt -topk 10

# --- Probabilistic support model (TODISMAX / APFIMAX) ---

# Requires both -minsup (integer) and -minprob
java -cp bin Main -algorithm TODISMAX -input data/mushroom_medium.txt -output output.txt -minsup 3000 -minprob 0.5
java -cp bin Main -algorithm APFIMAX -input data/mushroom_medium.txt -output output.txt -minsup 3000 -minprob 0.5
```

### 4. Run individual experiments (with stats output)

Each experiment class loads a dataset, runs one algorithm, and prints results
plus performance statistics (time, memory, pruning counts).

```bash
# Expected support experiments
java -cp bin experiment.ExpUFPMax data/mushroom_medium.txt 2000
java -cp bin experiment.ExpUGenMax data/mushroom_medium.txt 2000

# Probabilistic experiments
java -cp bin experiment.ExpTODISMAX data/mushroom_medium.txt 3000 0.5
java -cp bin experiment.ExpAPFIMAX data/mushroom_medium.txt 3000 0.5
```

### 5. Run all benchmarks at once

This runs all 4 experiments across all datasets and parameter configurations,
outputting CSV files to the `results/` directory:

```bash
java -Xmx4g -cp bin experiment.ExperimentRunner results
```

Outputs:

- `results/exp1_expected_support.csv` — UFPMax vs UGenMax across minsup values
- `results/exp2_probabilistic.csv` — TODISMAX vs APFIMAX across minsup/minprob
- `results/exp3_uncertainty.csv` — High/medium/low uncertainty comparison
- `results/exp4_scalability.csv` — Mushroom vs retail vs accidents

## Testing the Entire Project

Here is a step-by-step script to build, generate data, and verify all algorithms:

```bash
#!/bin/bash
set -e

echo "=== Step 1: Build ==="
mkdir -p bin
javac -d bin \
  src/util/*.java \
  src/model/*.java \
  src/algorithm/genmax/*.java \
  src/algorithm/*.java \
  src/Main.java \
  src/DatasetConverter.java \
  src/experiment/*.java
echo "Build OK"

echo ""
echo "=== Step 2: Generate uncertain datasets ==="
java -cp bin DatasetConverter data/retail.txt data/retail_medium.txt medium 42
java -cp bin DatasetConverter data/accidents.txt data/accidents_medium.txt medium 42
echo "Datasets generated"

echo ""
echo "=== Step 3: Verify correctness on small dataset ==="
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output /tmp/ufpmax_out.txt -minsup 1.0
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output /tmp/ugenmax_out.txt -minsup 1.0
echo "UFPMax results:"
cat /tmp/ufpmax_out.txt
echo ""
echo "UGenMax results:"
cat /tmp/ugenmax_out.txt
echo ""

# Check both produce identical output
if diff -q /tmp/ufpmax_out.txt /tmp/ugenmax_out.txt > /dev/null 2>&1; then
  echo "PASS: UFPMax and UGenMax produce identical results"
else
  echo "FAIL: Results differ!"
fi

echo ""
echo "=== Step 4: Test probabilistic algorithms ==="
java -cp bin Main -algorithm TODISMAX -input data/contextUncertain.txt -output /tmp/todis_out.txt -minsup 2 -minprob 0.5
java -cp bin Main -algorithm APFIMAX -input data/contextUncertain.txt -output /tmp/apfi_out.txt -minsup 2 -minprob 0.5
echo "TODIS-MAX results:"
cat /tmp/todis_out.txt
echo ""
echo "APFI-MAX results:"
cat /tmp/apfi_out.txt

echo ""
echo "=== Step 5: Benchmark on mushroom ==="
java -cp bin experiment.ExpUFPMax data/mushroom_medium.txt 2000
echo ""
java -cp bin experiment.ExpUGenMax data/mushroom_medium.txt 2000
echo ""
java -cp bin experiment.ExpTODISMAX data/mushroom_medium.txt 3000 0.5
echo ""
java -cp bin experiment.ExpAPFIMAX data/mushroom_medium.txt 3000 0.5

echo ""
echo "=== Step 6: Benchmark on retail ==="
java -cp bin experiment.ExpUFPMax data/retail_medium.txt 3000
echo ""
java -cp bin experiment.ExpUGenMax data/retail_medium.txt 3000

echo ""
echo "=== Step 7: Full experiment suite (optional, takes a few minutes) ==="
# Uncomment the next line to run all benchmarks:
# java -Xmx4g -cp bin experiment.ExperimentRunner results

echo ""
echo "=== All tests complete ==="
```

Save this as `test.sh`, then run:

```bash
chmod +x test.sh
./test.sh
```

## CLI Options

| Option       | Description                                                       |
| ------------ | ----------------------------------------------------------------- |
| `-algorithm` | `UFPMax`, `UGenMax`, `TODISMAX`, or `APFIMAX`                     |
| `-input`     | Path to uncertain transaction database file                       |
| `-output`    | Path to write results                                             |
| `-minsup`    | Support threshold (double for UFPMax/UGenMax, int for TODIS/APFI) |
| `-minprob`   | Probability threshold for TODISMAX/APFIMAX (e.g., 0.5, 0.9)       |
| `-topk`      | Top-K mode for UFPMax/UGenMax only                                |

## Input Format

Each line is one transaction. Items followed by probability in parentheses:

```
1(0.8) 3(0.6) 4(0.9)
2(0.7) 3(0.5) 5(0.9)
1(0.6) 2(0.8) 3(0.7) 5(0.85)
```

Items without parentheses are treated as probability 1.0.

## Output Format

```
item1 item2 ... #SUP: expectedSupport     (UFPMax/UGenMax)
item1 item2 ... #PROB: probability         (TODISMAX/APFIMAX)
```

## Algorithms Overview

| Algorithm | Support Model    | Strategy                               | Speed                        |
| --------- | ---------------- | -------------------------------------- | ---------------------------- |
| UGenMax   | Expected support | Bottom-up tidset intersection          | Fastest for expected support |
| UFPMax    | Expected support | Top-down conditional projection        | Slightly slower              |
| TODIS-MAX | Probabilistic    | Exact DP (O(nT) per candidate)         | Slower but exact             |
| APFI-MAX  | Probabilistic    | CLT approximation (O(n) per candidate) | 3-8x faster than TODIS-MAX   |

## Authors

- **Ma Quoc Cuong** (522I0001)
- **Nguyen Cao Phi** (521V0006)

Advised by **Prof. Nguyen Chi Thien**, Ton Duc Thang University
