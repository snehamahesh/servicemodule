package com.feed.zia.impl;

import com.feed.zia.PulseServiceIfc;
import com.feed.zia.conf.PConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mveerapp on 12/22/2015.
 */
public class PulseService implements PulseServiceIfc {
    private static final Logger LOG = LoggerFactory.getLogger(PulseService.class);

    private final PConfig serviceConfig;
    private final Map<PConfig, Boolean> lowerServices = new HashMap<>();
    private final Map<PConfig, Boolean> upperServices = new HashMap<>();
    private final CountDownLatch lowerServicesLatch;
    private final CountDownLatch upperServicesLatch;
    private final AtomicBoolean timeToExit = new AtomicBoolean(false);
    private Thread thread;

    public PulseService(PConfig service, List<PConfig> depends, List<PConfig> dependents) {
        serviceConfig = service;
        depends.forEach(pConfig -> lowerServices.put(pConfig, new Boolean(false)));
        dependents.forEach(pConfig -> upperServices.put(pConfig, new Boolean(false)));
        lowerServicesLatch = new CountDownLatch(serviceConfig.getOutDegree());
        upperServicesLatch = new CountDownLatch(serviceConfig.getInDegree());
    }

    public boolean atExit() {
        return timeToExit.get();
    }


    private boolean startService() {
        boolean done = timeToExit.get();
        while (!done) {
            try {
                if (!lowerServices.isEmpty()) {
                    LOG.debug(" WAIT lowerServices to start: " + lowerServices);
                    // wait for all to finish, if the current count is zero returns immediately.
                    lowerServicesLatch.await();
                    LOG.debug(" OK lowerServices are started: " + lowerServices);
                } else {
                    LOG.debug(" NO lowerServices wait needed: " + lowerServices);
                }
                Thread.sleep(serviceConfig.getStartTimer());
                if (!upperServices.isEmpty()) {
                    LOG.debug(" NOW signal upperServices : " + upperServices);
                    upperServices.keySet().forEach(sc -> sc.getService().start(serviceConfig, null));
                } else {
                    LOG.debug(" NO upperServices to signal: " + lowerServices);
                }
                done = true;
            } catch (InterruptedException ie) {
                if (timeToExit.get()) {
                    // NOTE: including the current-service/user has to hint the service stop via stop() interface.
                    //       dependency-services will also call the stop() interface to force the exit.
                    LOG.info("SOMEBODY WISHES TO STOP SERVICE WHILE SERVICE START!!!");
                } else {
                    LOG.error("SPURIOUS WAKEUP INTERRUPTION FOR START SERVICE!!!");
                }
                if (thread.isInterrupted() && Thread.currentThread().equals(thread)) {
                    // clear the interruption state so the cleanup can continue without!!
                    done = Thread.currentThread().interrupted();
                }
            }
        }
        return done;
    }

    private void stopServiceAndBroadcast() {
        boolean done = false;
        while (!done) {
            try {
                upperServices.keySet().forEach(uc -> uc.getService().stop(serviceConfig));
                lowerServices.keySet().forEach(lc -> lc.getService().stop(serviceConfig));
                upperServicesLatch.await();
                done = true;
            } catch (InterruptedException ie) {
                if (timeToExit.get()) {
                    // NOTE: including the current-service/user has to hint the service stop via stop() interface.
                    //       dependency-services will also call the stop() interface to force the exit.
                    LOG.info("SOMEBODY WISHES TO STOP SERVICE WHILE SHUTTING DOWN!!!");
                } else {
                    LOG.error("SPURIOUS WAKEUP INTERRUPTION WHILE SHUTTING DOWN!!!");
                }
                if (thread.isInterrupted() && Thread.currentThread().equals(thread)) {
                    // clear the interruption state so the cleanup can continue without!!
                    Thread.currentThread().interrupted();
                }
                done = false;  // lets continue shutting down properly!!
            }
        }
    }

    private void simulateWorkWithoutInterruption(Runnable runnable) {
        LOG.debug(" is up and running!!!");
        LOG.debug(" is simulating deep work!!!");
        while (!timeToExit.get() && !thread.isInterrupted()) {
            try {
                LOG.debug(" sleep simulation: " + serviceConfig.getWorkTimer());
                if (runnable == null) {
                    Thread.sleep(serviceConfig.getWorkTimer());
                } else {
                    runnable.run();
                }
            } catch (InterruptedException ie) {
                if (timeToExit.get()) {
                    // NOTE: including the current-service/user has to hint the service stop via stop() interface.
                    //       dependency-services will also call the stop() interface to force the exit.
                    LOG.info("SOMEBODY WISHES TO STOP SERVICE WHILE WORKING HARD!!!");
                } else {
                    LOG.error("SPURIOUS WAKEUP INTERRUPTION WHILE WORKING HARD!!!");
                }
                if (thread.isInterrupted() && Thread.currentThread().equals(thread)) {
                    // clear the interruption state so the cleanup can continue without!!
                    Thread.currentThread().interrupted();
                    break;
                }
            }
        }
    }

    public void start(PConfig caller, Runnable runnable) {
        if (Objects.nonNull(caller)) {
            if (caller.equals(serviceConfig) && (Objects.isNull(thread) || !thread.isAlive())) {
                // Service must be alive only as singleton!! otherwise a rouge caller can wack this to death!!
                thread = Thread.currentThread();
                if (startService()) {
                    simulateWorkWithoutInterruption(runnable);
                }
                stopServiceAndBroadcast();
                LOG.debug(" SERVICE SHUTDOWN " +
                        (timeToExit.get() && upperServicesLatch.getCount() == 0 ? "SUCCESS" : "FAILURE") + "\n");
                LOG.debug(" call for shutdown? -> timeToExit: " + timeToExit.get());

            } else if (!caller.equals(serviceConfig)) {
                // Signal the Start Done to dependent services;
                // idempotent operation: can inform the dependent without side-effect of reducing the latches.
                Thread current = Thread.currentThread();
                LOG.debug(" signaling dependent: " + serviceConfig + " start done by: " + current.getName());
                // caller must be a service that the current/this depends on
                done(caller, lowerServices, lowerServicesLatch);
            }
        }
    }

    private void done(PConfig caller, Map<PConfig, Boolean> services, CountDownLatch latch) {
        if (Objects.nonNull(caller) && services.containsKey(caller) && !services.get(caller)) {
            // caller is allowed to count-down the latch only once!!! makes it idempotent.
            // semantics of atomic guarantees the atomicity & visibility of changes to other threads;
            // but does not guarantee the critical region change guarding semantics.
            // think this carefully before discounting as overly complex!!!
            synchronized (caller) { // block the caller; if it's from different/same threads
                if (!services.get(caller)) { // re-verify that the change is still relevant
                    latch.countDown();
                    services.put(caller, true);
                }
            }

        }
    }

    public void stop(PConfig caller) {
        // a. caller can be "this/current" service! as it might initiate his own stop!!
        // b. user can initiate stop on this service; a special case of above!!
        // c. only the 'dependent:lower' services can invoke the stop as an obligation contract!!
        if (caller != null && (caller.equals(serviceConfig) || lowerServices.containsKey(caller))) {
            if (!timeToExit.get()) {
                // semantics of atomic guarantees the atomicity & visibility of changes to other threads;
                // but does not guarantee the critical region change guarding semantics.
                // think this carefully before discounting as overly complex!!!
                synchronized (this) {  // block the callers; if it's from different threads
                    if (!timeToExit.get()) { // re-verify that the change is still relevant
                        if (thread != null) { // interrupt only if the service is running!
                            thread.interrupt();  // all because you don't want to interrupt the thread many times.
                        }
                        timeToExit.set(true);
                    }
                }
            }
        } else {
            Thread current = Thread.currentThread();
            LOG.debug(" end signal dependent: " + serviceConfig + " by: " + current.getName());
            // caller must be a dependent service of the current/this service
            done(caller, upperServices, upperServicesLatch);
        }
    }

}
