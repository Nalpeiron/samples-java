package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.licensingclient.zentitle2core.PredefinedSystemFolder;
import com.nalpeiron.zentitle.sample.gui.Prompt;
import com.nalpeiron.zentitle.licensingclient.persistence.PersistentData;
import com.nalpeiron.zentitle.licensingclient.persistence.storage.IActivationStorage;
import com.nalpeiron.zentitle.licensingclient.persistence.storage.PlainTextFileActivationStorage;
import com.nalpeiron.zentitle.licensingclient.zentitle2core.SecureActivationStorage;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LicenseStorage {
    private static final Logger logger = LoggerFactory.getLogger(LicenseStorage.class);
    private static final Path APP_DIRECTORY = Paths.get("Z2_OnlineActivation_Console");

    private final Prompt prompt;
    private final Terminal terminal;

    public LicenseStorage(final Terminal terminal, final Prompt prompt) {
        this.prompt = prompt;
        this.terminal = terminal;
    }

    public IActivationStorage initialize(final boolean useCoreLibrary) {
        final IActivationStorage storage;
        if (useCoreLibrary) {
            storage = SecureActivationStorage.withAppDirectory(PredefinedSystemFolder.USER_DATA, APP_DIRECTORY.toString(), "license.encrypted");
        } else {
            final String localAppData = System.getProperty("user.home");
            final Path path = Paths.get(localAppData).resolve(APP_DIRECTORY).resolve("license.json");
            storage = new PlainTextFileActivationStorage(path);
        }

        logger.warn("- Using {} storage with file: {}", storage.getClass().getName(), storage.getStorageId());

        final PersistentData data = storage.load();
        if (data.isEmpty()) {
            return storage;
        }

        final boolean deleteExistingFiles = prompt.confirm("Do you want to delete existing activation data in the persistent storage?");
        if (deleteExistingFiles) {
            terminal.writer().println("Deleting already persisted activation data...");
            terminal.flush();
            storage.clear();
        }

        return storage;
    }
}