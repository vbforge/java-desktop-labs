package com.vbforge.animalpedia.ui;

import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.ConservationStatus;
import com.vbforge.animalpedia.model.DataSource;
import com.vbforge.animalpedia.model.Taxonomy;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Builds the reading page for one animal. */
public final class ArticleView {

    private ArticleView() {}

    public static Node build(Animal a, Consumer<String> openLink, Runnable refresh) {
        VBox page = new VBox(22);
        page.setPadding(new Insets(28));
        page.setMaxWidth(840);
        HBox.setHgrow(page, Priority.ALWAYS);

        page.getChildren().add(hero(a));
        if (notBlank(a.funFact())) page.getChildren().add(funFact(a.funFact()));
        Node facts = factGrid(a);
        if (facts != null) page.getChildren().add(facts);
        page.getChildren().add(about(a));
        Node tree = familyTree(a);
        if (tree != null) page.getChildren().add(tree);
        page.getChildren().add(footer(a, openLink, refresh));

        HBox wrap = new HBox(page);
        wrap.setAlignment(Pos.TOP_CENTER);
        return wrap;
    }

    // ---- sections -----------------------------------------------------------------------------

    private static Node hero(Animal a) {
        String color = Theme.groupColor(a.group());

        VBox text = new VBox(6);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label title = new Label(a.commonName());
        title.getStyleClass().add("article-title");
        title.setWrapText(true);
        text.getChildren().add(title);

        if (notBlank(a.scientificName())) {
            Label sci = new Label(a.scientificName());
            sci.getStyleClass().add("article-sci");
            text.getChildren().add(sci);
        }

        HBox pills = new HBox(8);
        pills.setPadding(new Insets(6, 0, 0, 0));
        pills.getChildren().add(Theme.pill(a.group(), color));
        if (a.status() != null) pills.getChildren().add(Theme.pill(a.status().displayName(), a.status().colorHex()));
        text.getChildren().add(pills);

        HBox hero = new HBox(22, Theme.badge(a.emoji(), a.commonName(), color, 108), text);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(22, 26, 22, 22));
        hero.setStyle("-fx-background-color: " + color + "1F;-fx-background-radius: 26;");
        return hero;
    }

    private static Node funFact(String fact) {
        String lead = Theme.emojiSupported() ? "💡 Did you know?" : "Did you know?";
        Label head = new Label(lead);
        head.getStyleClass().add("fact-title");
        if (Theme.emojiSupported()) head.setStyle("-fx-font-family: '" + Theme.emojiFont() + "';");

        Label body = new Label(fact);
        body.getStyleClass().add("fact-text");
        body.setWrapText(true);

        VBox box = new VBox(6, head, body);
        box.getStyleClass().add("fact-box");
        return box;
    }

    private static Node factGrid(Animal a) {
        List<String[]> tiles = new ArrayList<>();
        add(tiles, "What it eats", a.diet(), null);
        add(tiles, "Where it lives", a.habitat(), null);
        add(tiles, "How long it lives", a.lifespan(), null);
        add(tiles, "How big", a.size(), null);
        ConservationStatus s = a.status();
        if (s != null) add(tiles, "How safe are they?", s.displayName() + ": " + s.kidExplanation(), s.colorHex());
        if (tiles.isEmpty()) return null;

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < tiles.size(); i++) {
            String[] t = tiles.get(i);
            Node tile = tile(t[0], t[1], t[2]);
            boolean lastAlone = i == tiles.size() - 1 && i % 2 == 0;
            grid.add(tile, i % 2, i / 2, lastAlone ? 2 : 1, 1);
        }
        return grid;
    }

    private static void add(List<String[]> tiles, String label, String value, String color) {
        if (notBlank(value)) tiles.add(new String[] {label, value, color});
    }

    private static Node tile(String label, String value, String color) {
        Label l = new Label(label.toUpperCase(Locale.ROOT));
        l.getStyleClass().add("tile-label");
        Label v = new Label(value);
        v.getStyleClass().add("tile-value");
        v.setWrapText(true);
        if (color != null) v.setStyle("-fx-text-fill: " + color + ";-fx-font-weight: bold;");
        VBox box = new VBox(4, l, v);
        box.getStyleClass().add("tile");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Node about(Animal a) {
        VBox box = new VBox(12);
        Label head = new Label("About the " + a.commonName().toLowerCase(Locale.ROOT));
        head.getStyleClass().add("section-title");
        head.setWrapText(true);
        box.getChildren().add(head);

        if (notBlank(a.summary())) {
            Label lead = new Label(capitalise(a.summary()));
            lead.getStyleClass().add("lead");
            lead.setWrapText(true);
            box.getChildren().add(lead);
        }
        for (String p : a.paragraphs()) {
            Label para = new Label(p);
            para.getStyleClass().add("body-text");
            para.setWrapText(true);
            box.getChildren().add(para);
        }
        if (a.paragraphs().isEmpty() && !notBlank(a.summary())) {
            Label none = new Label("There's no story for this one yet.");
            none.getStyleClass().add("body-text");
            box.getChildren().add(none);
        }
        return box;
    }

    private static Node familyTree(Animal a) {
        Taxonomy t = a.taxonomy();
        if (t == null || t.isEmpty()) return null;

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        grid.getStyleClass().add("tile");
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(90);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        int row = 0;
        for (Map.Entry<String, String> e : t.ranks().entrySet()) {
            addRank(grid, row++, e.getKey(), e.getValue(), false);
        }
        if (notBlank(a.scientificName())) addRank(grid, row, "Species", a.scientificName(), true);

        Label head = new Label("Family tree");
        head.getStyleClass().add("section-title");
        return new VBox(12, head, grid);
    }

    private static void addRank(GridPane grid, int row, String rank, String value, boolean italic) {
        Label k = new Label(rank);
        k.getStyleClass().add("rank-label");
        Label v = new Label(value);
        v.getStyleClass().add("rank-value");
        if (italic) v.setStyle("-fx-font-style: italic;");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private static Node footer(Animal a, Consumer<String> openLink, Runnable refresh) {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 0, 12, 0));

        Label source = new Label("Source: " + a.source().label());
        source.getStyleClass().add("footer-text");
        bar.getChildren().add(source);

        Hyperlink more = new Hyperlink("Read more on Wikipedia");
        more.setOnAction(e -> openLink.accept(a.wikiUrl()));
        bar.getChildren().add(more);

        if (a.source() != DataSource.BUILT_IN) {
            Button again = new Button("Refresh");
            again.getStyleClass().add("small-button");
            again.setOnAction(e -> refresh.run());
            bar.getChildren().add(again);
        }
        return bar;
    }

    // ---- helpers ------------------------------------------------------------------------------

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String capitalise(String s) {
        String t = s.trim();
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
