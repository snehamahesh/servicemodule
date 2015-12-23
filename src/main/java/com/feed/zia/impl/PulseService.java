package com.feed.zia.impl;

import com.feed.zia.PulseServiceIfc;
import com.feed.zia.conf.PConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by mveerapp on 12/22/2015.
 */
public class PulseService implements PulseServiceIfc {
    private static final Logger LOG = LoggerFactory.getLogger(PulseService.class);

    private final PConfig serviceConfig;
    private final List<PConfig> outerServices;
    private final List<PConfig> innerServices;
    private final CountDownLatch prologCountDown;
    private final CountDownLatch epilogCountDown;

    private boolean timeToExit = false;
    private Thread thread;

    public PulseService(PConfig service, List<PConfig> iDependOn, List<PConfig> dependents) {
        serviceConfig = service;
        outerServices = iDependOn;
        innerServices = dependents;
        prologCountDown = new CountDownLatch(serviceConfig.getOutDegree());
        epilogCountDown = new CountDownLatch(serviceConfig.getInDegree());
    }

    public boolean atExit() {
        return timeToExit;
    }

    public void signalStartDone() {
        Thread current = Thread.currentThread();
        System.out.println(thread.getName() + " signaling dependent: " + serviceConfig + " by: " + current.getName());
        prologCountDown.countDown();
    }

    public void signalToEnd() {
        Thread current = Thread.currentThread();
        timeToExit = true;
        System.out.println(thread.getName() + " end signal dependent: " + serviceConfig + " by: " + current.getName());
        epilogCountDown.countDown();
    }

    private void prologService() throws InterruptedException {
        if (!outerServices.isEmpty()) {
            System.out.println(thread.getName() + " WAIT outerServices to start: " + outerServices);
            prologCountDown.await(); // wait for all to finish, if the current count is zero returns immediately.
            System.out.println(thread.getName() + " OK outerServices are started: " + outerServices);
        } else {
            System.out.println(thread.getName() + " NO outerServices wait needed: " + outerServices);
        }
        Thread.sleep(serviceConfig.getSleepTimer());
        if (!innerServices.isEmpty()) {
            System.out.println(thread.getName() + " NOW signal innerServices : " + innerServices);
            for (PConfig sc : innerServices) {
                if (sc.getService() != null) {
                    sc.getService().signalStartDone();
                }
            }
        } else {
            System.out.println(thread.getName() + " NO innerServices to signal: " + outerServices);
        }
    }

    private void signalToEnd(List<PConfig> services, String name) {
        if (!services.isEmpty()) {
            System.out.println(thread.getName() + " HINT " + name + " to exit: " + services);
            for (PConfig sc : services) {
                if (sc.getService() != null) {
                    sc.getService().signalToEnd();
                }
            }
        } else {
            System.out.println(thread.getName() + " NO " + name + " to signal: " + services);
        }
    }

    private void epilogService() throws InterruptedException {
        signalToEnd(innerServices, "innerServices");
        signalToEnd(outerServices, "outerServices");
        epilogCountDown.await();
    }

    public void start() {
        try {
            thread = Thread.currentThread();
            prologService();
            while (!timeToExit) {
                System.out.println(thread.getName() + " is up and running!!!");
                Thread.sleep(serviceConfig.getSleepTimer());
            }
            epilogService();
            System.out.println(thread.getName() + " SERVICE SHUTDOWN " + (timeToExit ? "SUCCESS" : "FAILURE") + "\n");
        } catch (InterruptedException ex) {
            System.out.println(thread.getName() + " call for shutdown? -> timeToExit: " + timeToExit);
        }
    } // return;


    public void stop() {
        timeToExit = true;
    }

}
