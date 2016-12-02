package com.example.sanjeevg.memorygame;

import java.io.Serializable;

/**
 * Created by sanjeevg on 30/11/16.
 */
public class InitialTimer implements Serializable {
    private long elapsed;
    private long start;

    public void start() {
        elapsed = 0;
        start = System.currentTimeMillis();
    }

    public void pause() {
        elapsed += System.currentTimeMillis() - start;
    }

    public void resume() {
        start = System.currentTimeMillis();
    }

    public long elapsed() {
        return  System.currentTimeMillis() - start + elapsed;
    }
}
