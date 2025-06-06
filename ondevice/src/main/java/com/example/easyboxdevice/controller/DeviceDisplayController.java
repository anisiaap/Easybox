package com.example.easyboxdevice.controller;

import javax.swing.*;
import java.awt.*;
import org.springframework.stereotype.Component;

@Component
public class DeviceDisplayController {
    private final JFrame frame;
    private final JLabel label;

    public DeviceDisplayController() {
        // Initialize a full-screen, borderless frame
        frame = new JFrame("Easybox Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true); // Remove borders and title bar
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize to full screen
        frame.setBackground(Color.BLACK); // Background color
        frame.setAlwaysOnTop(true); // Stay on top
        frame.setResizable(false);

        // Create a centered label
        label = new JLabel("Initializing...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 48));
        label.setForeground(Color.WHITE);
        label.setBackground(Color.BLACK);
        label.setOpaque(true);

        // Use layout and add label
        frame.setLayout(new BorderLayout());
        frame.add(label, BorderLayout.CENTER);

        frame.setVisible(true);
        showIdle();
    }

    public void showIdle() {
        updateText("📷 Please scan your QR code...");
    }

    public void showLoading() {
        updateText("⏳ Verifying QR...");
    }

    public void showMessage(String message) {
        updateText(message);
    }

    public boolean showConfirmationPrompt(String message) {
        // Show Swing YES/NO dialog centered over the screen
        int result = JOptionPane.showConfirmDialog(
                frame,
                message,
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private void updateText(String text) {
        SwingUtilities.invokeLater(() -> label.setText("<html><div style='text-align: center;'>" + text.replace("\n", "<br>") + "</div></html>"));
    }
}
