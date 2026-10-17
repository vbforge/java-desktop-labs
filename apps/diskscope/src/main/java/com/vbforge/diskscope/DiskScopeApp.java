package com.vbforge.diskscope;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class DiskScopeApp extends Application {

    private Path currentPath;
    private ScanTask currentTask;

    private Label breadcrumbLabel;
    private PieChart pieChart;
    private VBox legendBox;
    private Label totalSizeLabel;
    private Label statusLabel;
    private Label fileCountLabel;
    private StackPane chartStack;
    private VBox loadingBox;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildToolbar());
        root.setCenter(buildContent());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1000, 720);
        scene.getStylesheets().add(
                DiskScopeApp.class.getResource("/diskscope.css").toExternalForm());

        stage.setTitle("DiskScope");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> {
            if (currentTask != null) currentTask.cancel();
        });
        stage.show();
    }

    private Node buildToolbar() {
        Button selectBtn = new Button("Select Folder");
        selectBtn.getStyleClass().add("btn-primary");
        selectBtn.setOnAction(e -> chooseFolder());

        Button upBtn = new Button("↑ Up");
        upBtn.getStyleClass().add("btn");
        upBtn.setOnAction(e -> navigateUp());

        breadcrumbLabel = new Label("No folder selected");
        breadcrumbLabel.getStyleClass().add("breadcrumb");
        breadcrumbLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(breadcrumbLabel, Priority.ALWAYS);

        HBox toolbar = new HBox(16, selectBtn, upBtn, breadcrumbLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        return toolbar;
    }

    private Node buildContent() {
        pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.setAnimated(false);

        StackPane pieWrapper = new StackPane(pieChart);
        pieWrapper.setMaxSize(600, 600); 

        Circle hole = new Circle();
        hole.setFill(Color.web("#161b22"));
        hole.radiusProperty().bind(pieWrapper.widthProperty().multiply(0.22));

        totalSizeLabel = new Label("—");
        totalSizeLabel.getStyleClass().add("center-total");
        Label centerLbl = new Label("Total Size");
        centerLbl.getStyleClass().add("center-label");
        VBox centerBox = new VBox(4, totalSizeLabel, centerLbl);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);

        StackPane chartInner = new StackPane(pieWrapper, hole, centerBox);
        
        ProgressIndicator loader = new ProgressIndicator();
        loader.setMaxSize(40, 40);
        Label loadingLbl = new Label("SCANNING FOLDER...");
        loadingLbl.getStyleClass().add("loading-text");
        loadingBox = new VBox(16, loader, loadingLbl);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-background-color: rgba(13,17,23,0.8);");
        loadingBox.setVisible(false);

        chartStack = new StackPane(chartInner, loadingBox);
        chartStack.getStyleClass().add("chart-area");

        legendBox = new VBox(8);
        legendBox.getStyleClass().add("legend-list");

        // Apply specific class to remove ScrollPane white borders
        ScrollPane legendScroll = new ScrollPane(legendBox);
        legendScroll.setFitToWidth(true);
        legendScroll.getStyleClass().add("legend-scroll");
        legendScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Label legendHeader = new Label("LARGEST ITEMS");
        legendHeader.getStyleClass().add("legend-header");

        VBox rightPanel = new VBox(16, legendHeader, legendScroll);
        VBox.setVgrow(legendScroll, Priority.ALWAYS);
        rightPanel.getStyleClass().add("legend-panel");
        rightPanel.setPrefWidth(320);
        rightPanel.setMinWidth(280);

        HBox content = new HBox(chartStack, rightPanel);
        HBox.setHgrow(chartStack, Priority.ALWAYS);
        return content;
    }

    private Node buildStatusBar() {
        fileCountLabel = new Label("Ready");
        fileCountLabel.getStyleClass().add("status-hint");

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-ok");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox status = new HBox(10, fileCountLabel, spacer, statusLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        status.getStyleClass().add("status-bar");
        return status;
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a folder to analyze");
        if (currentPath != null) {
            chooser.setInitialDirectory(currentPath.toFile());
        }

        File file = chooser.showDialog(null);
        if (file != null) {
            loadDirectory(file.toPath());
        }
    }

    private void navigateUp() {
        if (currentPath != null && currentPath.getParent() != null) {
            loadDirectory(currentPath.getParent());
        }
    }

    private void loadDirectory(Path path) {
        if (currentTask != null) {
            currentTask.cancel();
        }

        currentPath = path;
        updateBreadcrumb(path);
        
        loadingBox.setVisible(true);
        legendBox.getChildren().clear();
        pieChart.getData().clear();
        totalSizeLabel.setText("—");
        fileCountLabel.setText("Scanning...");
        statusLabel.setText("Scanning...");
        statusLabel.getStyleClass().setAll("status-hint");

        currentTask = new ScanTask(path);
        
        currentTask.setOnSucceeded(e -> {
            loadingBox.setVisible(false);
            DirectoryScanner.ScanResult result = currentTask.getValue();
            updateUI(result);
        });

        currentTask.setOnFailed(e -> {
            loadingBox.setVisible(false);
            statusLabel.setText("Error: " + currentTask.getException().getMessage());
            statusLabel.getStyleClass().setAll("status-err");
        });

        Thread t = new Thread(currentTask);
        t.setDaemon(true);
        t.start();
    }

    private void updateUI(DirectoryScanner.ScanResult result) {
        List<DiskItem> items = result.items();
        
        if (items.isEmpty()) {
            statusLabel.setText("Folder is empty or unreadable");
            fileCountLabel.setText("0 files · 0 folders");
            return;
        }

        long totalSize = items.stream().mapToLong(DiskItem::size).sum();
        totalSizeLabel.setText(formatSize(totalSize));

        legendBox.getChildren().clear();
        pieChart.getData().clear();

        for (DiskItem item : items) {
            PieChart.Data data = new PieChart.Data(item.name(), item.size());
            pieChart.getData().add(data);

            Circle colorDot = new Circle(5);
            colorDot.setFill(Color.web(item.colorHex()));
            
            Label name = new Label(item.name());
            name.getStyleClass().add("legend-name");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            
            Label size = new Label(item.formattedSize());
            size.getStyleClass().add("legend-size");

            HBox row = new HBox(10, colorDot, name, size);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("legend-item");

            if (item.isDirectory() && item.path() != null) {
                row.setOnMouseClicked(e -> loadDirectory(item.path()));
                row.setCursor(javafx.scene.Cursor.HAND);
            } else {
                row.setOpacity(0.8);
            }

            legendBox.getChildren().add(row);
        }

        Platform.runLater(() -> {
            for (int i = 0; i < items.size(); i++) {
                DiskItem item = items.get(i);
                PieChart.Data data = pieChart.getData().get(i);
                if (data.getNode() != null) {
                    String style = "-fx-pie-color: " + item.colorHex() + "; "
                                 + "-fx-background-color: " + item.colorHex() + "; "
                                 + "-fx-border-color: transparent;";
                    data.getNode().setStyle(style);
                    
                    if (item.isDirectory() && item.path() != null) {
                        data.getNode().setOnMouseClicked(e -> loadDirectory(item.path()));
                        data.getNode().setCursor(javafx.scene.Cursor.HAND);
                    }
                }
            }
        });

        fileCountLabel.setText(String.format("%,d files · %,d folders", result.totalFiles(), result.totalFolders()));
        statusLabel.setText("Scan complete");
        statusLabel.getStyleClass().setAll("status-ok");
    }

    private void updateBreadcrumb(Path path) {
        String full = path.toString();
        String home = System.getProperty("user.home");
        if (full.startsWith(home)) {
            full = "~" + full.substring(home.length());
        }
        breadcrumbLabel.setText(full);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static void main(String[] args) {
        launch(args);
    }
}