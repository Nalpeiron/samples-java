package com.nalpeiron.zentitle;

import com.nalpeiron.zentitle.licensingclient.*;
import com.nalpeiron.zentitle.licensingclient.licensingapi.HttpClientFactory;
import com.nalpeiron.zentitle.licensingclient.licensingapi.LicensingApiOptions;
import com.nalpeiron.zentitle.licensingclient.options.ActivationOptions;
import com.nalpeiron.zentitle.licensingclient.options.OnlineActivationOptions;
import com.nalpeiron.zentitle.licensingclient.persistence.IPersistence;
import com.nalpeiron.zentitle.licensingclient.persistence.Persistence;
import com.nalpeiron.zentitle.licensingclient.persistence.models.ActivationEntitlementData;
import com.nalpeiron.zentitle.licensingclient.persistence.storage.InMemoryActivationStorage;
import com.nalpeiron.zentitle.licensingclient.services.DateTimeProvider;
import com.nalpeiron.zentitle.licensingclient.zentitle2core.Zentitle2Core;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public class Main {
    public static void main(final String[] args) throws IOException {

        // create storage
        final InMemoryActivationStorage sharedProcessStorage = new InMemoryActivationStorage();
        sharedProcessStorage.clear();
        printLine("Using storage: " + sharedProcessStorage.getStorageId());

        // collect information
        final String url = readTextInput("Enter server URL: ");
        final String tenantId = readTextInput("Enter tenant ID: ");
        final String productId = readTextInput("Enter product ID: ");

        final boolean useFingerprint = readBoolInput("Use fingerprint (true/false): ");
        final String seatId;
        if (useFingerprint) {
            seatId = Zentitle2Core.DeviceFingerprint.generateForCurrentMachine();
        } else {
            seatId = readTextInput("Enter seat ID: ");
        }

        // create activation
        final URI licensingApiUrl = URI.create(url);
        final IPersistence persistence = new Persistence(sharedProcessStorage, new DateTimeProvider());
        final LicensingApiOptions licensingApiOptions = new LicensingApiOptions(
                licensingApiUrl,
                tenantId,
                persistence::getAccessToken,
                persistence::getApiNonce,
                persistence::setApiNonce);
        final CloseableHttpClient httpClient = HttpClientFactory.createHttpClient(licensingApiOptions);
        final OnlineActivationOptions onlineActivationOptions = new OnlineActivationOptions(
                licensingApiUrl,
                () -> httpClient);
        final ActivationOptions activationOptions = new ActivationOptions(
                tenantId,
                productId,
                () -> seatId,
                sharedProcessStorage,
                onlineActivationOptions);
        activationOptions.setTransitionToNewStateCallback((oldState, updatedActivation) ->
                System.out.println("Activation state changed from " + oldState + " to " + updatedActivation.getState()));
        final IActivation activation = new Activation(activationOptions, persistence);

        // initialize activation
        activation.initialize();

        // activate seat
        final String activationCode = readTextInput("Enter activation code: ");
        String seatName = readTextInput("Enter seat name (keep empty for no seat name): ");
        seatName = seatName.isBlank() ? null : seatName;
        printLine("Activating seat with activation code ...");
        ActivationExtensions.activateWithCode(activation, activationCode, seatName);
        printLine("... completed.");
        final ActivationState state = activation.getState();
        printLine("\n" + "Activation state: " + state);
        printLine("\n" + "Activation info: " + activation.getInfo());

        // activation entitlement
        final ActivationEntitlementData activationEntitlement = activation.getActivationEntitlement();
        printLine("\n" + "Activation entitlement - customer name: " + activationEntitlement.getCustomerName());

        // feature - check access control
        final String featureAccessControlName = readTextInput("Enter feature access control name: ");
        printLine("Tracking usage of access control (bool) feature...");
        activation.getFeatures().trackUsage(featureAccessControlName);
        printLine("... completed.");
        printLine("\n" + "Activation info: " + activation.getInfo());

        // feature - checkout from usage count
        final String featureUsageCountName = readTextInput("Enter feature usage count name: ");
        final long amountToCheckoutFromUsageCount = readNumberInput("Amount to checkout from usage count: ");
        printLine("Checking out usage count feature...");
        activation.getFeatures().checkout(featureUsageCountName, amountToCheckoutFromUsageCount);
        printLine("... completed.");
        printLine("\n" + "Activation info: " + activation.getInfo());

        // feature - checkout from element pool
        final String featureElementPoolName = readTextInput("Enter feature element pool name: ");
        final long amountToCheckoutFromElementPool = readNumberInput("Amount to checkout from element pool: ");
        printLine("Checking out element pool feature ...");
        activation.getFeatures().checkout(featureElementPoolName, amountToCheckoutFromElementPool);
        printLine("... completed.");
        printLine("\n" + "Activation info: " + activation.getInfo());

        // feature - return to element pool
        final long amountToToReturnToElementPool = readNumberInput("Amount to return to element pool: ");
        printLine("Returning element pool feature ...");
        activation.getFeatures().returnFeature(featureElementPoolName, amountToToReturnToElementPool);
        printLine("... completed.");
        printLine("\n" + "Activation info: " + activation.getInfo());

        // refresh lease
        final ActivationOperationRes refreshLeaseResult = activation.refreshLease();
        printLine("Refreshing lease ...");
        printLine("... completed.");
        printLine("Result of refresh lease: " + refreshLeaseResult.isSuccess());
        printLine("\n" + "Activation info: " + activation.getInfo());

        // deactivate seat
        printLine("Deactivating seat ...");
        final ActivationOperationRes deactivate = activation.deactivate();
        printLine("... completed.");
        printLine("Result of deactivation: " + deactivate.isSuccess());
    }

    private static long readNumberInput(final String textToDisplay) throws IOException {
        final String text = readTextInput(textToDisplay);
        return Long.parseLong(text);
    }

    private static boolean readBoolInput(final String textToDisplay) throws IOException {
        final String text = readTextInput(textToDisplay);
        return Boolean.parseBoolean(text);
    }

    private static String readTextInput(final String textToDisplay) throws IOException {
        System.out.print(textToDisplay);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        return reader.readLine();
    }

    private static void printLine(final String text) {
        System.out.println(text);

    }
}
