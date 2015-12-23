package com.feed.zia;

/**
 * Created by mveerapp on 12/22/2015.
 */
public interface PulseServiceIfc {
    void start();

    void signalStartDone();

    boolean atExit();

    void stop();

    void signalToEnd();
}
