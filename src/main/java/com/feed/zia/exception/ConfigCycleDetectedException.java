package com.feed.zia.exception;

/**
 * Created by mveerapp on 12/23/2015.
 */
public class ConfigCycleDetectedException extends RuntimeException {

    public ConfigCycleDetectedException(String string) {
        super(string);
    }

}
