package com.nalpeiron.zentitle.sample.gui;

import io.bretty.console.table.Alignment;
import io.bretty.console.table.ColumnFormatter;
import io.bretty.console.table.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableProxy {

    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public TableProxy addColumn(final String header) {
        headers.add(header);
        return this;
    }

    public TableProxy addRow(final String... cells) {
        final List<String> row = Arrays.asList(cells);
        rows.add(row);
        return this;
    }

    public String toString() {
        final ColumnFormatter<String> formatter = ColumnFormatter.text(Alignment.LEFT, 30);

        final String[] firstColumn = prepareColumnData(0);
        final String firstHeader = headers.get(0);
        final Table.Builder builder = new Table.Builder(firstHeader, firstColumn, formatter);

        for (int i = 1; i < headers.size(); i++) {
            final String header = headers.get(i);
            final String[] column = prepareColumnData(i);
            builder.addColumn(header, column, formatter);
        }
        return builder.build().toString();
    }

    private String[] prepareColumnData(final int i) {
        return rows.stream()
                .map(cells -> cells.get(i))
//                .map(cell -> cell == null ? "" : cell)
//                .map(cell -> cell.length() > 30 ? cell.substring(0, 30) : cell)
//                .map(Object::toString)
                .map(this::indent)
                .toArray(String[]::new);
    }

    private String indent(final String text) {
        return " " + text + " ";
    }
}
