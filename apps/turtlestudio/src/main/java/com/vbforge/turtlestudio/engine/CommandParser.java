package com.vbforge.turtlestudio.engine;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    public static List<Command> parse(String script) {
        List<Command> commands = new ArrayList<>();
        String[] lines = script.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s+");
            String action = parts[0].toUpperCase();

            try {
                switch (action) {
                    case "FORWARD", "F" -> commands.add(new Command(Command.Type.FORWARD, parseNum(parts, i)));
                    case "BACK", "B" -> commands.add(new Command(Command.Type.BACK, parseNum(parts, i)));
                    case "LEFT", "L" -> commands.add(new Command(Command.Type.LEFT, parseNum(parts, i)));
                    case "RIGHT", "R" -> commands.add(new Command(Command.Type.RIGHT, parseNum(parts, i)));
                    case "PEN" -> {
                        if (parts.length > 1 && parts[1].equalsIgnoreCase("UP")) commands.add(new Command(Command.Type.PEN_UP, 0));
                        else commands.add(new Command(Command.Type.PEN_DOWN, 0));
                    }
                    case "CLEAR" -> commands.add(new Command(Command.Type.CLEAR, 0));
                    default -> throw new IllegalArgumentException("Unknown command: " + action + " on line " + (i + 1));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Error on line " + (i + 1) + ": " + e.getMessage());
            }
        }
        return commands;
    }

    private static double parseNum(String[] parts, int lineIdx) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing number value.");
        }
        try {
            return Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + parts[1]);
        }
    }
}