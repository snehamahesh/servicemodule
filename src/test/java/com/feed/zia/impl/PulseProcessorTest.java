package com.feed.zia.impl;

import com.feed.zia.conf.PConfig;
import com.feed.zia.conf.Services;
import com.feed.zia.exception.ConfigCycleDetectedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.io.InterruptedIOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by mveerapp on 12/22/2015.
 */
public class PulseProcessorTest {

    PulsarProcessor bootstrap;

    @Test(expected = ConfigCycleDetectedException.class)
    public void testCyclicDependency() throws IOException {
        new PulsarProcessor("conf/cyclic-services.yml");
        assertTrue(bootstrap == null);
    }

    @Test(expected = IOException.class)
    public void testFail() throws IOException {
        new PulsarProcessor("conf/fail.yml");
        assertTrue(bootstrap == null);
    }

    @Test
    public void testStartAllStopAll() throws InterruptedException, IOException {
        bootstrap = new PulsarProcessor("conf/services.yml");
        bootstrap.startAll();
        Services config = bootstrap.getConfig();
        config.getServices().stream().forEach(c -> assertTrue(c.getService() != null));

        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        Thread.sleep(6 * 1000);
        bootstrap.stopAll();
        config.getServices().stream().forEach(c -> assertTrue(c.getService().atExit()));
    }

    @Test
    public void testStartStopSingleService() throws InterruptedException, IOException {
        bootstrap = new PulsarProcessor("conf/services.yml");
        Services config = bootstrap.getConfig();
        PConfig pConfig = config.getServices().stream().filter(c -> c.getName().equals("1")).findFirst().get();
        bootstrap.start(pConfig, new Runnable() {
            @Override
            public void run()  {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(pConfig.getService() != null);
        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        Thread.sleep(16 * 1000);
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
    }

    @Test
    public void testStartStopMidAir() throws InterruptedException, IOException {
        bootstrap = new PulsarProcessor("conf/services.yml");
        Services config = bootstrap.getConfig();
        PConfig pConfig = config.getServices().stream().filter(c -> c.getName().equals("1")).findFirst().get();
        bootstrap.start(pConfig, new Runnable() {
            @Override
            public void run()  {
                System.out.println("testStartStopMidAir:");
            }
        });
        assertTrue(pConfig.getService() != null);
        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
    }
}
