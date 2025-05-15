package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.licensingclient.IActivation;
import com.nalpeiron.zentitle.licensingclient.api.model.ActivationMode;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ActivationAction {
    private final String name;
    private final Action action;
    private final List<ActivationMode> availableInModes;

    @FunctionalInterface
    public interface Action {
        void execute(final IActivation activation) throws IOException;
    }

    public ActivationAction(final String name, final Action action, final ActivationMode[] availableInModes) {
        this.name = Validate.notBlank(name, "Name must not be blank");
        this.action = Objects.requireNonNull(action, "Action must not be null");
        this.availableInModes = Arrays.asList(availableInModes);
    }

    public String getName() {
        return name;
    }

    public void executeAction(final IActivation activation) throws IOException {
        action.execute(activation);
    }

    public List<ActivationMode> getAvailableInModes() {
        return new ArrayList<>(availableInModes);
    }
}