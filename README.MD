# Mining Maximal Frequent Itemsets from Uncertain Databases

Two Java algorithms for finding **Maximal Frequent Itemsets (MFIs)** in uncertain
transaction databases under the expected support model.

| Algorithm   | Support Model    | Method                          |
| ----------- | ---------------- | ------------------------------- |
| **UGenMax** | Expected support | Bottom-up tidset intersection   |
| **UFPMax**  | Expected support | Top-down conditional projection |

Both algorithms support a static `minsup` mode and a Top-K mode.

## Requirements

- Java 25
- No external libraries (no Maven/Gradle needed)

## Project Layout

```
duan/
├── src/
│   ├── Main.java              # CLI entry point
│   ├── DatasetConverter.java  # Adds probabilities to standard datasets
│   ├── algorithm/             # UFPMax, UGenMax
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
item1 item2 ... #SUP: <expectedSupport>
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

| Option       | Description                                               |
| ------------ | --------------------------------------------------------- |
| `-algorithm` | `UFPMax` or `UGenMax`                                     |
| `-input`     | Path to uncertain transaction database                    |
| `-output`    | Path to write results                                     |
| `-minsup`    | Minimum expected support threshold (double)               |
| `-topk`      | Top-K mode: keep K itemsets with highest expected support |

### Examples

Run one algorithm on one dataset and write the maximal itemsets to a file.
This is the plain "use the tool" mode — minimal console output, no statistics.

```bash
java -cp bin Main -algorithm UFPMax  -input data/retail_medium.txt -output out.txt -minsup 3000
java -cp bin Main -algorithm UGenMax -input data/retail_medium.txt -output out.txt -topk 10
```

## Experiments

An **experiment** runs a single algorithm once and, in addition to the result
itemsets, reports performance measurements collected by `PerformanceTracker`:
execution time, memory usage, and pruning counts. Use this when you want to
measure one algorithm on one dataset.

```bash
java -cp bin experiment.ExpUFPMax   data/retail_medium.txt 3000
java -cp bin experiment.ExpUGenMax  data/retail_medium.txt 3000
```

## Full Benchmark Suite

The **benchmark suite** (`ExperimentRunner`) is the automated harness used to
produce the numbers in the report. It runs *both algorithms* across
*multiple datasets* (mushroom, retail, accidents) at *multiple parameter
values*, with 1 warm-up run and 3 measured runs averaged, and writes four CSV
files ready for plotting:

```bash
java -Xmx4g -cp bin experiment.ExperimentRunner results
```

Outputs in `results/`:

- `exp1_expected_support.csv` — UFPMax vs UGenMax across minsup
- `exp3_uncertainty.csv` — high vs medium vs low uncertainty
- `exp4_scalability.csv` — mushroom vs retail vs accidents
- `exp_topk.csv` — UFPMax vs UGenMax across K values

**In short:** *examples* run the tool; *experiments* measure one algorithm;
the *benchmark suite* runs everything and emits CSVs.

## Quick Sanity Check

A sanity check is a short smoke test that verifies the build works before
running anything heavy. UFPMax and UGenMax are two different algorithms that
must produce the *same set* of maximal itemsets, so running both on the tiny
bundled dataset and comparing the outputs quickly confirms correctness.

Note that the two algorithms emit itemsets in different orders (UFPMax is
top-down, UGenMax is bottom-up), so the comparison must be **order-insensitive**
— sort both files before diffing.


```bash
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output out_a.txt -minsup 1.0
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output out_b.txt -minsup 1.0  # no output = pass
```

## Authors

- **Ma Quoc Cuong** (522I0001)
- **Nguyen Cao Phi** (521V0006)

Advised by **Prof. Nguyen Chi Thien**, Ton Duc Thang University.
