"""Own tests. Python launches Java and supplies small independent reference answers."""
import gzip
import itertools
import json
import pathlib
import random
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from public_validate import check

RESULTS = ROOT / 'tests' / 'results'
GENERATED = ROOT / 'tests' / 'generated'
JAVA = ['java', '-Xmx384m', '-cp', str(ROOT / '.build' / 'Java'), 'Main']


def closure(n, edges, undirected=False):
    reach = [[i == j for j in range(n)] for i in range(n)]
    for edge in edges:
        u, v = edge[:2]
        reach[u][v] = True
        if undirected:
            reach[v][u] = True
    for k in range(n):
        for i in range(n):
            for j in range(n):
                reach[i][j] = reach[i][j] or (reach[i][k] and reach[k][j])
    return reach


def labels_from_reach(reach):
    n = len(reach)
    return [min(j for j in range(n) if reach[i][j] and reach[j][i]) for i in range(n)]


def reference(request):
    task, n = request['task'], request.get('n', 0)
    if task == 'ping':
        return {'status': 'ready'}
    if task == 'dsu':
        states, edges = [], []
        for pair in request['operations']:
            edges.append(pair)
            labels = labels_from_reach(closure(n, edges, True))
            groups = set(labels)
            states.append([len(groups), max((labels.count(x) for x in groups), default=0)])
        return {'states': states}
    edges = request['edges']
    if task in ('kruskal', 'prim'):
        reach = closure(n, edges, True)
        groups = set(labels_from_reach(reach))
        count = n - len(groups)
        best, best_ids = None, []
        for ids in itertools.combinations(range(len(edges)), count):
            chosen = [edges[i] for i in ids]
            if closure(n, chosen, True) == reach:
                cost = sum(edge[2] for edge in chosen)
                if best is None or cost < best:
                    best, best_ids = cost, list(ids)
        return {'cost': best, 'edge_ids': best_ids, 'components': len(groups)}
    if task == 'scc':
        labels = labels_from_reach(closure(n, edges))
        pairs = sorted({(labels[u], labels[v]) for u, v in edges if labels[u] != labels[v]})
        return {'component': labels, 'condensation': [list(pair) for pair in pairs]}
    reach = closure(n, edges)
    cyclic = any(reach[v][u] for u, v, *rest in edges)
    if task == 'topo':
        if cyclic:
            return {'acyclic': False, 'order': []}
        order = sorted(range(n), key=lambda v: sum(reach[u][v] for u in range(n)))
        return {'acyclic': True, 'order': order}
    if cyclic:
        return {'acyclic': False, 'distance': [], 'parent_edge': []}
    distance, parent = [None] * n, [-1] * n
    distance[request['source']] = 0
    for iteration in range(n - 1):
        changed = False
        for edge_id, (u, v, w) in enumerate(edges):
            if distance[u] is not None and (distance[v] is None or distance[u] + w < distance[v]):
                distance[v] = distance[u] + w
                parent[v] = edge_id
                changed = True
        if not changed:
            break
    return {'acyclic': True, 'distance': distance, 'parent_edge': parent}


def execute(requests):
    data = ''.join(json.dumps(x, separators=(',', ':')) + '\n' for x in requests)
    start = time.perf_counter()
    process = subprocess.run(JAVA, input=data, text=True, encoding='utf-8',
                             capture_output=True, timeout=20, check=True)
    answers = [json.loads(line) for line in process.stdout.splitlines()]
    assert len(answers) == len(requests), 'One JSON answer per request is required'
    assert all(isinstance(x, dict) and 'error' not in x for x in answers)
    return answers, time.perf_counter() - start


def write_jsonl(path, rows):
    with path.open('w', encoding='utf-8') as stream:
        for row in rows:
            stream.write(json.dumps(row, separators=(',', ':')) + '\n')


def small_cases():
    cases = []
    def add(name, request):
        cases.append({'name': name, 'input': request, 'output': reference(request)})
    add('ping before algorithms', {'task': 'ping'})
    for n, operations in [(0, []), (1, [[0, 0], [0, 0]]), (6, []),
                          (6, [[0, 1], [2, 3], [1, 3], [0, 2], [4, 4], [4, 5]])]:
        add('DSU boundaries and repeated unions', {'task': 'dsu', 'n': n, 'operations': operations})
    forests = [(0, []), (5, []),
               (6, [[0, 0, -100], [0, 1, 4], [0, 1, -2], [1, 2, 3], [0, 2, 8], [3, 4, -5]]),
               (3, [[0, 1, 1], [1, 2, 1], [0, 2, 1], [0, 1, 1]]),
               (5, [[i, i + 1, 1000000000] for i in range(4)]),
               (5, [[i, i + 1, -1000000000] for i in range(4)])]
    for task in ['kruskal', 'prim']:
        for n, edges in forests:
            add(task + ' boundary or weighted forest', {'task': task, 'n': n, 'edges': edges})
    directed = [(0, []), (5, []), (1, [[0, 0]]),
                (6, [[0, 1], [1, 0], [1, 2], [0, 2], [0, 2], [2, 3], [3, 2], [4, 4]]),
                (5, [[0, 2], [1, 2], [2, 3], [1, 3], [1, 3]]),
                (5, [[0, 1], [2, 3], [3, 2]])]
    for task in ['scc', 'topo']:
        for n, edges in directed:
            add(task + ' directed boundary', {'task': task, 'n': n, 'edges': edges})
    dags = [(1, 0, []), (5, 3, []),
            (5, 0, [[0, 1, -1], [1, 2, -2], [0, 2, 9]]),
            (5, 0, [[0, 1, 1], [2, 3, 1], [3, 2, 1]]),
            (1, 0, [[0, 0, 0]]),
            (4, 0, [[0, 1, 2], [0, 1, 2], [0, 2, 2], [1, 3, 1], [2, 3, 1]]),
            (5, 0, [[i, i + 1, -1000000000] for i in range(4)]),
            (5, 0, [[i, i + 1, 1000000000] for i in range(4)]),
            (4, 3, [[3, 2, -4], [2, 0, 6], [3, 0, 9]])]
    for n, source, edges in dags:
        add('DAG boundary and path witness', {'task': 'dag', 'n': n, 'source': source, 'edges': edges})
    rng = random.Random(20261025)
    for i in range(60):
        n = rng.randrange(1, 7)
        ops = [[rng.randrange(n), rng.randrange(n)] for _ in range(rng.randrange(13))]
        add('random DSU ' + str(i), {'task': 'dsu', 'n': n, 'operations': ops})
        edges = [[rng.randrange(n), rng.randrange(n), rng.randrange(-9, 10)]
                 for _ in range(rng.randrange(11))]
        for task in ['kruskal', 'prim']:
            add('random forest ' + str(i), {'task': task, 'n': n, 'edges': edges})
        directed_edges = [edge[:2] for edge in edges]
        for task in ['scc', 'topo']:
            add('random directed ' + str(i), {'task': task, 'n': n, 'edges': directed_edges})
        if i % 2 == 0:
            permutation = list(range(n))
            rng.shuffle(permutation)
            edges = [[permutation[min(u, v)], permutation[max(u, v)], w]
                     for u, v, w in edges if u != v]
        add('random DAG or cyclic ' + str(i),
            {'task': 'dag', 'n': n, 'source': rng.randrange(n), 'edges': edges})
    add('ping after algorithms', {'task': 'ping'})
    return cases


def witness_checks():
    records = []
    def verify(name, request, good, bad):
        expected = reference(request)
        assert check(request, good, expected), name + ' rejected valid alternative'
        assert not check(request, bad, expected), name + ' accepted invalid witness'
        records.append({'name': name, 'input': request, 'expected': expected,
                        'alternative_valid': good, 'invalid': bad,
                        'accepted_alternative': True, 'rejected_invalid': True})
    for task in ['kruskal', 'prim']:
        request = {'task': task, 'n': 4, 'edges': [[0, 1, 1], [1, 2, 1], [0, 2, 1], [2, 3, 1]]}
        verify(task, request, {'cost': 3, 'components': 1, 'edge_ids': [0, 2, 3]},
               {'cost': 3, 'components': 1, 'edge_ids': [0, 1, 2]})
    verify('SCC labels and condensation', {'task': 'scc', 'n': 3, 'edges': [[0, 1], [1, 0], [1, 2]]},
           {'component': [10, 10, 4], 'condensation': [[10, 4]]},
           {'component': [10, 10, 4], 'condensation': []})
    verify('topological alternative', {'task': 'topo', 'n': 3, 'edges': [[0, 2], [1, 2]]},
           {'acyclic': True, 'order': [1, 0, 2]}, {'acyclic': True, 'order': [2, 1, 0]})
    verify('DAG parallel-edge parent', {'task': 'dag', 'n': 3, 'source': 0,
                                       'edges': [[0, 1, 2], [0, 1, 2], [1, 2, 3]]},
           {'acyclic': True, 'distance': [0, 2, 5], 'parent_edge': [-1, 1, 2]},
           {'acyclic': True, 'distance': [0, 2, 5], 'parent_edge': [-1, 2, 1]})
    (RESULTS / 'witness_checks.json').write_text(json.dumps(records, indent=2) + '\n')
    return len(records)


def large_cases():
    n, count = 100000, 250000
    operations = [[i - 1, i] for i in range(1, n)]
    operations += [[0, n - 1] for _ in range(count - len(operations))]
    states = [[n - i - 1, i + 2] for i in range(n - 1)]
    states += [[1, n] for _ in range(count - len(states))]
    yield 'DSU full n and q', {'task': 'dsu', 'n': n, 'operations': operations}, {'states': states}, 'Chain merges, then repeated unions: state i is [n-i-1,i+2], then [1,n].'
    del operations, states
    weighted = [[i - 1, i, -1000000000] for i in range(1, n)]
    weighted += [[0, n - 1, 1000000000] for _ in range(count - len(weighted))]
    for task in ['kruskal', 'prim']:
        yield task + ' full n and m', {'task': task, 'n': n, 'edges': weighted}, {'cost': -(n - 1) * 1000000000, 'components': 1, 'edge_ids': list(range(n - 1))}, 'All negative chain edges are necessary in the unique optimum; positive extras cannot improve it.'
    del weighted
    directed = [[i - 1, i] for i in range(1, n)]
    directed += [[0, n - 1] for _ in range(count - len(directed))]
    condensation = [[i - 1, i] for i in range(1, n)] + [[0, n - 1]]
    yield 'SCC full n and m, deep chain', {'task': 'scc', 'n': n, 'edges': directed}, {'component': list(range(n)), 'condensation': condensation}, 'All edges increase the vertex index, so each SCC is a singleton; repeated shortcut gives one extra condensation edge.'
    yield 'topo full n and m, deep chain', {'task': 'topo', 'n': n, 'edges': directed}, {'acyclic': True, 'order': list(range(n))}, 'Chain forces the unique increasing order; shortcuts do not change it.'
    del directed, condensation
    dag_edges = [[i - 1, i, -1000000000] for i in range(1, n)]
    dag_edges += [[0, n - 1, 1000000000] for _ in range(count - len(dag_edges))]
    yield 'DAG full n and m, negative 64-bit distances', {'task': 'dag', 'n': n, 'source': 0, 'edges': dag_edges}, {'acyclic': True, 'distance': [-i * 1000000000 for i in range(n)], 'parent_edge': [-1] + list(range(n - 1))}, 'Negative chain is strictly cheaper than every shortcut; distance[v]=-v*10^9.'
    del dag_edges
    cycle = [[i - 1, i] for i in range(1, n)] + [[n - 1, 0]]
    cycle += [[0, 1] for _ in range(count - len(cycle))]
    yield 'SCC full-size single component', {'task': 'scc', 'n': n, 'edges': cycle}, {'component': [0] * n, 'condensation': []}, 'Directed cycle visits all vertices, hence all vertices are mutually reachable.'
    yield 'topo full-size directed cycle', {'task': 'topo', 'n': n, 'edges': cycle}, {'acyclic': False, 'order': []}, 'Last edge closes a cycle of length n.'
    del cycle
    hidden_cycle = [[i, i + 1, 1] for i in range(1, n - 1)] + [[n - 1, 1, 1]]
    hidden_cycle += [[1, 2, 1] for _ in range(count - len(hidden_cycle))]
    yield 'DAG full-size cycle unreachable from source', {'task': 'dag', 'n': n, 'source': 0, 'edges': hidden_cycle}, {'acyclic': False, 'distance': [], 'parent_edge': []}, 'Source 0 is isolated, but vertices 1..n-1 form a directed cycle.'


def main():
    RESULTS.mkdir(exist_ok=True)
    GENERATED.mkdir(exist_ok=True)
    subprocess.run([sys.executable, 'tools/run.py', '--setup'], cwd=ROOT, check=True)
    cases = small_cases()
    requests = [case['input'] for case in cases]
    expected = [case['output'] for case in cases]
    answers, seconds = execute(requests)
    for case, answer in zip(cases, answers):
        assert check(case['input'], answer, case['output']), case['name']
    write_jsonl(GENERATED / 'small_inputs.jsonl', requests)
    write_jsonl(GENERATED / 'small_expected.jsonl', expected)
    write_jsonl(RESULTS / 'small_actual.jsonl', answers)
    (GENERATED / 'small_cases.json').write_text(json.dumps(cases, indent=2) + '\n')
    repeated, reset_seconds = execute(requests + list(reversed(requests)) + requests)
    write_jsonl(RESULTS / 'mixed_actual.jsonl', repeated)
    mixed = cases + list(reversed(cases)) + cases
    for case, answer in zip(mixed, repeated):
        assert check(case['input'], answer, case['output']), 'state reset: ' + case['name']
    witness_count = witness_checks()
    scale_records = []
    with gzip.open(GENERATED / 'scale_inputs.jsonl.gz', 'wt', encoding='utf-8') as inp, \
         gzip.open(GENERATED / 'scale_expected.jsonl.gz', 'wt', encoding='utf-8') as exp, \
         gzip.open(RESULTS / 'scale_actual.jsonl.gz', 'wt', encoding='utf-8') as actual:
        for name, request, answer, reason in large_cases():
            result, duration = execute([request])
            assert check(request, result[0], answer), name
            for stream, row in [(inp, request), (exp, answer), (actual, result[0])]:
                stream.write(json.dumps(row, separators=(',', ':')) + '\n')
            scale_records.append({'name': name, 'n': request['n'],
                                  'items': len(request.get('edges', request.get('operations', []))),
                                  'seconds': duration, 'expected_reason': reason, 'passed': True})
            print('PASS', name, flush=True)
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', '.build/Java',
                    '-d', '.build/Java', 'tests/RouteTests.java'], cwd=ROOT, check=True)
    route = subprocess.run(['java', '-Xmx384m', '-cp', '.build/Java', 'RouteTests'],
                           cwd=ROOT, text=True, capture_output=True, check=True, timeout=20)
    (RESULTS / 'route_tests.txt').write_text(route.stdout)
    print(route.stdout, end='')
    summary = {'small_cases_passed': len(cases), 'small_batch_seconds': seconds,
               'repeated_mixed_requests_passed': len(mixed), 'mixed_batch_seconds': reset_seconds,
               'alternative_and_invalid_witness_pairs_passed': witness_count,
               'scale_cases_passed': len(scale_records), 'scale_cases': scale_records,
               'route_helper_tests_passed': 6,
               'command': 'python tests/run_tests.py', 'random_seed': 20261025}
    (RESULTS / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps({key: value for key, value in summary.items() if key != 'scale_cases'}, indent=2))


if __name__ == '__main__':
    main()
