package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.sample.gui.Panel;
import com.nalpeiron.zentitle.sample.gui.TableProxy;
import com.nalpeiron.zentitle.licensingclient.ActivationFeature;
import com.nalpeiron.zentitle.licensingclient.ActivationState;
import com.nalpeiron.zentitle.licensingclient.IActivation;
import org.jline.terminal.Terminal;

import java.util.List;

public class DisplayHelper {
    private final Terminal terminal;

    public DisplayHelper(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void showActivationInfoPanel(final IActivation activation) {
        final ActivationState state = activation.getState();
        final String stateDescription;
        switch (state) {
            case ACTIVE:
                stateDescription = "Active";
                break;
            case LEASE_EXPIRED:
                stateDescription = "Lease Expired";
                break;
            case ENTITLEMENT_NOT_ACTIVE:
                stateDescription = "Entitlement Not Active";
                break;
            case NOT_ACTIVATED:
                stateDescription = "Not Activated";
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + state);
        }
        final String status = "State: " + stateDescription;
        final String storage = "Storage: " + activation.getInfo().getLocalStorageId();
        final String activationInfo = activation.getInfo().toString();
        final String info = String.join(System.lineSeparator(), status, storage, System.lineSeparator(), activationInfo);
        terminal.writer().println(new Panel(info));
        terminal.flush();
    }

    public void showFeaturesTable(final List<ActivationFeature> features) {
        showFeaturesTable(features, null);
    }

    public void showFeaturesTable(final List<ActivationFeature> features, final String keyToHighlight) {
        final TableProxy table = new TableProxy()
                .addColumn("Feature Key")
                .addColumn("Feature Type")
                .addColumn("Active")
                .addColumn("Available")
                .addColumn("Total");

        for (final ActivationFeature feature : features) {
            table.addRow(
                    feature.getKey(),
                    feature.getType().toString(),
                    !feature.getActive().isPresent() ? "" : feature.getActive().get().toString(),
                    !feature.getAvailable().isPresent() ? "Unlimited" : feature.getAvailable().get().toString(),
                    !feature.getTotal().isPresent() ? "Unlimited" : feature.getTotal().get().toString()
            );
        }

        terminal.writer().println(table);
        terminal.flush();
    }

    public void writeError(final String message) {
        terminal.writer().println(message);
        terminal.flush();
    }

    public void writeSuccess(final String message) {
        terminal.writer().println(message);
        terminal.flush();
    }

    public void writeWarning(final String message) {
        terminal.writer().println(message);
        terminal.flush();
    }
}