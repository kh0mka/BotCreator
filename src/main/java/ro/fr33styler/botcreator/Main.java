package ro.fr33styler.botcreator;

import ro.fr33styler.botcreator.bot.Bot;
import ro.fr33styler.botcreator.logger.LogHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

public class Main {

    public static final Logger LOGGER = Logger.getLogger("BotCreator");
    private static final Deque<Bot> bots = new ArrayDeque<>();

    private static List<String> loadNicknames(String filePath) {
        List<String> nicknames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String nickname = line.trim();
                // Проверяем, что ник соответствует требованиям
                if (nickname.length() >= 4 && nickname.length() <= 16 && nickname.matches("[a-zA-Z0-9]+")) {
                    nicknames.add(nickname);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read nicknames file: " + e.getMessage());
        }

        if (nicknames.isEmpty()) {
            LOGGER.log(Level.WARNING, "No valid nicknames found in the file!");
        }

        return nicknames;
    }

    public static void main(String[] args) {

        if (Double.parseDouble(System.getProperty("java.class.version")) < 61) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null,
                    "You need Java 17 or higher to run this!",
                    "Version not supported!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame frame = new JFrame("BotCreator");

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Bottom
        JPanel bottomPanel = new JPanel();

        JComboBox<String> botsBox = new JComboBox<>();
        botsBox.addItem("All");
        bottomPanel.add(botsBox);

        JTextField sendInput = new JTextField("/", 64);
        bottomPanel.add(sendInput);

        sendInput.addKeyListener(new KeyAdapter() {

            private int position = 0;
            private final List<String> history = new ArrayList<>();

            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_UP && position > 0) {
                    sendInput.setText(history.get(--position));
                } else if (event.getKeyCode() == KeyEvent.VK_DOWN && position < history.size()) {
                    sendInput.setText(position + 1 == history.size() ? "/" : history.get(++position));
                } else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    String selected = (String) botsBox.getSelectedItem();

                    String text = sendInput.getText();
                    for (Bot bot : bots) {
                        if (selected == null || selected.equals("All") || selected.equals(bot.getName())) {
                            if (text.startsWith("/")) {
                                bot.executeCommand(text.substring(1));
                            } else {
                                bot.sendMessage(text);
                            }
                        }
                    }
                    history.remove(text);
                    history.add(text);

                    sendInput.setText("/");
                    position = history.size();
                }
            }

        });

        frame.add(bottomPanel, BorderLayout.PAGE_END);

        //Center
        JScrollPane centerPane = new JScrollPane();
        centerPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        centerPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextArea logArea = new JTextArea(10, 80);
        logArea.setEditable(false);
        centerPane.getViewport().setView(logArea);

        LOGGER.addHandler(new LogHandler(logArea));

        frame.add(centerPane, BorderLayout.CENTER);

        //Top
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Host: "));

        JTextField hostInput = new JTextField("127.0.0.1", 10);
        topPanel.add(hostInput);

        topPanel.add(new JLabel("Port: "));
        JTextField portInput = new JTextField("25565", 5);
        topPanel.add(portInput);

        topPanel.add(new JLabel("Clients: "));

        JTextField clientsInput = new JTextField("1", 3);
        topPanel.add(clientsInput);

        JButton connect = new JButton("Connect");
        topPanel.add(connect);

        topPanel.add(new JLabel("Nickname File: "));
        JTextField nicknameFileInput = new JTextField("nicknames.txt", 20);
        topPanel.add(nicknameFileInput);

        connect.addActionListener(action -> {

            int port;
            try {
                port = Integer.parseInt(portInput.getText());
            } catch (NumberFormatException exception) {
                LOGGER.log(Level.SEVERE, "The port must be a number!");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(clientsInput.getText());
            } catch (NumberFormatException exception) {
                LOGGER.log(Level.SEVERE, "The number of clients must be a number!");
                return;
            }

            if (amount < 0) {
                LOGGER.log(Level.SEVERE, "The number of clients must be 0 or higher!");
                return;
            }

            String host = hostInput.getText();
            String nicknameFile = nicknameFileInput.getText();

            // Loading nicknames from a specified file
            List<String> availableNicknames = loadNicknames(nicknameFile);

            if (availableNicknames.isEmpty()) {
                LOGGER.log(Level.SEVERE, "No valid nicknames found in the file!");
                return;
            }

            connect.setEnabled(false);
            connect.setText("Connecting...");

            // Creating or deleting bots
            while (bots.size() < amount) {
                int id = bots.size();
                String botName;

                // Selecting a random nickname from the list
                if (!availableNicknames.isEmpty()) {
                    botName = availableNicknames.remove((int) (Math.random() * availableNicknames.size()));
                } else {
                    LOGGER.log(Level.WARNING, "Not enough nicknames, skipping bot creation.");
                    break;
                }

                bots.addLast(new Bot(botName));
                botsBox.addItem(botName);
            }
            while (bots.size() > amount) {
                Bot bot = bots.removeLast();
                bot.disconnect();
                botsBox.removeItem(bot.getName());
            }

            // Connecting bots
            for (Bot client : bots) {
                if (!client.isOnline()) {
                    client.connect(host, port);
                }
            }
            connect.setText("Connect");
            connect.setEnabled(true);
        });

        frame.add(topPanel, BorderLayout.PAGE_START);

        frame.pack();
        frame.setVisible(true);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOGGER.log(Level.SEVERE, throwable.getMessage(), throwable));
        LOGGER.info("Bot Creator has started!");

    }

}
