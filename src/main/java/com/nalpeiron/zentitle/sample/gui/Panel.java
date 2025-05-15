package com.nalpeiron.zentitle.sample.gui;

import java.util.Objects;

public class Panel {
    private static final int LEADING_DASH_COUNT = 3;
    private static final String HORIZONTAL_LINE = "------------------------------------------";

    private final String info;
    private String header = null;

    public Panel(final String info) {
        this.info = info;
    }

    public Panel header(final String header) {
        this.header = header;
        return this;
    }

    @Override
    public String toString() {
        final String formattedHeader = Objects.isNull(header) || header.isEmpty() ? "" : " " + header + " ";
        final int indexOfTrailingLine = LEADING_DASH_COUNT + formattedHeader.length();
        final String inFrontOfHeader = HORIZONTAL_LINE.substring(0, LEADING_DASH_COUNT);
        final String behindHeader = (indexOfTrailingLine > 0) ? HORIZONTAL_LINE.substring(indexOfTrailingLine) : "";
        final String headerLine = inFrontOfHeader + formattedHeader + behindHeader;
        return headerLine + System.lineSeparator() +
                info + System.lineSeparator() +
                HORIZONTAL_LINE + System.lineSeparator();
    }
}
