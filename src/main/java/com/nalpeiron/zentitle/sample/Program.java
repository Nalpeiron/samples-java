package com.nalpeiron.zentitle.sample;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nalpeiron.zentitle.corelib.jni.ZentitleJniLazyLoading;
import com.nalpeiron.zentitle.licensingclient.ActivationState;
import com.nalpeiron.zentitle.licensingclient.ISharedActivation;
import com.nalpeiron.zentitle.licensingclient.SharedActivation;
import com.nalpeiron.zentitle.licensingclient.SharedActivationExtensions;
import com.nalpeiron.zentitle.licensingclient.api.model.ActivationMode;
import com.nalpeiron.zentitle.licensingclient.licensingapi.HttpClientFactory;
import com.nalpeiron.zentitle.licensingclient.licensingapi.LicensingApiOptions;
import com.nalpeiron.zentitle.licensingclient.options.*;
import com.nalpeiron.zentitle.licensingclient.persistence.IPersistence;
import com.nalpeiron.zentitle.licensingclient.persistence.Persistence;
import com.nalpeiron.zentitle.licensingclient.persistence.storage.IActivationStorage;
import com.nalpeiron.zentitle.licensingclient.services.DateTimeProvider;
import com.nalpeiron.zentitle.licensingclient.zentitle2core.DeviceFingerprint;
import com.nalpeiron.zentitle.licensingclient.zentitle2core.Zentitle2CoreFactory;
import com.nalpeiron.zentitle.sample.gui.Prompt;
import com.nalpeiron.zentitle.sample.options.AppSettings;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Program {
    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    private static final String QUIT_ACTION = "Quit";

    private final LicenseStorage storage;
    private final Prompt prompt;
    private final Terminal terminal;
    private final DisplayHelper displayHelper;
    private final ActivationActions activationActionsStatic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Program() throws IOException {
        terminal = TerminalBuilder.builder()
                .dumb(true)
                .system(true)
                .build();
        final LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        prompt = new Prompt(terminal, lineReader);
        displayHelper = new DisplayHelper(terminal);
        activationActionsStatic = new ActivationActions(terminal, prompt, displayHelper, objectMapper);
        storage = new LicenseStorage(terminal, prompt);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void run() throws Exception {
        final File appSettings;
        if (new File("appsettings.json").exists()) {
            appSettings = new File("appsettings.json");
        } else if (new File("src/main/java/com/nalpeiron/zentitle/sample/appsettings.json").exists()) {
            appSettings = new File("src/main/java/com/nalpeiron/zentitle/sample/appsettings.json");
        } else {
            appSettings = new File("samples/src/main/java/com/nalpeiron/zentitle/sample/appsettings.json");
        }
        final AppSettings config = objectMapper.readValue(appSettings, AppSettings.class);

        final boolean useCoreLibrary = config.isUseCoreLibrary();
        if (useCoreLibrary) {
            logger.warn("- Using Zentitle2Core C++ library for device fingerprint, secure license storage and offline activation operations");
            final boolean isPrintLibraryLogs = config.isPrintLibraryLogs();
            if (isPrintLibraryLogs) {
                enableCoreLibraryLogging();
            }
        } else {
            logger.warn("- Zentitle2Core C++ library usage is disabled in 'appsettings.json', the won't be loaded and offline activation won't work");
        }

        final AppSettings.Licensing licensingOptionsConfig = config.getLicensing();
        final IActivationStorage licenseStorage = storage.initialize(useCoreLibrary);

        final String licensingUrl = licensingOptionsConfig.getApiUrl();
        final URI licensingApiUrl = URI.create(licensingUrl);
        final IPersistence persistence = new Persistence(licenseStorage, new DateTimeProvider());
        final LicensingApiOptions licensingApiOptions = new LicensingApiOptions(
                licensingApiUrl,
                licensingOptionsConfig.getTenantId(),
                persistence::getAccessToken,
                persistence::getApiNonce,
                persistence::setApiNonce);
        final CloseableHttpClient httpClient = HttpClientFactory.createHttpClient(licensingApiOptions);
        final OnlineActivationOptions onlineActivationOptions = new OnlineActivationOptions(
                licensingApiUrl,
                () -> httpClient);
        final String tenantRsaKeyModulus = licensingOptionsConfig.getTenantRsaKeyModulus();
        final OfflineActivationOptions offlineActivationOptions = new OfflineActivationOptions(tenantRsaKeyModulus);
        final ActivationOptions activationOptions = new ActivationOptions(
                licensingOptionsConfig.getTenantId(),
                licensingOptionsConfig.getProductId(),
                (() -> {
                    if (useCoreLibrary && prompt.confirm("Use device fingerprint for seat ID generation?")) {
                        terminal.writer().println("Generating device fingerprint...");
                        terminal.flush();
                        return DeviceFingerprint.generateForCurrentMachine();
                    }
                    return prompt.input("Enter license seat ID: ");
                }),
                licenseStorage,
                onlineActivationOptions,
                offlineActivationOptions);
        activationOptions.setTransitionToNewStateCallback((oldState, updatedActivation) ->
                logger.info("Activation state changed from [{}] to [{}]", oldState, updatedActivation.getState()));

        final ActivationLockingOptions activationLockingOptions = new ActivationLockingOptionsBuilder()
                .useSystemLock("Global-Zentitle.Licensing.Client-SampleApplication")
                .build();

        final ISharedActivation activation = new SharedActivation(activationOptions, activationLockingOptions, persistence);

        terminal.writer().println("Initializing activation...");
        terminal.flush();
        SharedActivationExtensions.initializeWithLock(activation);

        final LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        String selectedOption = null;

        do {
            final List<String> options = activation.executeWithLock(a -> {
                final ActivationMode activationMode = a.getInfo().getMode();
                final ActivationAction[] activationActions = activationActionsStatic.getAvailableActions().get(a.getState());
                final List<String> opt = Arrays.stream(activationActions)
                        .filter(action -> action.getAvailableInModes().contains(activationMode))
                        .map(ActivationAction::getName)
                        .collect(Collectors.toList());
                opt.add(QUIT_ACTION);
                return opt;
            });

            prompt.showMenu(options);
            final Integer choice = readInput(lineReader, options);
            if (choice != null) {
                try {
                    selectedOption = processInput(options, choice, activation);
                } catch (final Exception exception) {
                    terminal.writer().println("Error: " + exception.getMessage());
                    terminal.flush();
                }
            }
        } while (!QUIT_ACTION.equals(selectedOption));

        activation.close();
        httpClient.close();
    }

    private void enableCoreLibraryLogging() {
        final ZentitleJniLazyLoading zentitleJniLazyLoading = Zentitle2CoreFactory.getZentitleJNI();
        final String pathToCoreLibLogFile = Paths.get("core-lib-test.log").toString();
        logger.info("Enabling core library logging to file: {}", pathToCoreLibLogFile);
        final boolean enabled = zentitleJniLazyLoading.enableLogging(pathToCoreLibLogFile);
        if (enabled) {
            logger.info("Core library logging enabled");
        } else {
            logger.warn("Failed to enable core library logging");
        }
    }

    private Integer readInput(final LineReader lineReader, final List<String> options) {
        String input = lineReader.readLine("> ");
        int choice;
        try {
            choice = Integer.parseInt(input);
            if (choice < 1 || choice > options.size()) {
                terminal.writer().println("Invalid choice. Please try again.");
                terminal.flush();
                return null;
            }
        } catch (final NumberFormatException e) {
            terminal.writer().println("Invalid input. Please enter a number.");
            terminal.flush();
            return null;
        }
        return choice;
    }

    private String processInput(final List<String> options, final int choice, final ISharedActivation activation) {
        final String selectedOption = options.get(choice - 1);
        terminal.writer().println("You selected: " + selectedOption);
        terminal.flush();

        if (QUIT_ACTION.equals(selectedOption)) {
            terminal.writer().println("Exiting...");
            terminal.flush();
        } else {
            terminal.writer().println("Performing action for: " + selectedOption);
            terminal.flush();

            activation.executeWithLock(a -> {
                final ActivationState state = a.getState();
                final ActivationAction activationAction = Arrays.stream(activationActionsStatic.getAvailableActions().get(state))
                        .filter(action -> action.getName().equals(selectedOption))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unknown action: " + selectedOption));
                final LockingActivationActionHandler.Callback callback = callbackActivation -> {
                    try {
                        activationAction.executeAction(callbackActivation);
                    } catch (final IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                };
                final boolean result = LockingActivationActionHandler.lockPullStateAndExecute(callback, a);
                if (!result) {
                    displayHelper.writeError("Local activation state changed, operation aborted and state refreshed");
                }
            });
        }
        return selectedOption;
    }
}
