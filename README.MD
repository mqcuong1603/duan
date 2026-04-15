# Mining Maximal Frequent Itemsets from Uncertain Databases

Four Java algorithms for finding **Maximal Frequent Itemsets (MFIs)** in uncertain
transaction databases, implementing both major support models.

| Algorithm     | Support Model    | Method                          |
| ------------- | ---------------- | ------------------------------- |
| **UGenMax**   | Expected support | Bottom-up tidset intersection   |
| **UFPMax**    | Expected support | Top-down conditional projection |
| **TODIS-MAX** | Probabilistic    | Exact dynamic programming       |
| **APFI-MAX**  | Probabilistic    | CLT approximation (faster)      |

## Requirements

- Java 21 or newer
- No external libraries (no Maven/Gradle needed)

## Project Layout

```
duan/
├── src/
│   ├── Main.java              # CLI entry point
│   ├── DatasetConverter.java  # Adds probabilities to standard datasets
│   ├── algorithm/             # UFPMax, UGenMax, TODISMAX, APFIMAX
│   ├── model/                 # Itemset, TopKHeap, UncertainDatabase, ...
│   ├── experiment/            # Per-algorithm runners + ExperimentRunner
│   └── util/                  # PerformanceTracker
├── data/                      # Datasets (mushroom, retail, accidents, ...)
├── main.tex                   # LaTeX report
└── README.MD
```

## Build

```bash
mkdir -p bin
javac -d bin src/util/*.java src/model/*.java \
    src/algorithm/genmax/*.java src/algorithm/*.java \
    src/Main.java src/DatasetConverter.java src/experiment/*.java
```

## Input & Output Format

**Input** — one transaction per line. Items followed by probability in parentheses
(items without parentheses are treated as probability 1.0):

```
1(0.8) 3(0.6) 4(0.9)
2(0.7) 3(0.5) 5(0.9)
```

**Output** — one maximal itemset per line:

```
item1 item2 ... #SUP: <expectedSupport>   (UFPMax/UGenMax)
item1 item2 ... #PROB: <probability>      (TODISMAX/APFIMAX)
```

## Generating Uncertain Datasets

The `data/` directory already contains `mushroom_{high,medium,low}.txt`. To
generate uncertain variants for `retail` and `accidents`:

```bash
java -cp bin DatasetConverter data/retail.txt data/retail_medium.txt medium 42
java -cp bin DatasetConverter data/accidents.txt data/accidents_medium.txt medium 42
```

Distributions: `high` N(0.8, 0.1), `medium` N(0.5, 0.2), `low` N(0.3, 0.15), `uniform`.

## Running Algorithms

### CLI options

| Option       | Description                                                       |
| ------------ | ----------------------------------------------------------------- |
| `-algorithm` | `UFPMax`, `UGenMax`, `TODISMAX`, or `APFIMAX`                     |
| `-input`     | Path to uncertain transaction database                            |
| `-output`    | Path to write results                                             |
| `-minsup`    | Support threshold (double for UFPMax/UGenMax, int for TODIS/APFI) |
| `-minprob`   | Probability threshold for TODISMAX/APFIMAX (e.g. 0.5)             |
| `-topk`      | Top-K mode (UFPMax/UGenMax only)                                  |

### Examples

```bash
# Expected support (UFPMax / UGenMax)
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output out.txt -minsup 1.5
java -cp bin Main -algorithm UGenMax -input data/mushroom_medium.txt  -output out.txt -topk 10

# Probabilistic support (TODISMAX / APFIMAX)
java -cp bin Main -algorithm TODISMAX -input data/mushroom_medium.txt -output out.txt -minsup 3000 -minprob 0.5
java -cp bin Main -algorithm APFIMAX  -input data/mushroom_medium.txt -output out.txt -minsup 3000 -minprob 0.5
```

## Experiments & Benchmarks

Each experiment class prints results plus timing, memory, and pruning stats.

```bash
# Individual experiments
java -cp bin experiment.ExpUFPMax   data/mushroom_medium.txt 2000
java -cp bin experiment.ExpUGenMax  data/mushroom_medium.txt 2000
java -cp bin experiment.ExpTODISMAX data/mushroom_medium.txt 3000 0.5
java -cp bin experiment.ExpAPFIMAX  data/mushroom_medium.txt 3000 0.5

# Full benchmark suite (writes CSV files to results/)
java -Xmx4g -cp bin experiment.ExperimentRunner results
```

The full suite produces:

- `exp1_expected_support.csv` — UFPMax vs UGenMax across minsup
- `exp2_probabilistic.csv` — TODISMAX vs APFIMAX across minsup/minprob
- `exp3_uncertainty.csv` — high vs medium vs low uncertainty
- `exp4_scalability.csv` — mushroom vs retail vs accidents

## Quick Sanity Check

Verify the build on the small bundled dataset:

```bash
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output /tmp/a.txt -minsup 1.0
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output /tmp/b.txt -minsup 1.0
diff /tmp/a.txt /tmp/b.txt   # should be identical
```

## Authors

- **Ma Quoc Cuong** (522I0001)
- **Nguyen Cao Phi** (521V0006)

Advised by **Prof. Nguyen Chi Thien**, Ton Duc Thang University.
