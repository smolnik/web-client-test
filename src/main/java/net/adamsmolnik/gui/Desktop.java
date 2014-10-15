package net.adamsmolnik.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import net.adamsmolnik.digest.DigestNoLimitUnderHeavyLoadClient;

public class Desktop extends JPanel {

    private static final long serialVersionUID = -499384771052856134L;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void createAndShowGUI() throws Exception {
        final JFrame frame = new JFrame("Digest service client");
        Desktop desktop = new Desktop(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(desktop, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return "";
            }

            @Override
            protected void done() {

            }

        }.execute();
    }

    private static class MyWindowAdapter extends WindowAdapter {

        public MyWindowAdapter(DigestNoLimitUnderHeavyLoadClient client) {
            super();
            this.client = client;
        }

        private final DigestNoLimitUnderHeavyLoadClient client;

        @Override
        public void windowClosing(WindowEvent e) {
            closeQuietly(client);
        }
    }

    public Desktop(JFrame parent) {
        setLayout(new BorderLayout());
        Border border = getBorder();
        Border margin = new EmptyBorder(5, 10, 5, 10);
        setBorder(new CompoundBorder(border, margin));
        setPreferredSize(new Dimension(810, 420));
        JLabel headLabel = new JLabel(
                "I am a very Demanding Customer - giving the digest-no-limit-service a hard time and overwhelming work to do... ");
        headLabel.setAlignmentX(JLabel.CENTER);
        headLabel.setFont(headLabel.getFont().deriveFont(Font.BOLD, 14.0f));
        headLabel.setForeground(Color.GRAY);
        add(headLabel, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(290, 250));
        GridBagConstraints c = new GridBagConstraints();
        c.insets.left = 15;
        c.insets.top = 10;
        c.anchor = GridBagConstraints.EAST;
        JLabel hostLabel = new JLabel("Destination host (of digest-no-limit-service):");
        form.add(hostLabel, c);
        c.gridy = 1;
        JLabel objectKeyLabel = new JLabel("Object key:");
        form.add(objectKeyLabel, c);
        c.gridy = 2;
        JLabel rnLabel = new JLabel("Requests number:");
        form.add(rnLabel, c);
        c.gridy = 3;
        JLabel wnLabel = new JLabel("Workers number:");
        form.add(wnLabel, c);
        c.gridy = 4;
        JLabel susLabel = new JLabel("Suspension (ms):");
        form.add(susLabel, c);
        c.gridy = 5;
        JLabel algLabel = new JLabel("Algorithm:");
        form.add(algLabel, c);
        c.gridy = 0;
        c.gridx = 1;
        c.weightx = 1;
        c.insets.right = 15;
        c.fill = GridBagConstraints.HORIZONTAL;
        JTextField hostTextField = new JTextField("digest.adamsmolnik.net");
        hostTextField.setBackground(Color.decode("#FCAEAE"));
        form.add(hostTextField, c);
        c.gridy = 1;
        JTextField objectKeyTextField = new JTextField("largefiles/file_sizedOf10000000");
        form.add(objectKeyTextField, c);
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        JTextField rnTextField = new JTextField("2000", 5);
        form.add(rnTextField, c);
        c.gridy = 3;
        JTextField wnTextField = new JTextField("10", 5);
        form.add(wnTextField, c);
        c.gridy = 4;
        JTextField susTextField = new JTextField("300", 5);
        form.add(susTextField, c);
        c.gridy = 5;
        JTextField algTextField = new JTextField("SHA-256", 5);
        form.add(algTextField, c);

        c.gridy = 6;
        JPanel buttonsPanel = new JPanel();
        JButton runButton = new JButton("Run processing");

        JButton stopButton = new JButton("Stop");
        buttonsPanel.add(runButton);
        buttonsPanel.add(stopButton);
        form.add(buttonsPanel, c);
        add(form, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.add(Box.createVerticalGlue());
        JPanel currentStatusPanel = new JPanel(new BorderLayout());
        currentStatusPanel.add(new JLabel("Current status:"), BorderLayout.WEST);
        statusPanel.add(currentStatusPanel);
        JTextField currentTextField = new JTextField();
        currentTextField.setEditable(false);
        statusPanel.add(Box.createVerticalStrut(2));
        statusPanel.add(currentTextField);
        statusPanel.add(Box.createVerticalStrut(10));

        JPanel statusHistoryPanel = new JPanel(new BorderLayout());
        statusHistoryPanel.add(new JLabel("Status history:"), BorderLayout.WEST);
        statusPanel.add(statusHistoryPanel);

        JTextArea historyTextArea = new JTextArea(5, 1);
        historyTextArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(historyTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        statusPanel.add(Box.createVerticalStrut(2));
        statusPanel.add(scroll);

        add(statusPanel, BorderLayout.SOUTH);

        currentTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                try {
                    Document histDoc = historyTextArea.getDocument();
                    String text = histDoc.getText(0, histDoc.getLength());
                    String newText = event.getDocument().getText(0, event.getDocument().getLength());
                    newText = text == null ? newText : newText + "\n";
                    histDoc.insertString(0, newText, SimpleAttributeSet.EMPTY);
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });;

        runButton.addActionListener(event -> {
            String host = hostTextField.getText();
            String objectKey = objectKeyTextField.getText();
            String alg = algTextField.getText();
            int requestsNumber = Integer.valueOf(rnTextField.getText());
            int suspensionInMs = Integer.valueOf(susTextField.getText());
            int workersNumber = Integer.valueOf(wnTextField.getText());

            DigestNoLimitUnderHeavyLoadClient.Builder builder = new DigestNoLimitUnderHeavyLoadClient.Builder(host, objectKey)
                    .requestsNumber(requestsNumber).suspensionInMs(suspensionInMs).workersNumber(workersNumber).algorithm(alg);

            final DigestNoLimitUnderHeavyLoadClient client = builder.build();
            try {
                parent.addWindowListener(new MyWindowAdapter(client));
                stopButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentTextField.setText("Service Digest requests processing is about to stop...");
                        closeQuietly(client);
                        stopButton.removeActionListener(this);
                    }
                });

                currentTextField.setText(requestsNumber + " Service Digest requests is about to be sent...");
                client.send(Optional.of(progressEvent -> {
                    try {
                        if (progressEvent.completed) {
                            currentTextField.setText("Completed");
                            return;
                        }
                        String info = "Submitted " + progressEvent.submitted + ", succeeded " + progressEvent.succeeded + ", failed "
                                + progressEvent.failed;
                        currentTextField.setText(info);
                    } catch (Exception ex) {
                        currentTextField.setText(ex.getLocalizedMessage());
                        ex.printStackTrace();
                    }
                }));
            } catch (Exception ex) {
                currentTextField.setText(ex.getLocalizedMessage());
                ex.printStackTrace();
                closeQuietly(client);
            }

        });

    }

    private static void closeQuietly(DigestNoLimitUnderHeavyLoadClient client) {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ex) {
            // deliberately ignored
        }
    }

}
