package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.licensingclient.ActivationState;
import com.nalpeiron.zentitle.licensingclient.IActivation;

import java.util.concurrent.locks.ReentrantLock;

public class LockingActivationActionHandler {
    private static final ReentrantLock lock = new ReentrantLock();

    public static boolean lockPullStateAndExecute(Callback callback, IActivation activation) {
        final ActivationState oldState = activation.getState();
        boolean result = true;

        lock.lock();
        try {
            activation.pullPersistedState();
            if (oldState != activation.getState()) {
                result = false;
                return result;
            }

            callback.execute(activation);
        } finally {
            lock.unlock();
        }

        return result;
    }

    @FunctionalInterface
    public interface Callback {
        void execute(IActivation activation);
    }
}