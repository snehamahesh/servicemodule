package com.feed.zia.impl;

import com.feed.zia.PulseProcessorIfc;
import com.feed.zia.PulseServiceIfc;
import com.feed.zia.conf.PConfig;
import com.feed.zia.conf.Services;
import com.feed.zia.exception.ConfigCycleDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// TBD: Find the cycles in the dependencyGraph and abort as there is no easy way out yet!!!

public class PulsarProcessor implements PulseProcessorIfc {
    private static final Logger LOG = LoggerFactory.getLogger(PulsarProcessor.class);

    //    private final ExecutorService executorService;
    private final CompletionService<PulseServiceIfc> completionService;
    private final Optional<Services> config;

    protected PulsarProcessor(String configFile)
            throws IOException, ConfigCycleDetectedException {
        try (InputStream in = Files.newInputStream(Paths.get(configFile))) {
            config = Optional.ofNullable(new Yaml().loadAs(in, Services.class));
        }
        Services services = config.orElse(new Services());
        List<PConfig> configs = services.getServices();
        for (PConfig pulsar : configs) {
//                System.out.println(pulsar);
            if (pulsar.getDependencies() != null) {
                pulsar.getDependencies().forEach(name -> {
                    configs.stream().filter(s -> name.equals(s.getName()))
                            .forEach(p -> {
                                services.addDependent(pulsar, p);
                                pulsar.incrementOutDegree();
                            });
                });
            } else {
                services.addEmptyDependent(pulsar);
            }
        }
        System.out.println("Service Dependency Graph: \n" + services);
        if (services.hasCycle()) {
            System.out.println("Service Config Has Cycle! ");
            Iterable<PConfig> iterator = services.findCycle();
            for (PConfig pConfig : iterator) {
                System.out.println("PConfig: " + pConfig);
            }
            throw new ConfigCycleDetectedException("Config has cycle: " + iterator);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(configs.size());
        completionService = new ExecutorCompletionService<PulseServiceIfc>(executorService);
    }

    Services getConfig() {
        return config.orElse(new Services());
    }

    private Future<PulseServiceIfc> submit(Services services, PConfig pConfig) {
        System.out.println("LAUNCHING: " + pConfig);
        pConfig.setService(new PulseService(pConfig,
                services.getDependsOn(pConfig),
                services.getDependentsOf(pConfig)));
        return completionService.submit(new Callable<PulseServiceIfc>() {
            public PulseServiceIfc call() {
                Thread thread = Thread.currentThread();
                thread.setName("service-" + String.format("%s", pConfig.toString()));
                pConfig.getService().start();
                return pConfig.getService();
            }
        });
    }

    public void start(PConfig serviceConfig) {
        Services services = config.orElse(new Services());
        if (serviceConfig.getService() == null) {
            submit(services, serviceConfig);
        }
        List<PConfig> dependsOn = services.getDependsOn(serviceConfig);
        dependsOn.forEach(pConfig -> {
            if (pConfig.getService() == null) {
                start(pConfig);
            }
        });
    }

    public void startAll() {
        Thread thread = Thread.currentThread();
        thread.setName("PULSE_SERVER-" + String.format("%03d", thread.getId()));
        Services services = config.orElse(new Services());
        List<PConfig> configs = services.getServices();

        for (PConfig serviceConfig : configs) {
            start(serviceConfig);
        }
    }

    public void stop(PConfig serviceConfig) {
        if (serviceConfig.getService() != null && !serviceConfig.getService().atExit()) {
            System.out.println("STOPPING: " + serviceConfig);
            serviceConfig.getService().stop();
        }
        Services services = config.orElse(new Services());
        List<PConfig> dependentsOf = services.getDependentsOf(serviceConfig);
        dependentsOf.forEach(pConfig -> {
            if (pConfig.getService() != null) {
                stop(pConfig);
            }
        });
    }

    public void stopAll() {
        Services services = config.orElse(new Services());
        List<PConfig> configs = services.getServices();
        for (PConfig serviceConfig : configs) {
            stop(serviceConfig);
        }
//        shutdownAndAwaitTermination();
    }

    // TBD: shutdown the executorService properly to cleanup the services hosted!!!  Later, later, later!

    //CHECKSTYLE IGNORE MagicNumber FOR NEXT 25 LINE
//    private void shutdownAndAwaitTermination() {
//        executorService.shutdown(); // Disable new tasks from being submitted
//        try {
//            // Wait a while for existing tasks to terminate
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow(); // Cancel currently executing tasks
//                // Wait a while for tasks to respond to being cancelled
//                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                    System.err.println("Pool did not terminate");
//                }
//            }
//        } catch (InterruptedException ie) {
//            // (Re-)Cancel if current thread also interrupted
//            executorService.shutdownNow();
//            // Preserve interrupt status
//            Thread.currentThread().interrupt();
//        }
//    }
}
