import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RouteTests {
    static int passed = 0;

    static void checkRoute(String name, Map<String, Object> input, int target,
                           List<Integer> expectedPath) {
        Map<String, Object> result = Algorithms.dag(input);
        if (!Boolean.TRUE.equals(result.get("acyclic"))) {
            throw new AssertionError("Expected a DAG");
        }
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        int[] parents = (int[]) result.get("parent_edge");
        Long[] distances = (Long[]) result.get("distance");
        List<Integer> path = Algorithms.reconstructPath(source, target, edges, parents);
        if (!path.equals(expectedPath)) {
            throw new AssertionError(name + ": unexpected path " + path);
        }
        List<Integer> ids = new ArrayList<>();
        long cost = 0;
        for (int i = 1; i < path.size(); i++) {
            int id = parents[path.get(i)];
            if (edges[id][0] != path.get(i - 1) || edges[id][1] != path.get(i)) {
                throw new AssertionError(name + ": wrong edge direction");
            }
            ids.add(id);
            cost += edges[id][2];
        }
        if (path.isEmpty()) {
            if (distances[target] != null) {
                throw new AssertionError(name + ": reachable target has no route");
            }
        } else if (cost != distances[target]) {
            throw new AssertionError(name + ": wrong route weight");
        }
        System.out.println(name + ": reconstructPath(" + source + ", " + target
                + ", edges, parentEdge) = " + path + "; edge_ids=" + ids
                + "; cost=" + (path.isEmpty() ? "null" : cost));
        passed++;
    }

    public static void main(String[] args) {
        Map<String, Object> input = Json.parseRequest(
                "{\"task\":\"dag\",\"n\":5,\"source\":0,\"edges\":[[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]}");
        checkRoute("reachable", input, 3, Arrays.asList(0, 1, 2, 3));
        checkRoute("source", input, 0, Arrays.asList(0));
        checkRoute("unreachable", input, 4, new ArrayList<>());
        Map<String, Object> tied = Json.parseRequest(
                "{\"task\":\"dag\",\"n\":4,\"source\":0,\"edges\":[[0,1,2],[0,1,2],[0,2,2],[1,3,1],[2,3,1]]}");
        checkRoute("tied and parallel", tied, 3, Arrays.asList(0, 2, 3));
        Map<String, Object> parallel = Json.parseRequest(
                "{\"task\":\"dag\",\"n\":3,\"source\":2,\"edges\":[[2,1,9],[2,1,-1],[1,0,4]]}");
        checkRoute("cheaper parallel edge, nonzero source", parallel, 0, Arrays.asList(2, 1, 0));
        int n = 100000;
        int[][] chain = new int[n - 1][3];
        int[] parents = new int[n];
        parents[0] = -1;
        for (int i = 1; i < n; i++) {
            chain[i - 1] = new int[]{i - 1, i, 1};
            parents[i] = i - 1;
        }
        List<Integer> path = Algorithms.reconstructPath(0, n - 1, chain, parents);
        for (int i = 0; i < n; i++) {
            if (path.get(i) != i) {
                throw new AssertionError("Deep route mismatch");
            }
        }
        if (path.size() != n) {
            throw new AssertionError("Deep route length mismatch");
        }
        passed++;
        System.out.println("deep route: 100000 vertices, endpoints 0 and 99999, PASS");
        System.out.println("PASS " + passed + " route-helper tests");
    }
}
