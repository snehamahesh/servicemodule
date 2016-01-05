package com.feed.zia;

import com.feed.zia.conf.PConfig;

/**
 * Created by mveerapp on 12/22/2015.
 */
public interface PulseProcessorIfc {

    void start(PConfig service, Runnable runnable);

    void startAll();

    void stop(PConfig service);

    void stopAll();
}
