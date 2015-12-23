package com.feed.zia.impl;

import com.feed.zia.conf.PConfig;
import com.feed.zia.conf.Services;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static org.junit.Assert.assertTrue;

/**
 * Created by mveerapp on 12/22/2015.
 */
public class PulseProcessorTest {

    PulsarProcessor bootstrap;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setup() throws Exception {
        bootstrap = new PulsarProcessor("conf/services.yml");
    }

    @Test(expected = IOException.class)
    public void testFail() throws IOException{
        new PulsarProcessor("conf/fail.yml");
        assertTrue(bootstrap == null);
    }

    @Test
    public void testStartAllStopAll() throws InterruptedException {
        bootstrap.startAll();
        Services config = bootstrap.getConfig();
        config.getServices().stream().forEach(c -> assertTrue(c.getService() != null));

        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        Thread.sleep(6 * 1000);
        bootstrap.stopAll();
        config.getServices().stream().forEach(c -> assertTrue(c.getService().atExit()));
    }


    @Test
    public void testStartStopSingleService() throws InterruptedException {
        Services config = bootstrap.getConfig();
        PConfig pConfig = config.getServices().stream().filter(c -> c.getName().equals("1")).findFirst().get();
        bootstrap.start(pConfig);
        assertTrue(pConfig.getService() != null);
        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        Thread.sleep(16 * 1000);
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
    }

    @Test
    public void testStartStopMidAir() throws InterruptedException {
        Services config = bootstrap.getConfig();
        PConfig pConfig = config.getServices().stream().filter(c -> c.getName().equals("1")).findFirst().get();
        bootstrap.start(pConfig);
        assertTrue(pConfig.getService() != null);
        //CHECKSTYLE IGNORE MagicNumber FOR NEXT LINE
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
        bootstrap.stop(pConfig);
        assertTrue(pConfig.getService().atExit());
    }

}
