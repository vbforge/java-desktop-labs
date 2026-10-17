package com.vbforge.audioflow;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class AudioFlowApp extends Application {

    private MediaPlayer mediaPlayer;
    private Stage primaryStage;

    private final ObservableList<Track> playlist = FXCollections.observableArrayList();
    private ListView<Track> playlistView;
    
    // Now Playing UI
    private Label trackTitleLabel;
    private Label trackAlbumLabel;
    private Label statusLabel;

    // Controls
    private Button playBtn;
    private Slider seekSlider;
    private Slider volumeSlider;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private boolean isSeeking = false;

    // Window dragging
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED); // Remove native OS title bar

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildTitleBar(stage));
        root.setLeft(buildPlaylist());
        root.setCenter(buildNowPlaying());
        root.setBottom(buildControls());

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/audioflow.css").toExternalForm());

        stage.setTitle("AudioFlow");
        stage.setScene(scene);
        stage.show();
    }

    private Node buildTitleBar(Stage stage) {
        Label title = new Label("AudioFlow");
        title.getStyleClass().add("app-title");

        // Make the title area draggable
        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 4));
        titleArea.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        titleArea.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Music");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openFileChooser());

        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("win-btn");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> {
            if (mediaPlayer != null) mediaPlayer.dispose();
            stage.close();
        });

        HBox header = new HBox(10, titleArea, addBtn, minBtn, closeBtn);
        header.setAlignment(Pos.CENTER_RIGHT);
        header.getStyleClass().add("header");
        return header;
    }

    private Node buildPlaylist() {
        playlistView = new ListView<>(playlist);
        playlistView.getStyleClass().add("playlist");
        playlistView.setPrefWidth(320);
        
        playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.title());
                }
            }
        });

        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Track selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected != null) loadMedia(selected);
            }
        });

        Label header = new Label("PLAYLIST");
        header.getStyleClass().add("section-title");

        VBox box = new VBox(10, header, playlistView);
        VBox.setVgrow(playlistView, Priority.ALWAYS);
        box.getStyleClass().add("sidebar");
        return box;
    }

    private Node buildNowPlaying() {
        StackPane albumArt = new StackPane();
        albumArt.getStyleClass().add("album-art");
        Label musicIcon = new Label("🎵");
        musicIcon.getStyleClass().add("music-icon");
        albumArt.getChildren().add(musicIcon);
        albumArt.setMaxSize(240, 240);

        trackTitleLabel = new Label("No track selected");
        trackTitleLabel.getStyleClass().add("track-title");

        trackAlbumLabel = new Label("Select a file to begin");
        trackAlbumLabel.getStyleClass().add("track-album");

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        VBox center = new VBox(20, albumArt, trackTitleLabel, trackAlbumLabel, statusLabel);
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("now-playing");
        return center;
    }

    private Node buildControls() {
        VBox controls = new VBox(15);
        controls.getStyleClass().add("controls");

        HBox seekRow = new HBox(10);
        seekRow.setAlignment(Pos.CENTER);
        
        currentTimeLabel = new Label("00:00");
        currentTimeLabel.getStyleClass().add("time-label");
        totalTimeLabel = new Label("00:00");
        totalTimeLabel.getStyleClass().add("time-label");
        
        seekSlider = new Slider(0, 100, 0);
        HBox.setHgrow(seekSlider, Priority.ALWAYS);
        
        seekSlider.setOnMousePressed(e -> isSeeking = true);
        seekSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(seekSlider.getValue() / 100.0));
            }
            isSeeking = false;
        });
        
        seekRow.getChildren().addAll(currentTimeLabel, seekSlider, totalTimeLabel);

        HBox buttonRow = new HBox(20);
        buttonRow.setAlignment(Pos.CENTER);
        
        Button prevBtn = new Button("⏮");
        prevBtn.getStyleClass().add("ctrl-btn");
        prevBtn.setOnAction(e -> playRelative(-1));

        playBtn = new Button("▶");
        playBtn.getStyleClass().add("play-btn");
        playBtn.setOnAction(e -> togglePlay());

        Button stopBtn = new Button("■");
        stopBtn.getStyleClass().add("ctrl-btn");
        stopBtn.setOnAction(e -> stopPlayback());

        Button nextBtn = new Button("⏭");
        nextBtn.getStyleClass().add("ctrl-btn");
        nextBtn.setOnAction(e -> playRelative(1));

        buttonRow.getChildren().addAll(prevBtn, playBtn, stopBtn, nextBtn);

        HBox volumeRow = new HBox(10);
        volumeRow.setAlignment(Pos.CENTER_RIGHT);
        
        Label volIcon = new Label("🔊");
        volIcon.getStyleClass().add("time-label");
        
        volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(val.doubleValue());
        });

        volumeRow.getChildren().addAll(volIcon, volumeSlider);

        BorderPane bottomRow = new BorderPane();
        bottomRow.setLeft(new Region());
        bottomRow.setCenter(buttonRow);
        bottomRow.setRight(volumeRow);

        controls.getChildren().addAll(seekRow, bottomRow);
        return controls;
    }

    // --- Logic ---

    private void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
        List<File> files = fc.showOpenMultipleDialog(primaryStage);
        if (files != null) {
            for (File f : files) {
                playlist.add(new Track(f.toPath(), f.getName()));
            }
            if (playlistView.getSelectionModel().isEmpty() && !playlist.isEmpty()) {
                playlistView.getSelectionModel().selectFirst();
            }
        }
    }

    private void loadMedia(Track track) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(track.path().toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());

            trackTitleLabel.setText(track.title());
            trackAlbumLabel.setText("Local File");
            statusLabel.setText("Loading...");

            mediaPlayer.setOnReady(() -> {
                totalTimeLabel.setText(formatTime(mediaPlayer.getTotalDuration()));
                statusLabel.setText("Playing");
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!isSeeking && mediaPlayer.getTotalDuration().greaterThan(Duration.ZERO)) {
                    double progress = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100;
                    seekSlider.setValue(progress);
                    currentTimeLabel.setText(formatTime(newTime));
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                stopPlayback();
                playRelative(1); // Auto-play next
            });

            mediaPlayer.play();
            playBtn.setText("⏸");

        } catch (Exception e) {
            statusLabel.setText("Error loading file.");
            e.printStackTrace();
        }
    }

    private void togglePlay() {
        if (mediaPlayer == null) {
            Track selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) loadMedia(selected);
            return;
        }

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playBtn.setText("▶");
            statusLabel.setText("Paused");
        } else {
            mediaPlayer.play();
            playBtn.setText("⏸");
            statusLabel.setText("Playing");
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playBtn.setText("▶");
            seekSlider.setValue(0);
            currentTimeLabel.setText("00:00");
            statusLabel.setText("Stopped");
        }
    }

    private void playRelative(int offset) {
        if (playlist.isEmpty()) return;
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        int nextIdx = Math.max(0, Math.min(playlist.size() - 1, idx + offset));
        
        if (nextIdx != idx || offset == 0) {
            playlistView.getSelectionModel().select(nextIdx);
            loadMedia(playlist.get(nextIdx));
        }
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSecs = (int) duration.toSeconds();
        return String.format("%02d:%02d", totalSecs / 60, totalSecs % 60);
    }

    public static void main(String[] args) {
        launch(args);
    }
}