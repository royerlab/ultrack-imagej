package org.czbiohub.royerlab;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandExecutor {

    private final String command;
    private final Frame owner;

    public CommandExecutor(Frame owner, String command) {
        this.owner = owner;
        this.command = command;
    }

    public CompletableFuture<Integer> executeCommand() {
        // Set up the dialog to show command output
        JDialog dialog = new JDialog(this.owner, "Command Output", true);
        dialog.setSize(400, 300);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(this.owner);
        dialog.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textArea.append("Executing command: " + this.command + "\n\n");
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);

        JButton closeButton = new JButton("Abort");
        dialog.add(closeButton, BorderLayout.SOUTH);

        CompletableFuture<Integer> exitCode = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> dialog.setVisible(true));

        Runnable task = () -> {
            try {
                // Execute the shell command
                Process process = Runtime.getRuntime().exec(this.command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                closeButton.addActionListener(e -> {
                    // abort the process
                    if (process.isAlive()) {
                        process.destroy();
                        process.destroyForcibly();
                    }
                    dialog.dispose();
                });

                String line;
                while ((line = reader.readLine()) != null) {
                    // Append the output to the text area
                    String finalLine = line;
                    SwingUtilities.invokeLater(() -> textArea.append(finalLine + "\n"));
                    System.out.println(line);
                }

                int exit = process.waitFor();

                if (exit != 0) {
                    textArea.setForeground(Color.RED);
                    // Append the exit code to the text area
                    InputStream errorStream = process.getErrorStream();
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        // Append the output to the text area
                        String finalLine = errorLine;
                        SwingUtilities.invokeLater(() -> textArea.append(finalLine + "\n"));
                        System.out.println(errorLine);
                    }
                }

                // Wait for the process to complete
                exitCode.complete(exit);
                closeButton.setText("Close");
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                exitCode.completeExceptionally(ex);
            }
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        };

        Executors.newSingleThreadExecutor().submit(task);

        return exitCode;
    }
}