package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import com.vbforge.fitapp.service.ServiceException;
import com.vbforge.fitapp.service.Validator;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** Split-screen authentication: animated gradient on the left, sliding Login/Register card on the right. */
public class AuthView extends HBox {

    private static final Duration SLIDE = Duration.millis(450);

    private final AppContext ctx;
    private final Runnable onAuthenticated;

    private final Pane slider = new Pane();
    private final VBox loginPanel;
    private final VBox registerPanel;
    private final StackPane viewport = new StackPane(slider);
    private boolean registerMode;
    private Timeline running;

    // login controls
    private final TextField loginUser = new TextField();
    private final PasswordField loginPass = new PasswordField();
    private final CheckBox remember = new CheckBox("Remember me");
    private final Label loginError = UiUtil.errorLabel();

    // register controls
    private final TextField regUser = new TextField();
    private final TextField regEmail = new TextField();
    private final PasswordField regPass = new PasswordField();
    private final Label regError = UiUtil.errorLabel();

    public AuthView(AppContext ctx, Runnable onAuthenticated) {
        this.ctx = ctx;
        this.onAuthenticated = onAuthenticated;

        Region visual = new SlideshowPane();
        HBox.setHgrow(visual, Priority.NEVER);
        visual.prefWidthProperty().bind(widthProperty().multiply(0.45));
        visual.minWidthProperty().bind(widthProperty().multiply(0.45));
        visual.maxWidthProperty().bind(widthProperty().multiply(0.45));

        loginPanel = buildLoginPanel();
        registerPanel = buildRegisterPanel();
        slider.getChildren().addAll(loginPanel, registerPanel);

        viewport.getStyleClass().add("content-area");
        HBox.setHgrow(viewport, Priority.ALWAYS);
        viewport.setMinWidth(0);
        // clip so the off-screen panel is hidden
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        // each panel always fills the viewport; the inactive one is parked one width to the side
        slider.setManaged(false);
        viewport.widthProperty().addListener((o, a, b) -> layoutPanels());
        viewport.heightProperty().addListener((o, a, b) -> layoutPanels());

        getChildren().addAll(visual, viewport);

        String saved = ctx.config().rememberedUsername();
        if (saved != null) {
            loginUser.setText(saved);
            remember.setSelected(true);
            javafx.application.Platform.runLater(loginPass::requestFocus);
        } else {
            javafx.application.Platform.runLater(loginUser::requestFocus);
        }
    }

    // ---- layout / animation ---------------------------------------------

    private void layoutPanels() {
        double w = viewport.getWidth();
        double h = viewport.getHeight();
        slider.resizeRelocate(0, 0, w, h);
        for (VBox p : new VBox[]{loginPanel, registerPanel}) {
            p.resize(w, h);
        }
        loginPanel.setLayoutY(0);
        registerPanel.setLayoutY(0);
        if (running == null || running.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            loginPanel.setLayoutX(registerMode ? -w : 0);
            registerPanel.setLayoutX(registerMode ? 0 : w);
        }
    }

    private void slideTo(boolean register) {
        if (register == registerMode && running != null) {
            return;
        }
        registerMode = register;
        UiUtil.setMessage(loginError, null);
        UiUtil.setMessage(regError, null);
        double w = viewport.getWidth();
        if (running != null) {
            running.stop();
        }
        running = new Timeline(new KeyFrame(SLIDE,
                new KeyValue(loginPanel.layoutXProperty(), register ? -w : 0, Interpolator.EASE_BOTH),
                new KeyValue(registerPanel.layoutXProperty(), register ? 0 : w, Interpolator.EASE_BOTH)));
        running.play();
        (register ? regUser : loginUser).requestFocus();
    }

    // ---- login panel -----------------------------------------------------

    private VBox buildLoginPanel() {
        Label title = new Label("Welcome Back");
        title.getStyleClass().add("form-title");
        Label sub = new Label("Sign in to track your fitness journey.");
        sub.getStyleClass().add("form-sub");

        loginUser.setPromptText("Enter username");
        loginPass.setPromptText("Enter password");
        loginPass.setOnAction(e -> doLogin());
        loginUser.setOnAction(e -> loginPass.requestFocus());

        Hyperlink forgot = new Hyperlink("Forgot password?");
        forgot.setOnAction(e -> showForgotPassword());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(remember, spacer, forgot);
        row.setAlignment(Pos.CENTER_LEFT);

        Button signIn = new Button("Sign In");
        signIn.getStyleClass().add("primary");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(false);
        signIn.setOnAction(e -> doLogin());

        Hyperlink toRegister = new Hyperlink("Don't have an account? Register here.");
        toRegister.setOnAction(e -> slideTo(true));

        VBox box = new VBox(16, title, sub, UiUtil.field("Username", loginUser),
                UiUtil.field("Password", loginPass), row, loginError, signIn, toRegister);
        box.setPadding(new Insets(40, 60, 40, 60));
        box.setAlignment(Pos.CENTER_LEFT);
        sub.setPadding(new Insets(0, 0, 14, 0));
        VBox.setMargin(toRegister, new Insets(0));
        toRegister.setAlignment(Pos.CENTER);
        toRegister.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void doLogin() {
        UiUtil.setMessage(loginError, null);
        try {
            ctx.auth().login(loginUser.getText(), loginPass.getText());
            if (remember.isSelected()) {
                ctx.config().remember(loginUser.getText().trim());
            } else {
                ctx.config().forget();
            }
            loginPass.clear();
            onAuthenticated.run();
        } catch (ServiceException ex) {
            UiUtil.setMessage(loginError, ex.getMessage());
        }
    }

    // ---- register panel --------------------------------------------------

    private VBox buildRegisterPanel() {
        Label title = new Label("Create Account");
        title.getStyleClass().add("form-title");
        Label sub = new Label("Join FitApp to start your workout diary.");
        sub.getStyleClass().add("form-sub");
        sub.setPadding(new Insets(0, 0, 14, 0));

        regUser.setPromptText("Choose a username");
        regEmail.setPromptText("name@example.com");
        regPass.setPromptText("Min. 8 characters, letters and digits");
        regPass.setOnAction(e -> doRegister());

        Button create = new Button("Create Account");
        create.getStyleClass().add("primary");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(e -> doRegister());

        Hyperlink toLogin = new Hyperlink("Already have an account? Sign in.");
        toLogin.setOnAction(e -> slideTo(false));
        toLogin.setMaxWidth(Double.MAX_VALUE);
        toLogin.setAlignment(Pos.CENTER);

        VBox box = new VBox(16, title, sub, UiUtil.field("Username", regUser), UiUtil.field("Email", regEmail),
                UiUtil.field("Password", regPass), regError, create, toLogin);
        box.setPadding(new Insets(40, 60, 40, 60));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void doRegister() {
        UiUtil.setMessage(regError, null);
        try {
            ctx.auth().register(regUser.getText(), regEmail.getText(), regPass.getText());
            regPass.clear();
            onAuthenticated.run();
        } catch (ServiceException ex) {
            UiUtil.setMessage(regError, ex.getMessage());
        }
    }

    // ---- forgot password -------------------------------------------------

    private void showForgotPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset password");
        dialog.setHeaderText("Enter the email of your account and choose a new password.");
        UiUtil.style(dialog, this);

        TextField email = new TextField();
        email.setPromptText("name@example.com");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New password");
        Label error = UiUtil.errorLabel();

        VBox content = new VBox(12, UiUtil.field("Email", email), UiUtil.field("New password", newPass), error);
        content.setPadding(new Insets(16));
        content.setPrefWidth(380);
        dialog.getDialogPane().setContent(content);

        ButtonType reset = new ButtonType("Reset password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reset, ButtonType.CANCEL);

        // validate inside the button handler so the dialog stays open on errors
        Button resetBtn = (Button) dialog.getDialogPane().lookupButton(reset);
        resetBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            UiUtil.setMessage(error, null);
            String emailError = Validator.email(email.getText().trim());
            if (emailError != null) {
                UiUtil.setMessage(error, emailError);
                ev.consume();
                return;
            }
            try {
                ctx.auth().resetPassword(email.getText(), newPass.getText());
            } catch (ServiceException ex) {
                UiUtil.setMessage(error, ex.getMessage());
                ev.consume();
            }
        });

        if (dialog.showAndWait().filter(b -> b == reset).isPresent()) {
            UiUtil.info(this, "Your password has been changed. You can sign in now.");
        }
    }
}
