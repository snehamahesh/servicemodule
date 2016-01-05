package com.feed.zia.conf;

import com.feed.zia.PulseServiceIfc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class PConfig {

    private Integer id;
    private String name;
    private List<String> dependencies;
    private String description;
    private boolean sysService;
    private int startTimer;
    private int workTimer;

    private PulseServiceIfc service;
    private final AtomicInteger inDegree = new AtomicInteger(0);
    private final AtomicInteger outDegree = new AtomicInteger(0);

    public int getWorkTimer() {
        return workTimer;
    }

    public void setWorkTimer(int workTimer) {
        this.workTimer = workTimer;
    }

    public int getStartTimer() {
        return startTimer;
    }

    public void setStartTimer(int startTimer) {
        this.startTimer = startTimer;
    }

    public void incrementInDegree() {
        inDegree.incrementAndGet();
    }

    public int getInDegree() {
        return inDegree.get();
    }

    public void incrementOutDegree() {
        outDegree.incrementAndGet();
    }

    public int getOutDegree() {
        return outDegree.get();
    }

    public PulseServiceIfc getService() {
        return service;
    }

    public void setService(PulseServiceIfc service) {
        this.service = service;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSysService() {
        return sysService;
    }

    public void setSysService(boolean sysService) {
        this.sysService = sysService;
    }

    public String toString() {

        return "{" + inDegree.get() + ":" + name + ":" + outDegree.get() + "}";
    }
}
