# Assignment 3: Analysis

**Student:** Amir Kunanbayev, SE-2514

Here, `n` is the number of vertices, `m` is the number of edges,
and `q` is the number of union operations. Memory estimates include
the returned results.

## 1. DSU

**Algorithm.** Each vertex initially forms a separate component.
The implementation uses path compression and union by size.
When two different components are joined, the smaller tree is
attached to the larger one. Their sizes are added, the component
count decreases, and the largest component size is updated.

**Invariant and correctness.** Two vertices have the same root
exactly when they belong to the same component. The size stored
at each root equals its component size. Path compression preserves
the root, while union merges exactly two components. Therefore,
the recorded component count and largest size are correct after
every operation.

**Boundary cases.** Self-unions and repeated unions do not
change the components, but still produce an output entry.
No initial state is included in the output. For `n=0`
and no operations, the result is empty.

**Complexity.** Time is `O(n + q·α(n))` amortized, where `α(n)` is
the inverse Ackermann function. Memory is `O(n+q)`.

## 2. Kruskal

**Algorithm.** The algorithm sorts original edge IDs by weight.
It processes the edges in that order and selects an edge only
when its endpoints belong to different DSU components.

**Invariant and correctness.** The selected edges form a forest
that can be extended to a minimum spanning forest. Each accepted
edge is a minimum-weight edge across a cut between current
components. By the cut property, it can be included in an optimal
forest. Edges joining vertices already connected are rejected
because they create a cycle.

After processing all edges, each original connected component
is spanned. The algorithm returns the total cost, original edge
IDs and component count. A forest with `c` components contains
`n-c` edges.

**Boundary cases.** Self-loops are skipped because their
endpoints are already in the same component. For `n=0`,
the cost and component count are zero.

**Complexity.** Time is `O(n + m·log(m+1))`.
Memory is `O(n+m)`.

## 3. Prim

**Algorithm.** Prim uses undirected adjacency lists and a priority
queue ordered by edge weight. It starts from an unvisited vertex
and repeatedly selects the cheapest edge leading to an unvisited
vertex. After finishing one component, it restarts from another
unvisited vertex.

**Invariant.** The selected edges form a tree in the current
component, and every edge crossing from visited to unvisited
vertices is represented in the queue. Edges whose endpoints are
both visited are discarded.

**Correctness.** Each accepted edge adds one new vertex and
cannot create a cycle. The cheapest crossing edge is safe by
the cut property. Restarting the algorithm processes every
component, producing a minimum spanning forest.

**Boundary cases.** Isolated vertices are counted as individual
components. Stale parallel edges are simply discarded when both
endpoints are visited.

**Complexity.** With the lazy priority queue, time is
`O(n + m·log(m+1))`. Memory is `O(n+m)`.

## 4. Kosaraju SCC

**Algorithm.** The algorithm builds the directed graph and its
transpose. The first DFS records vertices in finishing order.
The second DFS processes the transpose in decreasing finishing
order and assigns one label to each discovered component.

**Invariant.** In the first pass, a vertex finishes only after
its outgoing edges have been examined. During the second pass,
each traversal visits exactly one previously unassigned SCC.

**Correctness.** For an edge between different SCCs, the source
component has a larger maximum finishing time. Consequently,
the next unassigned component cannot reach another unassigned
component in the transpose. Its vertices remain mutually
reachable, so the second DFS identifies exactly that SCC.

The algorithm then scans all original edges to build the
condensation. A `HashSet<Long>` removes repeated component
pairs. Internal edges are excluded.

**Boundary cases.** Isolated vertices form separate SCCs. An empty
graph produces two empty arrays.

**Complexity.** Time is expected `O(n+m)` with hash-based
deduplication. Memory is `O(n+m)`.

## 5. DFS Topological Ordering

**Algorithm.** DFS uses three states: unvisited, active and
finished. An edge to an active vertex indicates a directed
cycle. If no cycle is found, the algorithm reverses the
finishing order.

**Invariant.** Active vertices form the current DFS path,
while finished vertices have all outgoing edges examined.
An edge to an active vertex closes a cycle. An edge to a
finished vertex does not indicate a cycle by itself.

**Correctness.** In an acyclic graph, for every edge `u → v`,
vertex `v` finishes before `u`. Reversing the finishing order
therefore places `u` before `v`.

**Boundary cases.** Every component is checked. A self-loop
or a cycle in any component produces `acyclic=false` and an
empty order. For `n=0`, the result is `acyclic=true` with an
empty order.

**Complexity.** Time and memory are `O(n+m)`.

## 6. DAG Shortest Paths

**Algorithm.** The algorithm first checks the whole graph
for cycles. If any cycle exists, including one unreachable
from the source, it returns `acyclic=false`, `distance=[]`
and `parent_edge=[]`.

Otherwise, the source distance is initialized to zero.
Other distances are `null`, and all parent IDs are initially
`-1`. Reachable vertices are processed in topological order,
relaxing their outgoing edges. A shorter route updates the
distance and stores the original incoming edge ID.

**Invariant.** A vertex has its final shortest distance when
it is processed.

**Correctness.** Every predecessor appears before the current
vertex in topological order. Its distance is therefore final
before its outgoing edges are relaxed. Taking the minimum
over these incoming routes gives the shortest distance.

**Boundary cases.** Unreachable vertices remain `null` with parent
`-1`. The source keeps distance zero and parent `-1`. Tied shortest
paths may produce different valid parent arrays.

**Complexity.** Time and memory are `O(n+m)`.

## 7. State Reset and Deep Graphs

All graph data, DSU objects, visited arrays and priority
queues are created separately for each request. No mutable
algorithm state is shared between requests. The supplied
`ping` response is preserved.

DFS uses an explicit array stack. The `nextNeighbour` array
stores the next adjacency position for each vertex, so
neighbours are not repeatedly scanned from the beginning.
This supports chains of **100000 vertices** without recursive
call-stack overflow.

Costs and distances use `long` to avoid 32-bit overflow.
Original edge IDs are preserved during sorting and
adjacency construction.

## 8. Route Reconstruction

**Algorithm.** The helper
`reconstructPath(source, target, edges, parentEdge)`
follows the selected incoming edges backward from the
target to the source, then reverses the vertex list.

**Invariant and correctness.** At each step, the collected
vertices form a reversed suffix of the selected shortest
path. Parent edges move backward in topological order,
so they eventually reach the source. Reversing the list
gives the correct forward route.

The route to the source is `[source]`. An unreachable
target returns `[]`. The helper accepts parent arrays
from a valid acyclic result for the same graph and source.

**Complexity.** For a route containing `L` vertices,
time and output space are `O(L)`.

### Executed Examples

The following calls were executed in `tests/RouteTests.java`.

For five vertices with source `0`, the edges were:
`[[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]`.

- **Reachable target:**
  `Algorithms.reconstructPath(0, 3, edges, parentEdge)`
  returned `[0,1,2,3]`. Selected edge IDs were `[0,2,3]`,
  with total weight `3-7+4=0`.

- **Source:**
  `Algorithms.reconstructPath(0, 0, edges, parentEdge)`
  returned `[0]`, with no edges and cost `0`.

- **Unreachable target:**
  `Algorithms.reconstructPath(0, 4, edges, parentEdge)`
  returned `[]`. Vertex `4` was unreachable.

For tied routes and parallel edges, the input edges were:
`[[0,1,2],[0,1,2],[0,2,2],[1,3,1],[2,3,1]]`.

- `Algorithms.reconstructPath(0, 3, edges, parentEdge)`
  returned `[0,2,3]`, using edge IDs `[2,4]` with cost `3`.

For a cheaper parallel edge, the input edges were:
`[[2,1,9],[2,1,-1],[1,0,4]]`.

- `Algorithms.reconstructPath(2, 0, edges, parentEdge)`
  returned `[2,1,0]`, using edge IDs `[1,2]` with cost `3`.

The tests checked each selected edge’s direction and verified
that the route weight matched the reported distance.
A separate test successfully reconstructed a chain containing
**100000 vertices**.

## 9. Testing

### Results

The completed checks passed:

- **36** public cases and the repeated public batch.
- **399** own small cases.
- **1197** mixed requests in one process.
- **9** full-limit cases.
- **5** valid-alternative and invalid-witness pairs.
- **6** route-helper tests.

### Coverage

Tests cover empty and disconnected graphs, isolates,
self-unions, repeated unions, loops, parallel edges, tied
answers, negative weights, 64-bit totals, unreachable vertices
and cycles outside source reachability.

All nine full-limit cases use **100000 vertices** and
**250000 edges or operations**. They include deep chains,
a single large SCC, cycle detection and negative distances
outside the 32-bit range.

### Expected Answers

Small random inputs use seed `20261025`. Expected answers
are computed independently of the Java solution:

- **DSU:** reachability and component sizes.
- **Minimum forests:** enumeration of small edge subsets.
- **SCC:** mutual reachability.
- **Topological ordering:** reachability and edge precedence.
- **DAG distances:** repeated edge relaxation.

Large cases use constructions with known answers.
A directed chain has singleton SCCs and a fixed topological
order. A directed cycle through all vertices has one SCC.
A chain with edge weights `-10^9` has distance `-v·10^9`
from vertex `0` to vertex `v`.

### Non-Unique Answers

Non-unique answers are checked semantically using the supplied
validator. Tests accept alternative optimal forests, different
SCC labels, another valid topological order and another
equal-weight parent edge.

They reject cyclic forests, missing condensation edges,
invalid orders and incorrectly directed parent edges.

Inputs and expected answers are stored in `tests/generated/`.
Actual outputs and verification results are stored in
`tests/results/`.

## 10. Kruskal–Prim Benchmark

### Graphs

Both algorithms were tested on the same graphs at three sizes:

- `n=1000`, `m=5000`, seed `311000`.
- `n=10000`, `m=50000`, seed `320000`.
- `n=50000`, `m=250000`, seed `360000`.

The generator in `tests/Benchmark.java` uses Java `Random`.
It creates a spanning chain with weights from `1` to `1000`,
adds random edges with weights from `1000000` to `1999999`,
and shuffles the edge array.

Every chain edge is the unique lightest edge across the cut
created by removing it. Therefore, the chain is the unique
minimum spanning tree. Its known cost provides an independent
expected answer.

Before timing, both algorithms were checked against this
cost. Validation also checked original edge IDs, their
uniqueness, the total weight, connectivity and the required
`n-1` selected edges.

### Procedure

Elapsed time was measured using `System.nanoTime()`
and converted to milliseconds.

Each algorithm first completed one validation run and three
warm-up runs at each size. Five measured runs followed.
The execution order alternated between Kruskal first and
Prim first.

**Included in timing:** initialization, internal data structure
construction, sorting or heap operations, and result
construction.

**Excluded from timing:** compilation, JVM startup, input
generation, JSON processing, output and validation.

The same timing boundary was used for both methods.
The median is the middle value after sorting the five
measurements.

### Results

All times are in **milliseconds**, rounded to three decimal
places.

#### Graph 1: 1000 vertices, 5000 edges

- **Kruskal:** `1.755, 1.645, 2.269, 2.360, 1.612`.
- **Kruskal median:** `1.755`.
- **Prim:** `3.270, 2.487, 2.826, 2.648, 2.066`.
- **Prim median:** `2.648`.
- **Cost returned by both:** `502922`.

#### Graph 2: 10000 vertices, 50000 edges

- **Kruskal:** `18.068, 15.889, 16.016, 15.384, 15.631`.
- **Kruskal median:** `15.889`.
- **Prim:** `37.869, 32.324, 33.208, 31.180, 31.896`.
- **Prim median:** `32.324`.
- **Cost returned by both:** `4991529`.

#### Graph 3: 50000 vertices, 250000 edges

- **Kruskal:** `110.029, 117.057, 107.039, 107.433, 134.061`.
- **Kruskal median:** `110.029`.
- **Prim:** `297.310, 293.479, 282.595, 275.418, 283.194`.
- **Prim median:** `283.194`.
- **Cost returned by both:** `25021557`.

The original measurements are saved in
`tests/results/benchmark.csv`.

### Discussion

Kruskal had a lower median at all three tested sizes.
These results apply to the generated graphs, which have
`m=5n` and a light spanning chain. Other graph types may
produce different results.

Both implementations have time complexity
`O(n + m·log(m+1))`. Actual running times can vary because
of JVM warm-up, garbage collection and system load.

## 11. Build, Run and Test Commands

Run from the project root with **JDK 17** and **Python 3.12+**.

### Build and Test

```sh
python tools/run.py --setup
python tools/run.py --check
python tests/run_tests.py
```

### Run

Run the JSON Lines program after compilation:

```sh
java -Xmx384m -cp .build/Java Main
```

### Benchmark

```sh
javac --release 17 -cp .build/Java -d .build/Java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark > tests/results/benchmark.csv
```

Benchmark compilation is completed before any measurements
begin.

## 12. Sources and Assistance

The [official assignment repository][assignment] provided
the specification, Java input/output files, public tests,
launcher and semantic validator.

ChatGPT was used as a supplementary tool to help debug Java syntax,
format the Markdown report, and generate boilerplate code for the
test suite

[assignment]: https://github.com/RZhaniya/daa-assignment-3-graphs