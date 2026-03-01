# Mining Top-K Frequent Maximal Itemsets in Uncertain Transaction Databases

> A comparative study of **UFPMax** and **UGenMax** algorithms for discovering
> frequent maximal itemsets from uncertain transaction databases, with
> both static minimum support and dynamic Top-K threshold modes.

---

##  Project Structure

```
project/
├── src/
│   ├── Main.java                       # Command-line entry point
│   ├── model/
│   │   ├── UncertainTransaction.java   # Single transaction with probabilities
│   │   ├── UncertainDatabase.java      # Database loader and manager
│   │   ├── Itemset.java                # Itemset with expected support
│   │   └── TopKHeap.java               # Min-heap for Top-K tracking
│   ├── algorithm/
│   │   ├── UFPMax.java                 # FPMax adapted for uncertain data
│   │   └── UGenMax.java                # GenMax adapted for uncertain data
│   └── util/
│       └── PerformanceTracker.java     # Time & memory measurement
├── data/
│   └── contextUncertain.txt            # Sample uncertain database
├── bin/                                # Compiled .class files (created by build)
└── README.md                           # This file
```

---

##  Requirements

| Requirement          | Details                                      |
|----------------------|----------------------------------------------|
| **Java**             | SE 21+ (or SE 25 as specified by the course) |
| **External libs**    | None required                                |
| **Build tools**      | None required (no Maven / Gradle)            |

---

##  Build

```bash
# Compile all source files
mkdir -p bin
javac -d bin src/util/*.java src/model/*.java src/algorithm/*.java src/Main.java
```

---

##  Run

### Using static minimum expected support

```bash
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output output.txt -minsup 1.5
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output output.txt -minsup 1.5
```

### Using Top-K (dynamic threshold)

```bash
java -cp bin Main -algorithm UFPMax  -input data/contextUncertain.txt -output output.txt -topk 5
java -cp bin Main -algorithm UGenMax -input data/contextUncertain.txt -output output.txt -topk 5
```

---

##  Input File Format

Each line represents one uncertain transaction.
Items are separated by spaces, and each item is followed by its existential probability in parentheses.
Lines starting with `#` or `@` are ignored (comments / metadata).

**Uncertain transactions:**

```
# Example uncertain transaction database
1(0.8)  3(0.6)  4(0.9)
2(0.7)  3(0.5)  5(0.9)
1(0.6)  2(0.8)  3(0.7)  5(0.85)
2(0.5)  5(0.6)
1(0.9)  2(0.75) 3(0.8)  5(0.7)
```

**Standard (non-uncertain) databases** are also supported — items without
probabilities are treated as having probability **1.0**:

```
1 3 4
2 3 5
1 2 3 5
```

---

##  Output File Format

Each line represents one maximal frequent itemset in **SPMF-compatible** format:

```
item1 item2 ... itemN #SUP: expectedSupport
```

**Example:**

```
1 2 3 5 #SUP: 1.6200
```

---

##  Algorithms

### UFPMax — FPMax for Uncertain Data

Based on **FPMax** (Grahne & Zhu, 2003).
Uses an FP-tree structure adapted for uncertain data.
Branch-and-bound pruning via anti-monotonicity of expected support
and superset checking with MFI-tree.

### UGenMax — GenMax for Uncertain Data

Based on **GenMax** (Gouda & Zaki, 2005).
Uses weighted tidsets and diffsets for vertical mining.
Branch-and-bound pruning via progressive focusing
and anti-monotonicity of expected support.

---

##  Authors

**[Your Name]** — *[Your Student ID]*

---