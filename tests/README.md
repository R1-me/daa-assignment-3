# Tests

Run the commands from the repository root.
JDK 17 and Python 3.12+ are required.
No third-party packages are needed.

```sh
python tools/run.py --setup
python tools/run.py --check
python tests/run_tests.py
```

The test script compiles the Java solution and checks small
graphs, full-limit inputs, repeated requests and route
reconstruction. Random tests use seed `20261025`.

## Files

- `run_tests.py` — runs the additional tests.
- `RouteTests.java` — tests route reconstruction.
- `Benchmark.java` — compares Kruskal and Prim.
- `generated/` — test inputs and expected results.
- `results/` — actual outputs, logs and benchmark results.

Large test files are stored in gzip format.
The test script generates the inputs and saves the results.

## Expected Results

Expected answers are calculated independently of the Java
implementation.

- **DSU:** compute reachability after each operation, then
  count the connected components and their sizes.
- **Kruskal and Prim:** enumerate small edge subsets with
  exactly `n-components` edges. Keep those that preserve
  the original connectivity and choose the lowest cost.
- **SCC:** compute reachability between all pairs of vertices.
  Two vertices belong to the same SCC if each can reach
  the other. Collect all distinct cross-component edges.
- **Topological ordering:** detect cycles using reachability.
  For an acyclic graph, sorting vertices by their number
  of ancestors gives a valid reference order.
- **DAG shortest paths:** check for cycles, then use up to
  `n-1` rounds of edge relaxation to calculate distances.

Large inputs use chains and cycles with known results.
For example, a directed chain has one SCC per vertex,
while a directed cycle through all vertices has one SCC.
Explanations for each large case are saved in
`results/summary.json`.

## Test Coverage

The tests include empty and disconnected graphs, isolated
vertices, repeated and self unions, self-loops, parallel
edges, equal weights, negative weights and totals beyond
the 32-bit range.

DAG tests also cover nonzero sources, unreachable vertices
and cycles that cannot be reached from the source.

Full-limit tests use `100000` vertices and `250000` edges
or union operations. They include deep chains, a large SCC,
cycle detection and large negative distances.

Small requests are repeated in forward, reversed and forward
order within one Java process to check state reset.

## Checking Non-Unique Answers

The supplied `tools/public_validate.py` checks forest
validity and cost, SCC membership and condensation,
topological order, shortest distances and parent paths.

Tests check that alternative valid forests, SCC labels,
topological orders and shortest-path parents are accepted.
They also check that invalid answers are rejected,
including cyclic forests, missing condensation edges,
incorrect orders and parent edges with the wrong direction.

These results are saved in `results/witness_checks.json`.

## Route Reconstruction

`RouteTests.java` calls `Algorithms.reconstructPath`.
It checks:

- A reachable target.
- The source vertex.
- An unreachable target.
- Equal-cost routes and parallel edges.
- Edge directions and total route weight.
- A route containing `100000` vertices.

These tests run automatically with `run_tests.py`.
Their output is saved in `results/route_tests.txt`.

## Benchmark

Run after the test suite:

```sh
javac --release 17 -cp .build/Java -d .build/Java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark > tests/results/benchmark.csv
```

The benchmark compares Kruskal and Prim on identical graphs
at three sizes. It checks the returned forests and costs,
performs warm-up runs, then records five timings per method
and their medians.

The generator, timing procedure and discussion are described
in [ANALYSIS.md](../ANALYSIS.md). Timings may differ between
runs and computers.