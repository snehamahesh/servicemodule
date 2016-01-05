package com.feed.zia;

import com.feed.zia.conf.PConfig;

/**
 * Created by mveerapp on 12/22/2015.
 */
public interface PulseServiceIfc {

    void start(PConfig pConfig, Runnable runnable);

    boolean atExit();

    void stop(PConfig pConfig);

}
