package com.nalpeiron.zentitle.sample.gui;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.util.List;

public class Prompt {

    private final Terminal terminal;
    private final LineReader lineReader;

    public Prompt(final Terminal terminal, final LineReader lineReader) {
        this.terminal = terminal;
        this.lineReader = lineReader;
    }

    public String select(String display, List<String> options) throws IOException {
        do {
            showMenu(options);
            final Integer choice = readInput(options);
            if (choice != null) {
                return options.get(choice - 1);
            }
        } while (true);
    }

    public void showMenu(final List<String> options) {
        terminal.writer().println();
        terminal.writer().println("Menu:");
        for (int i = 0; i < options.size(); i++) {
            terminal.writer().println((i + 1) + ". " + options.get(i));
        }
        terminal.writer().println("Select an option (1-" + options.size() + "):");
        terminal.flush();
    }

    private Integer readInput(final List<String> options) {
        final String input = lineReader.readLine("> ");
        int choice;
        try {
            choice = Integer.parseInt(input);
            if (choice < 1 || choice > options.size()) {
                terminal.writer().println("Invalid choice. Please try again.");
                terminal.flush();
                return null;
            }
        } catch (NumberFormatException e) {
            terminal.writer().println("Invalid input. Please enter a number.");
            terminal.flush();
            return null;
        }
        return choice;
    }


    public boolean confirm(final String prompt) {
        while (true) {
            final String input = lineReader.readLine(prompt + " (yes/no): ").trim().toLowerCase();
            if ("yes".equals(input) || "y".equals(input)) {
                return true;
            } else if ("no".equals(input) || "n".equals(input)) {
                return false;
            } else {
                terminal.writer().println("Invalid input. Please enter 'yes' or 'no'.");
                terminal.flush();
            }
        }
    }

    public String input(final String prompt) {
        return lineReader.readLine(prompt);
    }

    public int inputInt(final String prompt) {
        while (true) {
            try {
                String input = lineReader.readLine(prompt);
                return Integer.parseInt(input);
            } catch (final NumberFormatException exception) {
                terminal.writer().println("Invalid input. Please enter a valid number.");
                terminal.flush();
            }
        }
    }
}
