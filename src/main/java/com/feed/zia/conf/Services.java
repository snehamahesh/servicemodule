package com.feed.zia.conf;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
