

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.Queue;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;

class MCQ {
    private final String question;
    private final String[] choices;
    private final String answer;
    private final String reason;
    private final String questionAudioPath;
    private final String explanationAudioPath;

    public MCQ(String question, String[] choices, String answer, String reason, String questionAudioPath,
            String explanationAudioPath) {
        this.question = question;
        this.choices = choices;
        this.answer = answer.trim().substring(0, 1); // Extract only the letter (e.g., 'A', 'B', etc.)
        this.reason = reason;
        this.questionAudioPath = questionAudioPath;
        this.explanationAudioPath = explanationAudioPath;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getChoices() {
        return choices;
    }

    public String getAnswer() {
        return answer;
    } // Return only the answer letter (e.g., "B")

    public String getReason() {
        return reason;
    }

    public String getQuestionAudioPath() {
        return questionAudioPath;
    }

    public String getExplanationAudioPath() {
        return explanationAudioPath;
    }

    public static void shuffleQuestions(ArrayList<MCQ> questions) {
        long seed = System.nanoTime();
        Collections.shuffle(questions, new Random(seed));
    }
}

public class Main extends JFrame {
    private static final Map<String, Queue<Double>> quizHistory = new LinkedHashMap<>(); // Record-keeping structure
    private static final String HISTORY_FILE = "quiz_history.dat"; // File for storing history
    private final String quizFileName; // Store the filename of the quiz
    private ArrayList<MCQ> questions;
    private final ArrayList<String> userAnswers = new ArrayList<>();
    private final ArrayList<MCQ> incorrectQuestions = new ArrayList<>(); // Store incorrect answers
    int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private final JLabel questionLabel;
    private JButton[] choiceButtons;
    private final JLabel timerLabel;
    private final JLabel remainingQuestionsLabel;
    private Timer timer;
    private int timeLeft = 60;
    @SuppressWarnings("unused")
    private final String[] optionLetters = { "A", "B", "C", "D" };
    private JButton selectedButton = null;
    private int timeBetweenQuestions = 15; // Default time between questions
    private int fontSize = 24; // Default font size for question and choices

    // Constructor that takes in the font size and time between questions
    public Main(ArrayList<MCQ> questions, int fontSize, int timeBetweenQuestions, String quizFileName) {
        loadHistoryFromFile();
        this.quizFileName = quizFileName;
        this.questions = questions;
        this.fontSize = fontSize;
        this.timeBetweenQuestions = timeBetweenQuestions;

        setTitle("MCQ Quiz");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.DARK_GRAY);

        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setForeground(new Color(255, 165, 0)); // Orange text for the question
        // choiceButtons[i].setFont(new Font("Arial", Font.PLAIN, fontSize)); // Set the
        // font size
        add(questionLabel, BorderLayout.NORTH);

        JPanel choicesPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        choicesPanel.setBackground(Color.DARK_GRAY);
        choiceButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            choiceButtons[i] = new JButton();
            choiceButtons[i].setForeground(Color.WHITE);
            choiceButtons[i].setBackground(Color.GRAY);
            choiceButtons[i].setFocusPainted(false);
            choiceButtons[i].setBorderPainted(false);
            choiceButtons[i].setOpaque(true);
            questionLabel.setFont(new Font("Arial", Font.PLAIN, fontSize)); // Apply the selected font size
            choiceButtons[i].setFont(new Font("MS Mincho", Font.PLAIN, fontSize)); // Correctly set the font size here

            int finalI = i;
            choiceButtons[i].addActionListener(e -> selectChoice(finalI));
            choiceButtons[i].addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (choiceButtons[finalI] != selectedButton) {
                        choiceButtons[finalI].setBackground(Color.LIGHT_GRAY);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (choiceButtons[finalI] != selectedButton) {
                        choiceButtons[finalI].setBackground(Color.GRAY);
                    }
                }
            });
            choicesPanel.add(choiceButtons[i]);
        }

        add(choicesPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.DARK_GRAY);
        timerLabel = new JLabel("Time left: 60", SwingConstants.CENTER);
        timerLabel.setForeground(Color.YELLOW);
        bottomPanel.add(timerLabel, BorderLayout.WEST);

        remainingQuestionsLabel = new JLabel("Questions left: " + (questions.size() - currentQuestionIndex),
                SwingConstants.CENTER);
        remainingQuestionsLabel.setForeground(Color.WHITE);
        bottomPanel.add(remainingQuestionsLabel, BorderLayout.CENTER);

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new NextButtonListener());
        bottomPanel.add(nextButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        loadQuestion();
        startTimer();
    }

    private void loadQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            showResults();
            return;
        }

        MCQ question = questions.get(currentQuestionIndex);
        questionLabel.setText("<html><h2>" + question.getQuestion() + "</h2></html>");
        questionLabel.setFont(new Font("Arial", Font.PLAIN, fontSize)); // Dynamically set font size for each question

        String[] choices = question.getChoices();
        for (int i = 0; i < 4; i++) {
            choiceButtons[i].setText(choices[i]);
            choiceButtons[i].setBackground(Color.GRAY); // Reset button background
        }

        remainingQuestionsLabel.setText("Questions left: " + (questions.size() - currentQuestionIndex - 1));

        // Play question audio
        AudioPlayer.playAudioAsync(question.getQuestionAudioPath());
    }

    public class AudioPlayer {
        private static Clip currentClip;

        public static void playAudioAsync(String filePath) {
            stopCurrentAudio(); // Stop any audio already playing

            new Thread(() -> {
                try {
                    File audioFile = new File(filePath);
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    currentClip = AudioSystem.getClip();
                    currentClip.open(audioStream);
                    currentClip.start();
                } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                    System.err.println("Error playing audio: " + e.getMessage());
                }
            }).start();
        }

        public static void stopCurrentAudio() {
            if (currentClip != null && currentClip.isRunning()) {
                currentClip.stop();
                currentClip.close();
            }
        }
    }

    private void startTimer() {
        timeLeft = timeBetweenQuestions; // Use the selected time for each question
        timerLabel.setText("Time left: " + timeLeft);
        timer = new Timer(1000, (ActionEvent e) -> {
            timeLeft--;
            timerLabel.setText("Time left: " + timeLeft);
            if (timeLeft <= 0) {
                timer.stop();
                checkAnswer();
                nextQuestion();
            }
        });
        timer.start();
    }

    private void selectChoice(int index) {
        for (JButton btn : choiceButtons) {
            btn.setBackground(Color.GRAY);
            btn.setForeground(Color.WHITE); // Reset text color
        }

        selectedButton = choiceButtons[index];
        selectedButton.setBackground(Color.WHITE);
        selectedButton.setForeground(Color.BLUE);
    }

    private void checkAnswer() {
        if (selectedButton == null) {
            JOptionPane.showMessageDialog(this, "Please select an answer before proceeding!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        MCQ question = questions.get(currentQuestionIndex);
        String selectedAnswer = selectedButton.getText().substring(0, 1).toUpperCase();

        // Store user's answer
        userAnswers.add(selectedAnswer);

        // Play explanation audio asynchronously
        AudioPlayer.playAudioAsync(question.getExplanationAudioPath());

        // Show result dialog
        if (selectedAnswer.equals(question.getAnswer())) {
            JOptionPane.showMessageDialog(this,
                    "<html><font color='green'>Correct!</font><br>Reason: " + question.getReason() + "</html>",
                    "Result", JOptionPane.INFORMATION_MESSAGE);
            correctAnswers++;
        } else {
            JOptionPane.showMessageDialog(this,
                    "<html><font color='red'>Incorrect!</font><br>The correct answer was: " + "<font color='green'>"
                            + question.getAnswer() + ") "
                            + extractAnswerText(question.getAnswer(), question.getChoices()) + "</font><br>Reason: "
                            + question.getReason() + "</html>",
                    "Result", JOptionPane.ERROR_MESSAGE);
            incorrectQuestions.add(question); // Add to incorrect list
        }

        // Stop explanation audio after dialog is closed
        AudioPlayer.stopCurrentAudio();
    }

    private void nextQuestion() {
        timer.stop();
        currentQuestionIndex++;
        selectedButton = null;
        resetButtonColors();
        loadQuestion();
        startTimer();
    }

    private void resetButtonColors() {
        for (JButton btn : choiceButtons) {
            btn.setBackground(Color.GRAY);
            btn.setForeground(Color.WHITE);
        }
    }
    
    private void showResults() {
         timer.stop(); // Stop the timer

        double percentage = (double) correctAnswers / questions.size() * 100;
        updateHistory(quizFileName, percentage); // Update history after quiz completion
        saveHistoryToFile(); // Save the updated history to the file
        StringBuilder review = new StringBuilder("<html>Quiz Complete!<br><font color='green'>Correct Answers: "
                + correctAnswers + "</font><br>Your score: " + String.format("%.2f", percentage) + "%</html>");
                
        JOptionPane.showMessageDialog(this, review.toString(), "Results", JOptionPane.INFORMATION_MESSAGE);
        dispose(); // Close the quiz window
        showPerformanceHistory(quizFileName); // Show history in a new window

        if (!incorrectQuestions.isEmpty()) {
            int retry = JOptionPane.showConfirmDialog(this, "Would you like to retry the incorrect questions?", "Retry",
                    JOptionPane.YES_NO_OPTION);
            if (retry == JOptionPane.YES_OPTION) {
                questions = new ArrayList<>(incorrectQuestions); // Restart quiz with incorrect questions
                incorrectQuestions.clear(); // Clear incorrect list for this session
                currentQuestionIndex = 0;
                correctAnswers = 0;
                loadQuestion();
                startTimer();
                return;
            }
        }

        dispose(); // Close the quiz window
        timer.stop();
        showReviewWindow(); // Show the review window
    }

    private void updateHistory(String quizName, double percentage) {
        quizHistory.putIfAbsent(quizName, new LinkedList<>());
        Queue<Double> history = quizHistory.get(quizName);
        if (history.size() == 5) {
            history.poll(); // Remove oldest entry to maintain size limit
        }
        history.offer(percentage);
    }

    private void showPerformanceHistory(String quizName) {
        JFrame historyFrame = new JFrame("Performance History for " + quizName);
        historyFrame.setSize(400, 300);
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("<html><h2>Performance History:</h2></html>");
        historyPanel.add(titleLabel);

        Queue<Double> history = quizHistory.get(quizName);
        if (history == null || history.isEmpty()) {
            JLabel noHistoryLabel = new JLabel("No performance history available.");
            historyPanel.add(noHistoryLabel);
        } else {
            Double prev = null;
            for (Double score : history) {
                String record = String.format("%.2f%%", score);
                if (prev != null) {
                    double change = ((score - prev) / prev) * 100;
                    record += String.format(" (%.2f%% %s)", Math.abs(change), change >= 0 ? "improvement" : "decline");
                }
                prev = score;
                historyPanel.add(new JLabel(record));
            }
        }

        JScrollPane scrollPane = new JScrollPane(historyPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyFrame.add(scrollPane);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> historyFrame.dispose());
        historyFrame.add(closeButton, BorderLayout.SOUTH);

        historyFrame.setVisible(true);
    }

    private void saveHistoryToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(quizHistory);
        } catch (IOException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

@SuppressWarnings("unchecked")
private void loadHistoryFromFile() {
    File file = new File(HISTORY_FILE);
    if (!file.exists()) {
        try {
            file.createNewFile(); // Create the file if it doesn't exist
            System.out.println("History file created: " + HISTORY_FILE);
        } catch (IOException e) {
            System.err.println("Error creating history file: " + e.getMessage());
        }
    }

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
        Object data = ois.readObject();
        if (data instanceof Map) {
            quizHistory.putAll((Map<String, Queue<Double>>) data);
        }
    } catch (IOException | ClassNotFoundException e) {
        System.err.println("Error loading history: " + e.getMessage());
    }
}

    private class NextButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkAnswer();
            nextQuestion();
        }
    }

    static ArrayList<MCQ> loadQuestionsFromFile(String fileName, File baseDirectory) throws IOException {
        ArrayList<MCQ> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            String question = null;
            String[] choices = new String[4];
            String answer = null;
            String reason = null;
            int choiceIndex = 0;
            int questionNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Question")) {
                    question = reader.readLine().trim();
                    choiceIndex = 0;
                    questionNumber++;
                } else if (line.startsWith("A)") || line.startsWith("B)") || line.startsWith("C)") || line.startsWith("D)")) {
                    choices[choiceIndex++] = line.trim();
                } else if (line.startsWith("Answer:")) {
                    answer = line.split(":")[1].trim().substring(0, 1); // Extract only the letter
                } else if (line.startsWith("Explanation:")) {
                    reason = line.split(":")[1].trim();
                    
                    // Dynamically resolve paths relative to the base directory
                    File questionAudioFile = new File(baseDirectory, "audio/complete/question" + questionNumber + "/question " + questionNumber + ".wav");
                    File explanationAudioFile = new File(baseDirectory, "audio/complete/question" + questionNumber + "/question " + questionNumber + "(explanation).wav");
                    
                    // Add MCQ object
                    questions.add(new MCQ(
                            question,
                            choices.clone(),
                            answer,
                            reason,
                            questionAudioFile.getAbsolutePath(),
                            explanationAudioFile.getAbsolutePath()
                    ));
                }
            }
        }
        return questions;
    }
    

    public static void shuffleQuestions(ArrayList<MCQ> questions) {
        Collections.shuffle(questions, new Random(System.nanoTime())); // Use nanoTime for better randomization
    }

    String extractAnswerText(String letter, String[] choices) {
        return switch (letter) {
            case "A" -> choices[0].substring(3);
            case "B" -> choices[1].substring(3);
            case "C" -> choices[2].substring(3);
            case "D" -> choices[3].substring(3);
            default -> "";
        }; // Remove "A) " prefix
    }

    private void showReviewWindow() {
        timer.stop();
        JFrame reviewFrame = new JFrame("Quiz Review");
        reviewFrame.setSize(800, 600);
        reviewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Allow the user to close only the review window
        reviewFrame.setLayout(new BorderLayout());

        JPanel reviewPanel = new JPanel();
        reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
        reviewPanel.setBackground(Color.DARK_GRAY);

        for (int i = 0; i < questions.size(); i++) {
            MCQ question = questions.get(i);

            // Question text
            JLabel questionLabel = new JLabel(
                    "<html><b>Question " + (i + 1) + ":</b> " + question.getQuestion() + "</html>");
            questionLabel.setForeground(Color.WHITE);

            // Correct answer text
            JLabel correctAnswerLabel = new JLabel(
                    "<html>Correct Answer: <font color='green'>" + question.getAnswer() + ") "
                            + extractAnswerText(question.getAnswer(), question.getChoices()) + "</font></html>");
            correctAnswerLabel.setForeground(Color.GREEN);

            // User's answer
            String userAnswerText = extractAnswerText(userAnswers.get(i), question.getChoices());
            JLabel userAnswerLabel;
            if (incorrectQuestions.contains(question)) {
                userAnswerLabel = new JLabel("<html>Your Answer: <font color='red'>" + userAnswers.get(i) + ") "
                        + userAnswerText + "</font></html>");
                userAnswerLabel.setForeground(Color.RED);
            } else {
                userAnswerLabel = new JLabel("<html>Your Answer: <font color='green'>" + userAnswers.get(i) + ") "
                        + userAnswerText + "</font></html>");
                userAnswerLabel.setForeground(Color.GREEN);
            }

            // Explanation or reason
            JLabel explanationLabel = new JLabel("<html><b>Explanation:</b> " + question.getReason() + "</html>");
            explanationLabel.setForeground(Color.YELLOW);

            // Add components to the panel
            JPanel questionPanel = new JPanel();
            questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
            questionPanel.setBackground(Color.DARK_GRAY);
            questionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            questionPanel.add(questionLabel);
            questionPanel.add(correctAnswerLabel);
            questionPanel.add(userAnswerLabel);
            questionPanel.add(explanationLabel);

            reviewPanel.add(questionPanel);
        }

        JScrollPane scrollPane = new JScrollPane(reviewPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        reviewFrame.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> reviewFrame.dispose());
        reviewFrame.add(closeButton, BorderLayout.SOUTH);

        reviewFrame.setVisible(true);
    }

    public static void main(String[] args) {
        String[] options = { "15 seconds", "30 seconds", "45 seconds", "60 seconds" };
        int timeSelection = JOptionPane.showOptionDialog(null, "Select time per question:", "Select Time",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        String[] fontOptions = { "12", "18", "24", "30", "36", "46" };
        int fontSelection = JOptionPane.showOptionDialog(null, "Select Font Size:", "Select Font Size",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, fontOptions, fontOptions[1]);

        int timeBetweenQuestions = 15;
        switch (timeSelection) {
            case 0 -> timeBetweenQuestions = 15;
            case 1 -> timeBetweenQuestions = 30;
            case 2 -> timeBetweenQuestions = 45;
            case 3 -> timeBetweenQuestions = 60;
        }

        int fontSize = 18;
        switch (fontSelection) {
            case 0 -> fontSize = 12;
            case 1 -> fontSize = 18;
            case 2 -> fontSize = 24;
            case 3 -> fontSize = 30;
            case 4 -> fontSize = 36;
            case 5 -> fontSize = 46;
        }

        JFileChooser fileChooser = new JFileChooser(Paths.get("").toAbsolutePath().toString());
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle("Select Quiz Files");


        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            File selectedFile = files[0]; 
            String quizFileName = selectedFile.getName();
        
            // Get the directory of the first selected file
            File baseDirectory = files[0].getParentFile();
        
            ArrayList<MCQ> questions = new ArrayList<>();
            for (File file : files) {
                try {
                    questions.addAll(loadQuestionsFromFile(file.getAbsolutePath(), baseDirectory));
                    
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Error reading file: " + file.getName(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        
            MCQ.shuffleQuestions(questions);
            Main app = new Main(questions, fontSize, timeBetweenQuestions,quizFileName);
            app.setVisible(true);
        }
    }
}
