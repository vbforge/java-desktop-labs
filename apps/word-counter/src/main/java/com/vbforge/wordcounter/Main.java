package com.vbforge.wordcounter;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Global Dark Theme Overrides
        UIManager.put("nimbusBase", new java.awt.Color(0x161b22));
        UIManager.put("nimbusBlueGrey", new java.awt.Color(0x2a313c));
        UIManager.put("control", new java.awt.Color(0x161b22));
        UIManager.put("text", new java.awt.Color(0xe6edf3));
        UIManager.put("background", new java.awt.Color(0x0d1117));
        UIManager.put("Label.foreground", new java.awt.Color(0xe6edf3));
        UIManager.put("Panel.background", new java.awt.Color(0x0d1117));
        UIManager.put("CheckBox.background", new java.awt.Color(0x161b22));
        UIManager.put("CheckBox.foreground", new java.awt.Color(0x8b949e));
        
        // Specific overrides for JFileChooser dialog
        UIManager.put("FileChooser.background", new java.awt.Color(0x161b22));
        UIManager.put("FileChooser.foreground", new java.awt.Color(0xe6edf3));
        UIManager.put("FileChooser.listViewBackground", new java.awt.Color(0x0d1117));
        UIManager.put("FileChooser.listViewForeground", new java.awt.Color(0xe6edf3));
        UIManager.put("FileChooser.listViewSelectionBackground", new java.awt.Color(0xf89820));
        UIManager.put("FileChooser.listViewSelectionForeground", java.awt.Color.BLACK);

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}