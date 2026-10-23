package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import com.vbforge.fitapp.service.ServiceException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AccountView extends ScrollPane {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault());

    private final AppContext ctx;
    private final Label userLabel = new Label();
    private final Label emailLabel = new Label();
    private final Label clock = new Label();
    private final Label date = new Label();
    private final TableView<LocalDateTime> history = new TableView<>();
    private final Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));

    private final PasswordField oldPass = new PasswordField();
    private final PasswordField newPass = new PasswordField();
    private final PasswordField confirmPass = new PasswordField();
    private final Label passMsg = UiUtil.errorLabel();

    public AccountView(AppContext ctx) {
        this.ctx = ctx;
        ticker.setCycleCount(Animation.INDEFINITE);

        Label title = new Label("Account Dashboard");
        title.getStyleClass().add("page-title");
        userLabel.getStyleClass().add("accent-text");
        emailLabel.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label as = new Label("Logged in as: ");
        as.getStyleClass().add("muted");
        VBox who = new VBox(2, new HBox(as, userLabel), emailLabel);
        who.setAlignment(Pos.CENTER_RIGHT);
        ((HBox) who.getChildren().get(0)).setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(title, spacer, who);
        header.setAlignment(Pos.CENTER_LEFT);

        // clock card
        Label caption = new Label("LOCAL TIME");
        caption.getStyleClass().add("field-label");
        clock.getStyleClass().add("clock-label");
        date.getStyleClass().add("muted");
        VBox clockCard = new VBox(6, caption, clock, date);
        clockCard.setAlignment(Pos.CENTER);
        clockCard.getStyleClass().add("card");

        // change password card
        Label cpTitle = new Label("Change Password");
        cpTitle.getStyleClass().add("section-title");
        oldPass.setPromptText("Current password");
        newPass.setPromptText("New password (min. 8, letters + digits)");
        confirmPass.setPromptText("Repeat new password");
        confirmPass.setOnAction(e -> changePassword());
        Button update = new Button("Update Password");
        update.setMaxWidth(Double.MAX_VALUE);
        update.setOnAction(e -> changePassword());
        VBox passCard = new VBox(10, cpTitle, oldPass, newPass, confirmPass, passMsg, update);
        passCard.getStyleClass().add("card");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c, c);
        grid.add(clockCard, 0, 0);
        grid.add(passCard, 1, 0);
        clockCard.setMaxHeight(Double.MAX_VALUE);
        passCard.setMaxHeight(Double.MAX_VALUE);

        // login history
        Label hTitle = new Label("Recent Login History");
        hTitle.getStyleClass().add("section-title");
        TableColumn<LocalDateTime, String> dCol = new TableColumn<>("Date");
        dCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().format(DATE)));
        TableColumn<LocalDateTime, String> tCol = new TableColumn<>("Time");
        tCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().format(TIME)));
        history.getColumns().addAll(java.util.List.of(dCol, tCol));
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        history.setPlaceholder(new Label("No logins recorded yet."));
        history.setFixedCellSize(46);
        history.setPrefHeight(46 * 5 + 42);
        history.setMinHeight(46 * 5 + 42);
        VBox historyCard = new VBox(10, hTitle, history);

        VBox content = new VBox(24, header, grid, historyCard);
        content.setPadding(new Insets(0, 4, 0, 0));
        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
    }

    /** Reloads all data for the logged-in user and (re)starts the clock. */
    public void refresh() {
        if (!ctx.session().isLoggedIn()) {
            return;
        }
        userLabel.setText(ctx.session().getCurrentUser().username());
        emailLabel.setText(ctx.session().getCurrentUser().email());
        history.setItems(FXCollections.observableArrayList(ctx.auth().recentLogins(5)));
        tick();
        ticker.play();
    }

    public void dispose() {
        ticker.stop();
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        clock.setText(now.format(TIME));
        date.setText(now.format(LONG_DATE));
    }

    private void changePassword() {
        UiUtil.setMessage(passMsg, null);
        passMsg.getStyleClass().setAll("error-label");
        if (!newPass.getText().equals(confirmPass.getText())) {
            UiUtil.setMessage(passMsg, "New passwords do not match.");
            return;
        }
        try {
            ctx.auth().changePassword(oldPass.getText(), newPass.getText());
            oldPass.clear();
            newPass.clear();
            confirmPass.clear();
            passMsg.getStyleClass().setAll("success-label");
            UiUtil.setMessage(passMsg, "Password updated.");
        } catch (ServiceException ex) {
            UiUtil.setMessage(passMsg, ex.getMessage());
        }
    }
}
