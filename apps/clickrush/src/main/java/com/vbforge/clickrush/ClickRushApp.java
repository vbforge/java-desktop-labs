package com.vbforge.clickrush;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ClickRushApp extends Application {

    private final Db db = new Db();
    private final ScoreDao dao = new ScoreDao(db);

    private Label pbReactionLabel;
    private Label pbSpeedLabel;

    private ReactionGame reactionGame;
    private SpeedGame speedGame;
    private StackPane gameContainer;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setLeft(buildSidebar());
        root.setCenter(buildMainArea());

        // Load PBs
        refreshPBs();

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/clickrush.css").toExternalForm());

        stage.setTitle("ClickRush");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> db.close());
        stage.show();
        
        switchMode("reaction");
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);

        Label brand = new Label("ClickRush");
        brand.getStyleClass().add("brand");
        VBox.setMargin(brand, new Insets(0, 0, 24, 0));

        Button navReaction = new Button("Reaction Time");
        navReaction.getStyleClass().add("nav-btn");
        navReaction.setMaxWidth(Double.MAX_VALUE);
        navReaction.setOnAction(e -> switchMode("reaction"));

        Button navSpeed = new Button("Click Speed");
        navSpeed.getStyleClass().add("nav-btn");
        navSpeed.setMaxWidth(Double.MAX_VALUE);
        navSpeed.setOnAction(e -> switchMode("speed"));

        // Stats Box
        pbReactionLabel = new Label("-- ms");
        pbReactionLabel.getStyleClass().add("stat-value");
        
        pbSpeedLabel = new Label("-- CPS");
        pbSpeedLabel.getStyleClass().add("stat-value");

        HBox rRow = new HBox(new Label("Reaction"), new Region(), pbReactionLabel);
        HBox.setHgrow(rRow.getChildren().get(1), Priority.ALWAYS);
        rRow.getStyleClass().add("stat-row");
        ((Label)rRow.getChildren().get(0)).getStyleClass().add("stat-label");

        HBox sRow = new HBox(new Label("Speed"), new Region(), pbSpeedLabel);
        HBox.setHgrow(sRow.getChildren().get(1), Priority.ALWAYS);
        sRow.getStyleClass().add("stat-row");
        ((Label)sRow.getChildren().get(0)).getStyleClass().add("stat-label");

        Label statsTitle = new Label("PERSONAL BESTS");
        statsTitle.getStyleClass().add("stats-title");

        VBox statsBox = new VBox(8, statsTitle, rRow, sRow);
        statsBox.getStyleClass().add("stats-box");
        VBox.setVgrow(statsBox, Priority.ALWAYS);
        statsBox.setAlignment(Pos.BOTTOM_LEFT);

        sidebar.getChildren().addAll(brand, navReaction, navSpeed, statsBox);
        return sidebar;
    }

    private Node buildMainArea() {
        gameContainer = new StackPane();
        
        reactionGame = new ReactionGame(score -> {
            double best = dao.getBest("reaction", 999999.0);
            if (score < best) {
                dao.saveBest("reaction", score);
                refreshPBs();
            }
        });

        speedGame = new SpeedGame(score -> {
            double best = dao.getBest("speed", 0.0);
            if (score > best) {
                dao.saveBest("speed", score);
                refreshPBs();
            }
        });

        gameContainer.getChildren().addAll(reactionGame, speedGame);
        return gameContainer;
    }

    private void switchMode(String mode) {
        // Reset games when switching to prevent background states
        reactionGame.reset();
        speedGame.reset();

        reactionGame.setVisible(mode.equals("reaction"));
        speedGame.setVisible(mode.equals("speed"));
    }

    private void refreshPBs() {
        double r = dao.getBest("reaction", -1);
        pbReactionLabel.setText(r == -1 ? "-- ms" : (int)r + " ms");
        if (r != -1) pbReactionLabel.setStyle("-fx-text-fill: #3fb950;");

        double s = dao.getBest("speed", -1);
        pbSpeedLabel.setText(s == -1 ? "-- CPS" : String.format("%.1f CPS", s));
        if (s != -1) pbSpeedLabel.setStyle("-fx-text-fill: #3fb950;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}