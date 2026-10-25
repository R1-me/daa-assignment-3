import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class Algorithms {
    static class DSU {
        int[] parent;
        int[] size;
        int components;
        int largest;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int vertex) {
            int root = vertex;
            while (parent[root] != root) {
                root = parent[root];
            }
            while (vertex != root) {
                int next = parent[vertex];
                parent[vertex] = root;
                vertex = next;
            }
            return root;
        }

        boolean union(int u, int v) {
            int a = find(u);
            int b = find(v);
            if (a == b) {
                return false;
            }
            if (size[a] < size[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            largest = Math.max(largest, size[a]);
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DSU sets = new DSU(n);
        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++) {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.components;
            states[i][1] = sets.largest;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("states", states);
        return result;
    }

    static Map<String, Object> forestResult(long cost, List<Integer> ids, int components) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", ids);
        result.put("components", components);
        return result;
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        return kruskalGraph(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> kruskalGraph(int n, int[][] edges) {
        Integer[] order = new Integer[edges.length];
        for (int i = 0; i < edges.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt((Integer id) -> edges[id][2])
                .thenComparingInt(id -> id));
        DSU sets = new DSU(n);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        for (int id : order) {
            if (sets.union(edges[id][0], edges[id][1])) {
                selected.add(id);
                cost += edges[id][2];
            }
        }
        return forestResult(cost, selected, sets.components);
    }

    static List<List<Integer>> emptyGraph(int n) {
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        return graph;
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        return primGraph(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> primGraph(int n, int[][] edges) {
        List<List<Integer>> graph = emptyGraph(n);
        for (int id = 0; id < edges.length; id++) {
            graph.get(edges[id][0]).add(id);
            graph.get(edges[id][1]).add(id);
        }
        PriorityQueue<Integer> queue = new PriorityQueue<>(
                Comparator.comparingInt((Integer id) -> edges[id][2]).thenComparingInt(id -> id));
        boolean[] visited = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            queue.addAll(graph.get(start));
            while (!queue.isEmpty()) {
                int id = queue.remove();
                int u = edges[id][0];
                int v = edges[id][1];
                if (visited[u] && visited[v]) {
                    continue;
                }
                int next = visited[u] ? v : u;
                visited[next] = true;
                selected.add(id);
                cost += edges[id][2];
                for (int nextId : graph.get(next)) {
                    int a = edges[nextId][0];
                    int b = edges[nextId][1];
                    if (!visited[a] || !visited[b]) {
                        queue.add(nextId);
                    }
                }
            }
        }
        return forestResult(cost, selected, components);
    }

    static List<Integer> finishOrder(List<List<Integer>> graph, boolean detectCycle) {
        int n = graph.size();
        int[] state = new int[n];
        int[] nextNeighbour = new int[n];
        int[] stack = new int[n];
        List<Integer> finished = new ArrayList<>();
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }
            int top = 0;
            stack[top] = start;
            state[start] = 1;
            while (top >= 0) {
                int vertex = stack[top];
                if (nextNeighbour[vertex] == graph.get(vertex).size()) {
                    state[vertex] = 2;
                    finished.add(vertex);
                    top--;
                } else {
                    int next = graph.get(vertex).get(nextNeighbour[vertex]++);
                    if (state[next] == 0) {
                        state[next] = 1;
                        stack[++top] = next;
                    } else if (detectCycle && state[next] == 1) {
                        return null;
                    }
                }
            }
        }
        return finished;
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> graph = emptyGraph(n);
        List<List<Integer>> transpose = emptyGraph(n);
        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            transpose.get(edge[1]).add(edge[0]);
        }
        List<Integer> finished = finishOrder(graph, false);
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] stack = new int[n];
        int count = 0;
        for (int i = finished.size() - 1; i >= 0; i--) {
            int start = finished.get(i);
            if (component[start] != -1) {
                continue;
            }
            int top = 0;
            stack[top] = start;
            component[start] = count;
            while (top >= 0) {
                int vertex = stack[top--];
                for (int next : transpose.get(vertex)) {
                    if (component[next] == -1) {
                        component[next] = count;
                        stack[++top] = next;
                    }
                }
            }
            count++;
        }
        HashSet<Long> seen = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();
        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            long key = (long) from * n + to;
            if (from != to && seen.add(key)) {
                condensation.add(new int[]{from, to});
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("component", component);
        result.put("condensation", condensation);
        return result;
    }

    static List<Integer> topologicalOrder(int n, int[][] edges) {
        List<List<Integer>> graph = emptyGraph(n);
        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }
        List<Integer> order = finishOrder(graph, true);
        if (order != null) {
            Collections.reverse(order);
        }
        return order;
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        List<Integer> order = topologicalOrder(Input.vertexCount(input), Input.edges(input));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", order != null);
        result.put("order", order == null ? new ArrayList<Integer>() : order);
        return result;
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        List<Integer> order = topologicalOrder(n, edges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", order != null);
        if (order == null) {
            result.put("distance", new Long[0]);
            result.put("parent_edge", new int[0]);
            return result;
        }
        List<List<Integer>> graph = emptyGraph(n);
        for (int id = 0; id < edges.length; id++) {
            graph.get(edges[id][0]).add(id);
        }
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int u : order) {
            if (distance[u] == null) {
                continue;
            }
            for (int id : graph.get(u)) {
                int v = edges[id][1];
                long candidate = distance[u] + edges[id][2];
                if (distance[v] == null || candidate < distance[v]) {
                    distance[v] = candidate;
                    parentEdge[v] = id;
                }
            }
        }
        result.put("distance", distance);
        result.put("parent_edge", parentEdge);
        return result;
    }

    static List<Integer> reconstructPath(int source, int target, int[][] edges,
                                         int[] parentEdge) {
        List<Integer> path = new ArrayList<>();
        if (target != source && parentEdge[target] == -1) {
            return path;
        }
        int vertex = target;
        while (vertex != source) {
            path.add(vertex);
            vertex = edges[parentEdge[vertex]][0];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }
}
