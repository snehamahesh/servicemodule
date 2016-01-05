package com.feed.zia.conf;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Created by mveerapp on 12/22/2015.
 */
public class Services {

    private List<PConfig> services = new LinkedList<>();
    private final Map<PConfig, List<PConfig>> graph = new IdentityHashMap<>();
    private final Map<PConfig, List<PConfig>> hparg = new IdentityHashMap<>();

    public void addDependent(PConfig v, PConfig w) {
        dependent(graph, v, w);
        w.incrementInDegree();
        dependent(hparg, w, v);
    }

    private void dependent(Map<PConfig, List<PConfig>> map, PConfig v, PConfig w) {
        List<PConfig> vList;
        if (map.containsKey(v)) {
            vList = map.get(v);
            vList.add(w);
        } else {
            vList = new LinkedList<PConfig>();
            vList.add(w);
            map.put(v, vList);
        }
    }

    public void addEmptyDependent(PConfig v) {
        if (!graph.containsKey(v)) {
            graph.put(v, new LinkedList<PConfig>());
        }
        if (!hparg.containsKey(v)) {
            hparg.put(v, new LinkedList<PConfig>());
        }
    }

    public List<PConfig> getDependentsOf(PConfig serviceConfig) {
        if (hparg.containsKey(serviceConfig)) {
            return hparg.get(serviceConfig).stream().collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    public List<PConfig> getDependsOn(PConfig serviceConfig) {
        if (graph.containsKey(serviceConfig)) {
            return graph.get(serviceConfig).stream().collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    public List<PConfig> getServices() {
        return services;
    }

    public void setServices(List<PConfig> services) {
        this.services = services;
    }

    public boolean hasCycle() {
        return new CycleFinder().hasCycle();
    }


    public Iterable<PConfig> findCycle() {
        CycleFinder cycleFinder = new CycleFinder();
        return cycleFinder.hasCycle() ? cycleFinder.cycle() : Collections.EMPTY_LIST;
    }

    private class CycleFinder {
        private boolean[] marked;        // marked[v] = has vertex v been marked?
        private PConfig[] edgeTo;        // edgeTo[v] = previous vertex on path to v
        private boolean[] onStack;       // onStack[v] = is vertex on the stack?
        private Stack<PConfig> cycle;    // directed cycle (or null if no such cycle)

        // Determines whether the digraph has a directed cycle and, if so, finds such a cycle.
        public CycleFinder() {
            marked = new boolean[graph.size()];
            onStack = new boolean[graph.size()];
            edgeTo = new PConfig[graph.size()];
            for (PConfig pConfig : services) {
                if (!marked[pConfig.getId()]) {
                    dfs(pConfig);
                }
            }
        }

        // check that algorithm computes either the topological order or finds a directed cycle
        private void dfs(PConfig v) {
            onStack[v.getId()] = true;
            marked[v.getId()] = true;
            for (PConfig w : graph.get(v)) {
                // short circuit if directed cycle found
                if (cycle != null) {
                    return;
                } else if (!marked[w.getId()]) {  //found new vertex, so recur
                    edgeTo[w.getId()] = v;
                    dfs(w);
                } else if (onStack[w.getId()]) {
                    // trace back directed cycle
                    cycle = new Stack<PConfig>();
                    for (PConfig x = v; !x.equals(w); x = edgeTo[x.getId()]) {
                        cycle.push(x);
                    }
                    cycle.push(w);
                    cycle.push(v);
                }
            }
            onStack[v.getId()] = false;
        }

        /**
         * Does the digraph have a directed cycle?
         *
         * @return <tt>true</tt> if the digraph has a directed cycle, <tt>false</tt> otherwise
         */
        public boolean hasCycle() {
            return cycle != null;
        }

        /**
         * Returns a directed cycle if the digraph has a directed cycle, and <tt>null</tt> otherwise.
         *
         * @return a directed cycle (as an iterable) if the digraph has a directed cycle,
         * and <tt>null</tt> otherwise
         */
        public Iterable<PConfig> cycle() {
            return cycle;
        }
    }

    @SuppressWarnings("PMD")
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("-----------------Service: Dependents--------------------------\n");
        for (PConfig k : services) {
            s.append(k + ": ");
            if (graph.containsKey(k)) {
                graph.get(k).stream().forEach(w -> s.append(w + " "));
            }
            s.append("\n");
        }
        s.append("-----------------Service: DependsOn--------------------------\n");
        for (PConfig k : services) {
            s.append(k + ": ");
            if (hparg.containsKey(k)) {
                hparg.get(k).stream().forEach(w -> s.append(w + " "));
            }
            s.append("\n");
        }
        s.append("-----------------------------------------------------------\n");

        return s.toString();
    }
}
