package com.vbforge.sortscope;

import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SortScopeApp extends Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private Pane canvasPane;
    private Label statTimeLabel, statComparisonsLabel, statMovesLabel;
    
    private ComboBox<String> algoCombo;
    private ComboBox<String> speedCombo;
    private ComboBox<String> theoryCombo;
    private Slider sizeSlider;
    private Label sizeLabel;
    
    private Button shuffleBtn, sortBtn, pauseBtn, readTheoryBtn;
    
    private CNode[] cnodes;
    private SequentialTransition activeTransition;
    private boolean isSorting = false;
    
    private Timeline statsTimer;
    private long startTime;
    private long accumulatedTime;
    
    private static final double PADDING = 40;
    private final Map<String, String> theoryMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        initTheoryData();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildVisualizer());

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/sortscope.css").toExternalForm());

        stage.setTitle("SortScope");
        stage.setScene(scene);
        stage.show();
        
        generateArray();
        
        canvasPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSorting) generateArray();
        });
        canvasPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSorting) generateArray();
        });
    }

    private void initTheoryData() {
        theoryMap.put("Bubble Sort", 
            "BUBBLE SORT\n\n" +
            "How it works:\n" +
            "Repeatedly steps through the list, compares adjacent elements and swaps them if they are in the wrong order. The pass through the list is repeated until the list is sorted.\n\n" +
            "Complexity:\n" +
            "• Time: O(n²) worst/average, O(n) best\n" +
            "• Space: O(1)\n\n" +
            "Fun Fact:\n" +
            "It's called 'Bubble Sort' because the largest elements 'bubble' to the top (end) of the array with each pass.");
            
        theoryMap.put("Selection Sort", 
            "SELECTION SORT\n\n" +
            "How it works:\n" +
            "Divides the input list into two parts: a sorted sublist and an unsorted sublist. It repeatedly selects the smallest element from the unsorted sublist and swaps it into the sorted sublist.\n\n" +
            "Complexity:\n" +
            "• Time: O(n²) for all cases\n" +
            "• Space: O(1)\n\n" +
            "Fun Fact:\n" +
            "It performs the minimum number of swaps possible (at most n-1), making it useful when write operations are expensive.");
            
        theoryMap.put("Insertion Sort", 
            "INSERTION SORT\n\n" +
            "How it works:\n" +
            "Builds the final sorted array one item at a time. It takes each element and inserts it into its correct position among the previously sorted elements, shifting larger elements to the right.\n\n" +
            "Complexity:\n" +
            "• Time: O(n²) worst/average, O(n) best\n" +
            "• Space: O(1)\n\n" +
            "Fun Fact:\n" +
            "It is highly efficient for small datasets and is often used as the base case in optimized Quick Sort or Merge Sort implementations.");
            
        theoryMap.put("Merge Sort", 
            "MERGE SORT\n\n" +
            "How it works:\n" +
            "A Divide and Conquer algorithm. It divides the input array into two halves, recursively calls itself for the two halves, and then merges the two sorted halves back together.\n\n" +
            "Complexity:\n" +
            "• Time: O(n log n) for all cases\n" +
            "• Space: O(n) (requires a temporary array)\n\n" +
            "Fun Fact:\n" +
            "It is a stable sort and highly parallelizable, making it ideal for sorting massive datasets (like external sorting).");
            
        theoryMap.put("Quick Sort", 
            "QUICK SORT\n\n" +
            "How it works:\n" +
            "A Divide and Conquer algorithm. It picks an element as a pivot and partitions the array around it (smaller elements left, larger elements right). It then recursively sorts the sub-arrays.\n\n" +
            "Complexity:\n" +
            "• Time: O(n log n) average, O(n²) worst\n" +
            "• Space: O(log n) due to recursion\n\n" +
            "Fun Fact:\n" +
            "Despite its O(n²) worst-case (which occurs on already sorted arrays with poor pivot choices), it is practically the fastest sorting algorithm for most real-world data.");
            
        theoryMap.put("Heap Sort", 
            "HEAP SORT\n\n" +
            "How it works:\n" +
            "First, it builds a Max Heap (a complete binary tree where the parent is larger than its children). Then, it repeatedly swaps the root (largest element) with the last element, reduces the heap size by 1, and heapifies the new root.\n\n" +
            "Complexity:\n" +
            "• Time: O(n log n) for all cases\n" +
            "• Space: O(1)\n\n" +
            "Fun Fact:\n" +
            "It combines the time complexity of Merge Sort with the space efficiency of Selection Sort, but it is not a stable sort.");
    }

    private Node buildTitleBar() {
        Label title = new Label("SortScope");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> primaryStage.close());

        HBox bar = new HBox(titleArea, closeBtn);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(280);
        sidebar.setPadding(new Insets(20));

        algoCombo = new ComboBox<>();
        algoCombo.getItems().addAll("Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort");
        algoCombo.getSelectionModel().selectFirst();
        algoCombo.setMaxWidth(Double.MAX_VALUE);
        algoCombo.getStyleClass().add("styled-combo");

        sizeSlider = new Slider(10, 100, 40);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(30);
        sizeSlider.setBlockIncrement(10);
        sizeSlider.valueProperty().addListener((obs, old, val) -> {
            if (!isSorting) generateArray();
        });
        sizeLabel = new Label("40");
        sizeLabel.getStyleClass().add("stat-value");
        sizeSlider.valueProperty().addListener((obs, old, val) -> sizeLabel.setText(String.valueOf(val.intValue())));

        HBox sizeBox = new HBox(10, sizeSlider, sizeLabel);
        HBox.setHgrow(sizeSlider, Priority.ALWAYS);
        sizeBox.setAlignment(Pos.CENTER_LEFT);

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("Slow", "Normal", "Fast", "Instant");
        speedCombo.getSelectionModel().select(1);
        speedCombo.setMaxWidth(Double.MAX_VALUE);
        speedCombo.getStyleClass().add("styled-combo");

        shuffleBtn = new Button("Shuffle");
        shuffleBtn.getStyleClass().add("btn");
        shuffleBtn.setMaxWidth(Double.MAX_VALUE);
        shuffleBtn.setOnAction(e -> generateArray());

        sortBtn = new Button("Sort");
        sortBtn.getStyleClass().add("btn-primary");
        sortBtn.setMaxWidth(Double.MAX_VALUE);
        sortBtn.setOnAction(e -> startSort());

        pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("btn");
        pauseBtn.setMaxWidth(Double.MAX_VALUE);
        pauseBtn.setDisable(true);
        pauseBtn.setOnAction(e -> togglePause());

        HBox actionRow = new HBox(10, shuffleBtn, sortBtn);
        HBox.setHgrow(shuffleBtn, Priority.ALWAYS);
        HBox.setHgrow(sortBtn, Priority.ALWAYS);

        statTimeLabel = new Label("0 ms");
        statTimeLabel.getStyleClass().add("stat-value");
        statComparisonsLabel = new Label("0");
        statComparisonsLabel.getStyleClass().add("stat-value");
        statMovesLabel = new Label("0");
        statMovesLabel.getStyleClass().add("stat-value");

        VBox statsBox = new VBox(12,
            createStatRow("Time", statTimeLabel),
            createStatRow("Comparisons", statComparisonsLabel),
            createStatRow("Moves", statMovesLabel)
        );
        statsBox.getStyleClass().add("stats-box");

        // THEORY SECTION
        theoryCombo = new ComboBox<>();
        theoryCombo.getItems().addAll("Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort");
        theoryCombo.getSelectionModel().selectFirst();
        theoryCombo.setMaxWidth(Double.MAX_VALUE);
        theoryCombo.getStyleClass().add("styled-combo");

        readTheoryBtn = new Button("Read Theory");
        readTheoryBtn.getStyleClass().add("btn");
        readTheoryBtn.setMaxWidth(Double.MAX_VALUE);
        readTheoryBtn.setOnAction(e -> showTheoryDialog());

        VBox theoryBox = new VBox(8, theoryCombo, readTheoryBtn);
        
        // Push the stats and theory to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(
            createLabel("ALGORITHM"), algoCombo,
            createLabel("ARRAY SIZE"), sizeBox,
            createLabel("ANIMATION SPEED"), speedCombo,
            actionRow, pauseBtn,
            spacer,
            statsBox,
            createLabel("THEORY"), theoryBox
        );

        return sidebar;
    }

    private Node buildVisualizer() {
        canvasPane = new Pane();
        canvasPane.getStyleClass().add("visualizer");
        canvasPane.setMinSize(400, 400);
        return canvasPane;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    private HBox createStatRow(String label, Label valueLabel) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(l, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void generateArray() {
        if (isSorting) return;
        canvasPane.getChildren().clear();
        
        int size = (int) sizeSlider.getValue();
        cnodes = new CNode[size];
        Random r = new Random();
        
        double width = canvasPane.getWidth();
        double height = canvasPane.getHeight();
        
        if (width < 100) width = 800;
        if (height < 100) height = 600;
        
        double usableWidth = width - (PADDING * 2);
        double usableHeight = height - (PADDING * 2);
        double barWidth = usableWidth / size;
        
        for (int i = 0; i < size; i++) {
            int val = r.nextInt(100) + 5;
            CNode node = new CNode(val);
            
            node.setX(PADDING + (i * barWidth));
            node.setWidth(barWidth - 2);
            node.setHeight(usableHeight * (val / 100.0));
            node.setY(height - PADDING - node.getHeight());
            
            node.setFill(AbstractSort.START_COLOR);
            
            cnodes[i] = node;
            canvasPane.getChildren().add(node);
        }
        
        statTimeLabel.setText("0 ms");
        statComparisonsLabel.setText("0");
        statMovesLabel.setText("0");
    }

    private void startSort() {
        if (isSorting) return;
        isSorting = true;
        sortBtn.setDisable(true);
        shuffleBtn.setDisable(true);
        algoCombo.setDisable(true);
        sizeSlider.setDisable(true);
        theoryCombo.setDisable(true);
        readTheoryBtn.setDisable(true);
        pauseBtn.setDisable(false);
        pauseBtn.setText("Pause");

        double width = canvasPane.getWidth();
        double usableWidth = width - (PADDING * 2);
        double dx = usableWidth / cnodes.length; 
        
        AbstractSort sorter = switch (algoCombo.getValue()) {
            case "Bubble Sort" -> new BubbleSort(dx);
            case "Selection Sort" -> new SelectionSort(dx);
            case "Insertion Sort" -> new InsertionSort(dx);
            case "Merge Sort" -> new MergeSort(dx);
            case "Quick Sort" -> new QuickSort(dx);
            case "Heap Sort" -> new HeapSort(dx);
            default -> new BubbleSort(dx);
        };

        ArrayList<javafx.animation.Transition> transitions = sorter.startSort(cnodes);
        
        statTimeLabel.setText("0 ms");
        statComparisonsLabel.setText("0");
        statMovesLabel.setText("0");
        
        activeTransition = new SequentialTransition();
        activeTransition.getChildren().addAll(transitions);
        
        switch (speedCombo.getValue()) {
            case "Slow" -> activeTransition.setRate(0.5);
            case "Normal" -> activeTransition.setRate(1.0);
            case "Fast" -> activeTransition.setRate(2.5);
            case "Instant" -> activeTransition.setRate(10.0);
        }

        startTime = System.currentTimeMillis();
        accumulatedTime = 0;

        statsTimer = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long elapsed = accumulatedTime + (System.currentTimeMillis() - startTime);
            statTimeLabel.setText(elapsed + " ms");
        }));
        statsTimer.setCycleCount(Timeline.INDEFINITE);
        statsTimer.play();
        
        activeTransition.setOnFinished(e -> {
            statsTimer.stop();
            long totalTime = accumulatedTime + (System.currentTimeMillis() - startTime);
            
            statTimeLabel.setText(totalTime + " ms");
            statComparisonsLabel.setText(String.valueOf(sorter.comparisons));
            statMovesLabel.setText(String.valueOf(sorter.moves));
            endSorting();
        });

        activeTransition.play();
    }

    private void togglePause() {
        if (activeTransition == null) return;
        if (pauseBtn.getText().equals("Pause")) {
            activeTransition.pause();
            statsTimer.pause();
            pauseBtn.setText("Resume");
            accumulatedTime += System.currentTimeMillis() - startTime;
        } else {
            activeTransition.play();
            statsTimer.play();
            pauseBtn.setText("Pause");
            startTime = System.currentTimeMillis();
        }
    }

    private void endSorting() {
        isSorting = false;
        sortBtn.setDisable(false);
        shuffleBtn.setDisable(false);
        algoCombo.setDisable(false);
        sizeSlider.setDisable(false);
        theoryCombo.setDisable(false);
        readTheoryBtn.setDisable(false);
        pauseBtn.setDisable(true);
    }

    private void showTheoryDialog() {
        String algoName = theoryCombo.getValue();
        if (algoName == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("modal-bg");

        Label title = new Label(algoName);
        title.getStyleClass().add("modal-title");

        TextArea textArea = new TextArea(theoryMap.get(algoName));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("theory-text");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        box.getChildren().addAll(title, textArea, closeBtn);

        // Increased window size
        Scene scene = new Scene(box, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/sortscope.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}