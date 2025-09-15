package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.licensingclient.ActivationState;
import com.nalpeiron.zentitle.licensingclient.IActivation;
import com.nalpeiron.zentitle.licensingclient.IActivationFeature;
import com.nalpeiron.zentitle.sample.gui.Panel;
import com.nalpeiron.zentitle.sample.gui.TableProxy;
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

    public void showFeaturesTable(final List<IActivationFeature> features) {
        showFeaturesTable(features, null);
    }

    public void showFeaturesTable(final List<IActivationFeature> features, final String keyToHighlight) {
        final TableProxy table = new TableProxy()
                .addColumn("Feature Key")
                .addColumn("Feature Type")
                .addColumn("Active")
                .addColumn("Available")
                .addColumn("Total");

        for (final IActivationFeature feature : features) {
            table.addRow(
                    feature.getKey(),
                    feature.getType().toString(),
                    feature.getActive() == null ? "" : feature.getActive().toString(),
                    feature.getAvailable() == null ? "Unlimited" : feature.getAvailable().toString(),
                    feature.getTotal() == null ? "Unlimited" : feature.getTotal().toString()
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