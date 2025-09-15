package com.nalpeiron.zentitle.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nalpeiron.zentitle.licensingclient.ActivationState;
import com.nalpeiron.zentitle.licensingclient.IActivationFeature;
import com.nalpeiron.zentitle.licensingclient.api.model.ActivationMode;
import com.nalpeiron.zentitle.licensingclient.api.model.FeatureType;
import com.nalpeiron.zentitle.licensingclient.persistence.models.ActivationEntitlementData;
import com.nalpeiron.zentitle.sample.gui.Panel;
import com.nalpeiron.zentitle.sample.gui.Prompt;
import org.jline.terminal.Terminal;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ActivationActions {

    private final ActivationAction activateWithCode;
    private final ActivationAction generateOfflineActivationRequest;
    private final ActivationAction activateOffline;
    private final ActivationAction showActivationInfo;
    private final ActivationAction pullActivationStateFromServer;
    private final ActivationAction pullActivationStateFromLocalStorage;
    private final ActivationAction refreshActivationLease;
    private final ActivationAction refreshOfflineActivationLease;
    private final ActivationAction checkoutFeature;
    private final ActivationAction returnFeature;
    private final ActivationAction trackBoolFeatureUsage;
    private final ActivationAction getActivationEntitlement;
    private final ActivationAction deactivate;
    private final ActivationAction deactivateOffline;
    private final Map<ActivationState, ActivationAction[]> availableActions;

    public ActivationActions(final Terminal terminal, final Prompt prompt, final DisplayHelper displayHelper, final ObjectMapper objectMapper) {

        activateWithCode = new ActivationAction(
                "Activate license with code",
                (activation) -> {
                    final String activationCode = prompt.input("Enter activation code: ");
                    final String seatName = prompt.input("Enter seat name (keep empty for no seat name): ");
                    final String seatNameValue = seatName.trim().isEmpty() ? null : seatName;
                    ActivationExtensions.activateWithCode(activation, activationCode, seatNameValue);
                    displayHelper.showActivationInfoPanel(activation);
                },
                new ActivationMode[]{null}
        );

        generateOfflineActivationRequest = new ActivationAction(
                "Generate offline activation request (for End User Portal)",
                (activation) -> {
                    displayHelper.writeWarning(
                            "Make sure that the product/entitlement that you want to use for the offline activation has the " +
                                    "[Offline Lease Period] initialized, " +
                                    "otherwise the offline seat activation will not be possible.");

                    final String activationCode = prompt.input("Enter activation code: ");
                    final String seatName = prompt.input("Enter seat name (keep empty for no seat name): ");
                    terminal.writer().println("Generating activation request token...");
                    terminal.flush();
                    final String seatNameValue = seatName.trim().isEmpty() ? null : seatName;
                    final String token = activation.generateOfflineActivationRequestToken(activationCode, seatNameValue);
                    terminal.writer().println("Activation request token (copy and use in the End User Portal): ");
                    terminal.flush();
                    displayHelper.writeSuccess(token);
                },
                new ActivationMode[]{null}
        );

        activateOffline = new ActivationAction(
                "Activate offline (with activation response from End User Portal)",
                (activation) -> {
                    final String offlineActivationResponseToken = prompt.input("Enter offline activation response token: ");
                    terminal.writer().println("Activating offline...");
                    terminal.flush();
                    activation.activateOffline(offlineActivationResponseToken);
                    displayHelper.showActivationInfoPanel(activation);
                },
                new ActivationMode[]{null}
        );

        showActivationInfo = new ActivationAction(
                "Show activation info",
                (activation) -> displayHelper.showActivationInfoPanel(activation),
                new ActivationMode[]{ActivationMode.ONLINE, ActivationMode.OFFLINE, null}
        );

        pullActivationStateFromServer = new ActivationAction(
                "Pull activation state from the server",
                (activation) -> {
                    terminal.writer().println("Pulling current activation state from the server...");
                    terminal.flush();
                    activation.pullRemoteState();
                    displayHelper.showActivationInfoPanel(activation);
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        pullActivationStateFromLocalStorage = new ActivationAction(
                "Pull activation state from the local storage",
                (activation) -> {
                    terminal.writer().println("Pulling current activation state from the local storage...");
                    terminal.flush();
                    activation.pullPersistedState();
                    displayHelper.showActivationInfoPanel(activation);
                },
                new ActivationMode[]{ActivationMode.ONLINE, ActivationMode.OFFLINE, null}
        );

        refreshActivationLease = new ActivationAction(
                "Refresh activation lease",
                (activation) -> {
                    terminal.writer().println("Refreshing current activation...");
                    terminal.flush();
                    final OffsetDateTime previousLeaseExpiry = activation.getInfo().getLeaseExpiry();
                    final boolean refreshed = activation.refreshLease().isSuccess();
                    if (!refreshed) {
                        terminal.writer().println("Activation lease period could not be refreshed, please activate again. Current lease expiry is " + activation.getInfo().getLeaseExpiry());
                    } else {
                        OffsetDateTime newLeaseExpiry = activation.getInfo().getLeaseExpiry();
                        terminal.writer().println("Activation lease successfully refreshed from [" + previousLeaseExpiry + "] to [" + newLeaseExpiry + "]");
                    }
                    terminal.flush();
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        refreshOfflineActivationLease = new ActivationAction(
                "Refresh offline activation lease (with refresh token from End User Portal)",
                (activation) -> {
                    final OffsetDateTime previousLeaseExpiry = activation.getInfo().getLeaseExpiry();
                    final String offlineRefreshToken = prompt.input("Enter offline refresh token: ");
                    terminal.writer().println("Refreshing current offline activation...");
                    terminal.flush();
                    activation.refreshLeaseOffline(offlineRefreshToken);
                    final OffsetDateTime newLeaseExpiry = activation.getInfo().getLeaseExpiry();
                    terminal.writer().println("Activation lease successfully refreshed from [" + previousLeaseExpiry + "] to [" + newLeaseExpiry + "]");
                    terminal.flush();
                },
                new ActivationMode[]{ActivationMode.OFFLINE}
        );

        checkoutFeature = new ActivationAction(
                "Checkout advanced feature",
                (activation) -> {
                    final List<IActivationFeature> featuresToCheckout = activation.getInfo().getFeatures().stream()
                            .filter(feature -> !FeatureType.BOOL.equals(feature.getType()) && (feature.getAvailable() == null || (feature.getAvailable() > 0)))
                            .collect(Collectors.toList());

                    if (featuresToCheckout.isEmpty()) {
                        displayHelper.writeError("There are no features eligible for checkout");
                        return;
                    }

                    terminal.writer().println("Following features can be checked out:");
                    terminal.flush();
                    displayHelper.showFeaturesTable(featuresToCheckout);

                    final List<String> keys = featuresToCheckout.stream()
                            .map(IActivationFeature::getKey)
                            .collect(Collectors.toList());
                    final String featureKey = prompt.select("Select feature to checkout", keys);
                    final int amountToCheckout = prompt.inputInt("Specify amount to checkout: ");
                    terminal.writer().println("Checking out " + amountToCheckout + " " + (amountToCheckout > 1 ? "features" : "feature") + " with key '" + featureKey + "'");
                    terminal.flush();
                    final IActivationFeature featureToCheckout = activation.getFeatures().tryGet(featureKey)
                            .orElseThrow(() -> new IllegalStateException("Feature with key '" + featureKey + "' not found"));
                    activation.checkoutFeature(featureToCheckout, amountToCheckout);

                    terminal.writer().println("Feature successfully checked out!");
                    terminal.writer().println();
                    terminal.flush();
                    displayHelper.showFeaturesTable(activation.getInfo().getFeatures().stream().filter(f -> f.getType() != FeatureType.BOOL).collect(Collectors.toList()), featureKey);
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        returnFeature = new ActivationAction(
                "Return element-pool feature",
                (activation) -> {
                    final List<IActivationFeature> featuresToReturn = activation.getInfo().getFeatures().stream()
                            .filter(feature -> (feature.getActive() != null) && (feature.getActive() > 0) && (FeatureType.ELEMENT_POOL.equals(feature.getType())))
                            .collect(Collectors.toList());

                    if (featuresToReturn.isEmpty()) {
                        displayHelper.writeError("There are no features eligible for return");
                        return;
                    }

                    terminal.writer().println("Following features can be returned:");
                    terminal.flush();
                    displayHelper.showFeaturesTable(featuresToReturn);

                    final List<String> featureKeys = featuresToReturn.stream()
                            .map(IActivationFeature::getKey)
                            .collect(Collectors.toList());
                    final String featureKey = prompt.select("Select feature to return", featureKeys);
                    final int amountToReturn = prompt.inputInt("Specify amount to return: ");

                    terminal.writer().println("Returning " + amountToReturn + " " + (amountToReturn > 1 ? "features" : "feature") + " with key '" + featureKey + "'");
                    terminal.flush();
                    final IActivationFeature feature = activation.getFeatures().tryGet(featureKey)
                            .orElseThrow(() -> new IllegalStateException("Feature with key '" + featureKey + "' not found"));
                    activation.returnFeature(feature, amountToReturn);

                    terminal.writer().println("Feature successfully returned!");
                    terminal.writer().println();
                    terminal.flush();
                    displayHelper.showFeaturesTable(activation.getInfo().getFeatures(), featureKey);
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        trackBoolFeatureUsage = new ActivationAction(
                "Track usage of a bool feature",
                (activation) -> {
                    List<IActivationFeature> boolFeatures = activation.getInfo().getFeatures().stream()
                            .filter(f -> f.getType() == FeatureType.BOOL)
                            .collect(Collectors.toList());

                    if (boolFeatures.isEmpty()) {
                        displayHelper.writeError("There are no bool features");
                        return;
                    }

                    terminal.writer().println("Usage can be tracked on the following features:");
                    terminal.flush();
                    displayHelper.showFeaturesTable(boolFeatures);

                    final String featureKey = prompt.select("Select feature for tracking the usage", boolFeatures.stream().map(IActivationFeature::getKey).collect(Collectors.toList()));
                    final IActivationFeature feature = activation.getFeatures().tryGet(featureKey)
                            .orElseThrow(() -> new IllegalStateException("Feature with key '" + featureKey + "' not found"));
                    activation.trackFeatureUsage(feature);
                    terminal.writer().println("Feature usage successfully tracked!");
                    terminal.flush();
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        getActivationEntitlement = new ActivationAction(
                "Get entitlement associated with the activation",
                (activation) -> {
                    terminal.writer().println("Retrieving the entitlement...");
                    terminal.flush();
                    final ActivationEntitlementData activationEntitlement = activation.getActivationEntitlement();
                    final String entitlementJson = objectMapper.writeValueAsString(activationEntitlement);
                    final Panel activationEntitlementAsPanel = new Panel(entitlementJson)
                            .header("Activation Entitlement");
                    final String activationEntitlementAsString = activationEntitlementAsPanel.toString();
                    terminal.writer().println(activationEntitlementAsString);
                    terminal.flush();
                },
                new ActivationMode[]{ActivationMode.ONLINE, ActivationMode.OFFLINE}
        );

        deactivate = new ActivationAction(
                "Deactivate license",
                (activation) -> {
                    terminal.writer().println("Deactivating the license...");
                    terminal.flush();
                    activation.deactivate();
                },
                new ActivationMode[]{ActivationMode.ONLINE}
        );

        deactivateOffline = new ActivationAction(
                "Deactivate offline license",
                (activation) -> {
                    terminal.writer().println("Deactivating the offline license...");
                    terminal.flush();
                    final String offlineDeactivationToken = activation.deactivateOffline();
                    terminal.writer().println("Offline deactivation token (copy and use in the End User Portal):");
                    terminal.flush();
                    displayHelper.writeSuccess(offlineDeactivationToken);
                },
                new ActivationMode[]{ActivationMode.OFFLINE}
        );

        availableActions = new HashMap<>();
        availableActions.put(ActivationState.ACTIVE, new ActivationAction[]{
                showActivationInfo, pullActivationStateFromServer, pullActivationStateFromLocalStorage,
                checkoutFeature, returnFeature, trackBoolFeatureUsage, refreshActivationLease,
                deactivate, deactivateOffline, getActivationEntitlement
        });
        availableActions.put(ActivationState.LEASE_EXPIRED, new ActivationAction[]{
                showActivationInfo, pullActivationStateFromServer, pullActivationStateFromLocalStorage,
                refreshActivationLease, refreshOfflineActivationLease, deactivate, deactivateOffline,
                getActivationEntitlement
        });
        availableActions.put(ActivationState.NOT_ACTIVATED, new ActivationAction[]{
                showActivationInfo, pullActivationStateFromLocalStorage,
                activateWithCode, generateOfflineActivationRequest, activateOffline
        });
        availableActions.put(ActivationState.ENTITLEMENT_NOT_ACTIVE, new ActivationAction[]{
                showActivationInfo, pullActivationStateFromServer, pullActivationStateFromLocalStorage,
                getActivationEntitlement,
                activateWithCode, generateOfflineActivationRequest, activateOffline
        });

    }

    public Map<ActivationState, ActivationAction[]> getAvailableActions() {
        return availableActions;
    }
}