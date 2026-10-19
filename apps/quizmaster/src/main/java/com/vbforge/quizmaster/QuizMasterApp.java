package com.vbforge.quizmaster;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizMasterApp extends Application {

    private Stage stage;
    private Db db;
    private QuestionDao dao;
    
    private double xOffset = 0;
    private double yOffset = 0;

    // Game State
    private List<Question> currentQuestions;
    private int currentIndex = 0;
    private int score = 0;
    private AnimationTimer timer;
    private int currentShuffledCorrectIndex = 0;
    
    // History for Retrospective
    private record QuestionResult(Question question, List<String> shuffledOptions, int selectedIndex, int correctShuffledIndex, boolean isCorrect, boolean timedOut) {}
    private final List<QuestionResult> history = new ArrayList<>();

    // Screens
    private StackPane root;
    private VBox startScreen;
    private VBox quizScreen;
    private VBox resultScreen;

    // UI Refs
    private ComboBox<String> categoryCombo;
    private Label qProgressLabel, qScoreLabel, qTextLabel;
    private ProgressBar timeProgress;
    private Button[] optionButtons = new Button[4];
    private Label finalScoreLabel, finalTextLabel;
    private ListView<QuestionResult> reviewList;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);

        db = new Db();
        dao = new QuestionDao(db);

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root-pane");
        mainLayout.setTop(buildTitleBar());

        root = new StackPane();
        startScreen = buildStartScreen();
        quizScreen = buildQuizScreen();
        resultScreen = buildResultScreen();
        
        root.getChildren().addAll(startScreen, quizScreen, resultScreen);
        mainLayout.setCenter(root);

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/quizmaster.css").toExternalForm());

        stage.setTitle("QuizMaster");
        stage.setScene(scene);
        stage.show();
        
        showScreen(startScreen);
    }

    private Node buildTitleBar() {
        Label title = new Label("QuizMaster");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - xOffset); stage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> stage.close());

        HBox bar = new HBox(titleArea, closeBtn);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    // --- SCREENS ---

    private VBox buildStartScreen() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");

        Label logo = new Label("QuizMaster");
        logo.getStyleClass().add("logo");
        
        Label subtitle = new Label("Test your knowledge");
        subtitle.getStyleClass().add("subtitle");

        categoryCombo = new ComboBox<>(FXCollections.observableArrayList(dao.getCategories()));
        categoryCombo.getSelectionModel().selectFirst();
        categoryCombo.setPrefWidth(300);

        Button importBtn = new Button("+ Import Custom Quiz (JSON)");
        importBtn.getStyleClass().add("btn");
        importBtn.setPrefWidth(300);
        importBtn.setOnAction(e -> importJson());

        Button startBtn = new Button("Start Quiz");
        startBtn.getStyleClass().addAll("btn", "primary");
        startBtn.setPrefWidth(300);
        startBtn.setOnAction(e -> startQuiz());

        box.getChildren().addAll(logo, subtitle, categoryCombo, importBtn, startBtn);
        return box;
    }

    private VBox buildQuizScreen() {
        VBox box = new VBox(20);
        box.getStyleClass().add("screen");

        HBox header = new HBox();
        qProgressLabel = new Label("Question 1 of 10");
        qProgressLabel.setStyle("-fx-text-fill: #f89820; -fx-font-weight: bold; -fx-font-family: monospace; -fx-font-size: 14px;");
        
        qScoreLabel = new Label("Score: 0");
        qScoreLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold; -fx-font-family: monospace; -fx-font-size: 14px;");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(qProgressLabel, sp, qScoreLabel);

        timeProgress = new ProgressBar(1.0);
        timeProgress.setMaxWidth(Double.MAX_VALUE);
        timeProgress.getStyleClass().add("timer-bar");

        qTextLabel = new Label("Question text");
        qTextLabel.getStyleClass().add("question-text");
        qTextLabel.setWrapText(true);
        qTextLabel.setAlignment(Pos.CENTER);

        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(15);
        optionsGrid.setVgap(15);
        optionsGrid.setAlignment(Pos.CENTER);

        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            Button btn = new Button();
            btn.getStyleClass().add("option-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(80);
            
            HBox content = new HBox(15, new Label(letters[i]), new Label());
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().get(0).getStyleClass().add("option-letter");
            
            btn.setGraphic(content);
            final int index = i;
            btn.setOnAction(e -> checkAnswer(index));
            
            optionButtons[i] = btn;
            optionsGrid.add(btn, i % 2, i / 2);
        }
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        optionsGrid.getColumnConstraints().addAll(col1, col2);

        VBox.setVgrow(optionsGrid, Priority.ALWAYS);
        box.getChildren().addAll(header, timeProgress, qTextLabel, optionsGrid);
        return box;
    }

    private VBox buildResultScreen() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        box.setPadding(new Insets(30));

        finalScoreLabel = new Label("0%");
        finalScoreLabel.getStyleClass().add("score-circle");
        
        Label title = new Label("Quiz Complete!");
        title.getStyleClass().add("results-title");
        
        finalTextLabel = new Label("You got 0 out of 0 questions right.");
        finalTextLabel.getStyleClass().add("results-sub");

        Button restartBtn = new Button("Try Again");
        restartBtn.getStyleClass().addAll("btn", "primary");
        restartBtn.setOnAction(e -> showScreen(startScreen));

        reviewList = new ListView<>();
        reviewList.getStyleClass().add("review-list");
        reviewList.setPrefHeight(200);
        reviewList.setMaxWidth(Double.MAX_VALUE);
        
        reviewList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(QuestionResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    VBox vbox = new VBox(5);
                    vbox.setPadding(new Insets(8, 12, 8, 12));

                    Label qText = new Label(item.question().text());
                    qText.setStyle("-fx-font-weight: bold; -fx-text-fill: #e6edf3;");
                    qText.setWrapText(true);

                    String userAns = (item.selectedIndex() == -1) ? "Timed Out" : item.shuffledOptions().get(item.selectedIndex());
                    String correctAns = item.shuffledOptions().get(item.correctShuffledIndex());

                    Label ansText = new Label();
                    if (item.isCorrect()) {
                        ansText.setText("Correct: " + userAns);
                        ansText.setStyle("-fx-text-fill: #3fb950;");
                        vbox.setStyle("-fx-background-color: rgba(63, 185, 80, 0.1); -fx-background-radius: 6;");
                    } else {
                        ansText.setText("Your answer: " + userAns + "  |  Correct: " + correctAns);
                        ansText.setStyle("-fx-text-fill: #f85149;");
                        vbox.setStyle("-fx-background-color: rgba(248, 81, 73, 0.1); -fx-background-radius: 6;");
                    }

                    vbox.getChildren().addAll(qText, ansText);
                    setGraphic(vbox);
                    setText(null);
                }
            }
        });

        VBox scoreArea = new VBox(10, finalScoreLabel, title, finalTextLabel);
        scoreArea.setAlignment(Pos.CENTER);

        VBox reviewArea = new VBox(10, new Label("Retrospective:"), reviewList);
        VBox.setVgrow(reviewList, Priority.ALWAYS);
        reviewArea.getChildren().get(0).setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
        
        HBox btnArea = new HBox(restartBtn);
        btnArea.setAlignment(Pos.CENTER);

        box.getChildren().addAll(scoreArea, reviewArea, btnArea);
        return box;
    }

    // --- LOGIC ---

    private void showScreen(Node screen) {
        startScreen.setVisible(false);
        quizScreen.setVisible(false);
        resultScreen.setVisible(false);
        screen.setVisible(true);
    }

    private void startQuiz() {
        String category = categoryCombo.getValue();
        if (category == null) return;

        currentQuestions = dao.getQuestionsByCategory(category);
        Collections.shuffle(currentQuestions);
        
        if (currentQuestions.isEmpty()) return;

        currentIndex = 0;
        score = 0;
        history.clear();
        showScreen(quizScreen);
        loadQuestion();
    }

    private void loadQuestion() {
        if (currentIndex >= currentQuestions.size()) {
            endQuiz();
            return;
        }

        Question q = currentQuestions.get(currentIndex);
        qProgressLabel.setText("Question " + (currentIndex + 1) + " of " + currentQuestions.size());
        qScoreLabel.setText("Score: " + score);
        qTextLabel.setText(q.text());

        List<String> opts = new ArrayList<>(q.options());
        String correctStr = opts.get(q.correctIndex());
        Collections.shuffle(opts);
        currentShuffledCorrectIndex = opts.indexOf(correctStr);

        for (int i = 0; i < 4; i++) {
            HBox content = (HBox) optionButtons[i].getGraphic();
            Label textLbl = (Label) content.getChildren().get(1);
            textLbl.setText(opts.get(i));
            
            optionButtons[i].getStyleClass().removeAll("correct", "wrong");
            optionButtons[i].setDisable(false);
        }

        startTimer();
    }

    private void startTimer() {
        if (timer != null) timer.stop();
        timeProgress.setProgress(1.0);

        final long startTime = System.nanoTime();
        final long durationNs = 15_000_000_000L; // 15 seconds

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - startTime;
                if (elapsed >= durationNs) {
                    timeProgress.setProgress(0);
                    this.stop();
                    checkAnswer(-1); // Time out
                } else {
                    timeProgress.setProgress(1.0 - ((double) elapsed / durationNs));
                }
            }
        };
        timer.start();
    }

    private void checkAnswer(int selectedIndex) {
        if (timer != null) timer.stop();
        for (Button b : optionButtons) b.setDisable(true);

        boolean isCorrect = (selectedIndex == currentShuffledCorrectIndex);
        
        Question q = currentQuestions.get(currentIndex);
        List<String> currentOpts = new ArrayList<>();
        for (Button b : optionButtons) {
            currentOpts.add(((Label)((HBox)b.getGraphic()).getChildren().get(1)).getText());
        }

        // Save the correct shuffled index so we can show it later
        history.add(new QuestionResult(q, currentOpts, selectedIndex, currentShuffledCorrectIndex, isCorrect, selectedIndex == -1));

        if (isCorrect) {
            score++;
            qScoreLabel.setText("Score: " + score);
            optionButtons[selectedIndex].getStyleClass().add("correct");
        } else {
            if (selectedIndex != -1) {
                optionButtons[selectedIndex].getStyleClass().add("wrong");
            }
            optionButtons[currentShuffledCorrectIndex].getStyleClass().add("correct");
        }

        Timeline pause = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            currentIndex++;
            loadQuestion();
        }));
        pause.play();
    }

    private void endQuiz() {
        showScreen(resultScreen);
        int total = currentQuestions.size();
        int pct = (int) (((double) score / total) * 100);
        
        finalScoreLabel.setText(pct + "%");
        finalTextLabel.setText("You got " + score + " out of " + total + " questions right.");
        
        reviewList.setItems(FXCollections.observableArrayList(history));
    }

    private void importJson() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fc.showOpenDialog(stage);

        if (file != null) {
            try {
                List<Question> imported = JsonImporter.importFromFile(file);

                // Get the count of newly added questions
                int addedCount = dao.addQuestions(imported);
                int skippedCount = imported.size() - addedCount;

                categoryCombo.setItems(FXCollections.observableArrayList(dao.getCategories()));
                categoryCombo.getSelectionModel().select(imported.get(0).category());

                String msg = "Imported " + addedCount + " new questions!";
                if (skippedCount > 0) {
                    msg += "\n(" + skippedCount + " duplicates were skipped)";
                }

                Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
                a.show();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to import: " + ex.getMessage());
                a.show();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}