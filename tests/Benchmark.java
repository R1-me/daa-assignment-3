import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Benchmark {
    static class Graph {
        int n;
        int[][] edges;
        long expectedCost;
        long seed;

        Graph(int vertices, int edgeCount) {
            n = vertices;
            seed = 310000L + n;
            Random random = new Random(seed);
            edges = new int[edgeCount][3];
            for (int i = 1; i < n; i++) {
                int weight = 1 + random.nextInt(1000);
                edges[i - 1] = new int[]{i - 1, i, weight};
                expectedCost += weight;
            }
            for (int i = n - 1; i < edgeCount; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n - 1);
                if (v >= u) {
                    v++;
                }
                edges[i] = new int[]{u, v, 1000000 + random.nextInt(1000000)};
            }
            for (int i = edges.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int[] temp = edges[i];
                edges[i] = edges[j];
                edges[j] = temp;
            }
        }
    }

    static void validate(Graph graph, Map<String, Object> result) {
        long cost = ((Number) result.get("cost")).longValue();
        int components = ((Number) result.get("components")).intValue();
        List<?> ids = (List<?>) result.get("edge_ids");
        if (cost != graph.expectedCost || components != 1 || ids.size() != graph.n - 1) {
            throw new AssertionError("Wrong optimum or forest size");
        }
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < graph.n; i++) {
            adjacency.add(new ArrayList<>());
        }
        boolean[] used = new boolean[graph.edges.length];
        long sum = 0;
        for (Object value : ids) {
            int id = ((Number) value).intValue();
            if (id < 0 || id >= used.length || used[id]) {
                throw new AssertionError("Invalid or duplicate edge ID");
            }
            used[id] = true;
            int[] edge = graph.edges[id];
            adjacency.get(edge[0]).add(edge[1]);
            adjacency.get(edge[1]).add(edge[0]);
            sum += edge[2];
        }
        boolean[] seen = new boolean[graph.n];
        int[] queue = new int[graph.n];
        int front = 0;
        int back = 1;
        seen[0] = true;
        while (front < back) {
            int u = queue[front++];
            for (int v : adjacency.get(u)) {
                if (!seen[v]) {
                    seen[v] = true;
                    queue[back++] = v;
                }
            }
        }
        if (back != graph.n || sum != cost) {
            throw new AssertionError("Selected edges do not form the expected tree");
        }
    }

    static double timed(Graph graph, boolean kruskal) {
        long start = System.nanoTime();
        Map<String, Object> result = kruskal
                ? Algorithms.kruskalGraph(graph.n, graph.edges)
                : Algorithms.primGraph(graph.n, graph.edges);
        long end = System.nanoTime();
        validate(graph, result);
        return (end - start) / 1000000.0;
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        int[] sizes = {1000, 10000, 50000};
        System.out.println("n,m,seed,method,run1_ms,run2_ms,run3_ms,run4_ms,run5_ms,median_ms,cost");
        for (int n : sizes) {
            Graph graph = new Graph(n, n * 5);
            validate(graph, Algorithms.kruskalGraph(n, graph.edges));
            validate(graph, Algorithms.primGraph(n, graph.edges));
            for (int i = 0; i < 3; i++) {
                timed(graph, true);
                timed(graph, false);
            }
            double[][] times = new double[2][5];
            for (int i = 0; i < 5; i++) {
                if (i % 2 == 0) {
                    times[0][i] = timed(graph, true);
                    times[1][i] = timed(graph, false);
                } else {
                    times[1][i] = timed(graph, false);
                    times[0][i] = timed(graph, true);
                }
            }
            for (int method = 0; method < 2; method++) {
                double[] sorted = times[method].clone();
                Arrays.sort(sorted);
                System.out.printf("%d,%d,%d,%s", n, graph.edges.length, graph.seed,
                        method == 0 ? "Kruskal" : "Prim");
                for (double time : times[method]) {
                    System.out.printf(",%.6f", time);
                }
                System.out.printf(",%.6f,%d%n", sorted[2], graph.expectedCost);
            }
        }
    }
}
