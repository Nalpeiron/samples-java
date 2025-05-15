package com.nalpeiron.zentitle.sample;

import com.nalpeiron.zentitle.licensingclient.IActivation;
import com.nalpeiron.zentitle.licensingclient.api.model.ActivationCodeCredentialsModel;
import com.nalpeiron.zentitle.licensingclient.api.model.OpenIdTokenCredentialsModel;
import com.nalpeiron.zentitle.licensingclient.api.model.PasswordCredentialsModel;

public class ActivationExtensions {

    public static void activateWithCode(IActivation activation, String activationCode) {
        activateWithCode(activation, activationCode, null);
    }

    public static void activateWithCode(IActivation activation, String activationCode, String seatName) {
        if (activation == null) {
            throw new IllegalArgumentException("activation cannot be null");
        }

        if (activationCode == null || activationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("activationCode cannot be null or whitespace");
        }

        activation.activate(new ActivationCodeCredentialsModel().code(activationCode), seatName);
    }

    public static void activateWithOpenIdToken(IActivation activation, String openIdAccessToken, String seatName) {
        if (activation == null) {
            throw new IllegalArgumentException("activation cannot be null");
        }

        if (openIdAccessToken == null || openIdAccessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("openIdAccessToken cannot be null or whitespace");
        }

        activation.activate(new OpenIdTokenCredentialsModel().token(openIdAccessToken), seatName);
    }

    public static void activateWithPassword(IActivation activation, String username, String password, String seatName) {
        if (activation == null) {
            throw new IllegalArgumentException("activation cannot be null");
        }

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username cannot be null or whitespace");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("password cannot be null or whitespace");
        }

        activation.activate(new PasswordCredentialsModel().username(username).password(password), seatName);
    }
}