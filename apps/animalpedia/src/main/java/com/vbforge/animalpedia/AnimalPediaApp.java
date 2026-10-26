package com.vbforge.animalpedia;

import com.vbforge.animalpedia.engine.AnimalRepository;
import com.vbforge.animalpedia.engine.LookupException;
import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;
import com.vbforge.animalpedia.ui.ArticleView;
import com.vbforge.animalpedia.ui.Theme;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * AnimalPedia: a friendly animal encyclopedia.
 * Header with search on top, animal list on the left, reading page on the right.
 */
public class AnimalPediaApp extends Application {

    private enum Kind { ANIMAL, WEB }

    private record Row(Kind kind, String title, String subtitle, Animal animal) {}

    private static final System.Logger LOG = System.getLogger(AnimalPediaApp.class.getName());

    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ListView<Row> list = new ListView<>(rows);
    private final TextField search = new TextField();
    private final Label status = new Label();
    private final ScrollPane reader = new ScrollPane();
    private final ToggleGroup groupToggle = new ToggleGroup();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private final Random random = new Random();

    private AnimalRepository repo;
    private Stage stage;
    private Scene scene;
    private ToggleButton allChip;

    private List<String> webTitles = List.of();
    private String group;          // null means "All"
    private Animal current;
    private int openSeq;
    private int searchSeq;
    private boolean swallowSlash;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.repo = AnimalRepository.createDefault();

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildReader());
        root.setBottom(buildFooter());

        scene = new Scene(root, 1120, 740);
        scene.getStylesheets().add(getClass().getResource("/css/animalpedia.css").toExternalForm());
        installShortcuts();

        debounce.setOnFinished(e -> remoteSearch());
        search.textProperty().addListener((o, was, now) -> onFilterChanged());
        groupToggle.selectedToggleProperty().addListener((o, was, now) -> {
            if (now == null) {                         // never allow "nothing selected"
                if (was != null) groupToggle.selectToggle(was);
                return;
            }
            group = (String) now.getUserData();
            onFilterChanged();
        });
        allChip.setSelected(true);
        refreshRows();
        showWelcome();

        stage.setTitle("AnimalPedia");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    // ---- layout -------------------------------------------------------------------------------

    private Node buildHeader() {
        Label name = new Label("AnimalPedia");
        name.getStyleClass().add("logo-title");
        Label tagline = new Label("Discover amazing animals!");
        tagline.getStyleClass().add("logo-tagline");
        VBox logo = new VBox(0, name, tagline);

        search.setPromptText("Search for any animal...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(420);
        search.setOnAction(e -> {
            if (!rows.isEmpty()) open(rows.get(0));
        });
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN && !rows.isEmpty()) {
                list.requestFocus();
                list.getSelectionModel().select(0);
                e.consume();
            }
        });

        Button surprise = new Button("Surprise me!");
        surprise.getStyleClass().add("header-button");
        surprise.setOnAction(e -> surpriseMe());

        Region left = new Region();
        Region right = new Region();
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox header = new HBox(18, logo, left, search, right, surprise);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("header");
        return header;
    }

    private Node buildSidebar() {
        FlowPane chips = new FlowPane(6, 6);
        allChip = chip("All", null);
        chips.getChildren().add(allChip);
        for (String g : repo.catalog().groups()) chips.getChildren().add(chip(g, g));

        list.setCellFactory(lv -> new RowCell());
        list.setPlaceholder(new Label("No animals found.\nTry another name!"));
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                Row r = list.getSelectionModel().getSelectedItem();
                if (r != null) open(r);
                e.consume();
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);

        status.getStyleClass().add("status-text");
        status.setWrapText(true);

        VBox side = new VBox(12, chips, list, status);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(330);
        side.setMinWidth(330);
        side.setMaxWidth(330);
        return side;
    }

    private ToggleButton chip(String text, String value) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("chip");
        b.setUserData(value);
        b.setToggleGroup(groupToggle);
        return b;
    }

    private Node buildReader() {
        reader.setFitToWidth(true);
        reader.setFitToHeight(true);
        reader.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        reader.getStyleClass().add("reader");
        return reader;
    }

    private Node buildFooter() {
        Label text = new Label(repo.catalog().size()
                + " animals in the guide  ·  works without internet  ·  press / to search");
        text.getStyleClass().add("footer-text");
        HBox bar = new HBox(text);
        bar.getStyleClass().add("footer-bar");
        return bar;
    }

    // ---- list and search ----------------------------------------------------------------------

    private void onFilterChanged() {
        refreshRows();
        scheduleRemoteSearch();
    }

    private void refreshRows() {
        List<Row> next = new ArrayList<>();
        for (Animal a : repo.catalog().search(search.getText(), group)) {
            next.add(new Row(Kind.ANIMAL, a.commonName(), a.scientificName(), a));
        }
        if (group == null) {
            for (String t : webTitles) next.add(new Row(Kind.WEB, t, "From Wikipedia", null));
        }
        rows.setAll(next);
        if (current != null) highlight(current, false);
    }

    private void scheduleRemoteSearch() {
        searchSeq++;
        webTitles = List.of();
        status.setText("");
        debounce.stop();
        if (group == null && search.getText().trim().length() >= 2) debounce.playFromStart();
    }

    private void remoteSearch() {
        String query = search.getText().trim();
        if (query.length() < 2 || group != null || repo.catalog().findExact(query).isPresent()) return;

        int id = ++searchSeq;
        status.setText("Searching Wikipedia...");
        background(() -> repo.searchWeb(query),
                titles -> {
                    if (id != searchSeq) return;
                    status.setText("");
                    webTitles = titles;
                    refreshRows();
                },
                error -> {
                    if (id != searchSeq) return;
                    boolean offline = error instanceof LookupException le && le.kind() == LookupException.Kind.OFFLINE;
                    status.setText(offline ? error.getMessage() : "");
                });
    }

    private void highlight(Animal a, boolean scrollTo) {
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            String slug = r.kind() == Kind.ANIMAL ? r.animal().slug() : Animal.slugOf(r.title());
            if (slug.equals(a.slug())) {
                list.getSelectionModel().select(i);
                if (scrollTo) list.scrollTo(Math.max(0, i - 2));
                return;
            }
        }
        list.getSelectionModel().clearSelection();
    }

    // ---- opening animals ----------------------------------------------------------------------

    private void open(Row row) {
        if (row.kind() == Kind.ANIMAL) {
            openSeq++;
            show(row.animal());
        } else {
            loadFromWeb(row.title(), false);
        }
    }

    private void show(Animal a) {
        current = a;
        reader.setContent(ArticleView.build(a, this::openLink, this::refreshCurrent));
        reader.setVvalue(0);
        highlight(a, false);
    }

    private void loadFromWeb(String title, boolean refresh) {
        int id = ++openSeq;
        showLoading(title);
        background(() -> repo.open(title, refresh),
                animal -> {
                    if (id == openSeq) show(animal);
                },
                error -> {
                    if (id != openSeq) return;
                    String msg = error instanceof LookupException
                            ? error.getMessage()
                            : "Something went wrong. Please try again!";
                    if (!(error instanceof LookupException)) LOG.log(System.Logger.Level.ERROR, "Lookup failed", error);
                    showError(msg, () -> loadFromWeb(title, refresh));
                });
    }

    private void refreshCurrent() {
        if (current != null && current.source() != DataSource.BUILT_IN) {
            loadFromWeb(current.commonName(), true);
        }
    }

    private void surpriseMe() {
        List<Animal> all = repo.catalog().all();
        Animal pick;
        do {
            pick = all.get(random.nextInt(all.size()));
        } while (all.size() > 1 && current != null && pick.slug().equals(current.slug()));

        openSeq++;
        search.clear();
        allChip.setSelected(true);
        show(pick);
        highlight(pick, true);
    }

    private void openLink(String url) {
        if (url != null && url.startsWith("https://")) getHostServices().showDocument(url);
    }

    // ---- reader states ------------------------------------------------------------------------

    private void showWelcome() {
        Label title = new Label("Welcome to AnimalPedia!");
        title.getStyleClass().add("article-title");
        Label text = new Label("Pick an animal from the list, or search for any animal in the world.");
        text.getStyleClass().add("body-text");
        text.setWrapText(true);
        text.setMaxWidth(420);
        text.setAlignment(Pos.CENTER);
        text.setStyle("-fx-text-alignment: center;");
        Button go = new Button("Surprise me!");
        go.getStyleClass().add("primary-button");
        go.setOnAction(e -> surpriseMe());
        Label tip = new Label("Tip: press / to start searching");
        tip.getStyleClass().add("footer-text");
        reader.setContent(centered(Theme.badge(Animal.DEFAULT_EMOJI, "A", Theme.ORANGE, 120), title, text, go, tip));
    }

    private void showLoading(String title) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(56, 56);
        Label text = new Label("Looking up " + title + "...");
        text.getStyleClass().add("body-text");
        reader.setContent(centered(spinner, text));
    }

    private void showError(String message, Runnable retry) {
        Label title = new Label("Oops!");
        title.getStyleClass().add("article-title");
        Label text = new Label(message);
        text.getStyleClass().add("body-text");
        text.setWrapText(true);
        text.setMaxWidth(420);
        text.setStyle("-fx-text-alignment: center;");
        Button again = new Button("Try again");
        again.getStyleClass().add("primary-button");
        again.setOnAction(e -> retry.run());
        reader.setContent(centered(Theme.badge("🙈", "?", "#E5484D", 100), title, text, again));
    }

    private static Node centered(Node... children) {
        VBox box = new VBox(16, children);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }

    // ---- keyboard -----------------------------------------------------------------------------

    private void installShortcuts() {
        // After "/" moves focus to the search box, the matching "typed" event must not insert a slash.
        scene.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (swallowSlash && "/".equals(e.getCharacter())) e.consume();
            swallowSlash = false;
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            boolean typing = scene.getFocusOwner() instanceof TextInputControl;
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                e.consume();
            } else if ((e.isShortcutDown() && e.getCode() == KeyCode.F) || (!typing && "/".equals(e.getText()))) {
                swallowSlash = "/".equals(e.getText());
                search.requestFocus();
                search.selectAll();
                e.consume();
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.R) {
                refreshCurrent();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                if (!search.getText().isEmpty()) {
                    search.clear();
                } else {
                    list.requestFocus();
                }
                e.consume();
            }
        });
    }

    // ---- background work ----------------------------------------------------------------------

    private <T> void background(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onFailure.accept(task.getException()));
        Thread t = new Thread(task, "animalpedia-worker");
        t.setDaemon(true);
        t.start();
    }

    // ---- list cell ----------------------------------------------------------------------------

    private final class RowCell extends ListCell<Row> {
        RowCell() {
            setOnMouseClicked(e -> {
                if (!isEmpty() && getItem() != null) open(getItem());
            });
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            setText(null);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            Node badge;
            if (row.kind() == Kind.ANIMAL) {
                Animal a = row.animal();
                badge = Theme.badge(a.emoji(), a.commonName(), Theme.groupColor(a.group()), 46);
            } else {
                badge = Theme.badge("🔎", row.title(), Theme.groupColor(Animal.WEB_GROUP), 46);
            }
            Label title = new Label(row.title());
            title.getStyleClass().add("row-title");
            VBox text = new VBox(1, title);
            if (row.subtitle() != null && !row.subtitle().isBlank()) {
                Label sub = new Label(row.subtitle());
                sub.getStyleClass().add("row-sub");
                text.getChildren().add(sub);
            }
            text.setAlignment(Pos.CENTER_LEFT);
            HBox card = new HBox(12, badge, text);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("row-card");
            setGraphic(card);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
