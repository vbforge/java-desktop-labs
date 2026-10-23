package com.vbforge.dietarytrack;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The data-entry form. The fields scroll when the window is short, while Submit / Clear stay
 * pinned at the bottom so they are always visible.
 */
final class EntryFormPanel extends VBox {

    static final double WIDTH = 500;

    private static final int DEFAULT_WATER = 15;
    private static final int DEFAULT_MEALS = 3;
    private static final String DEFAULT_WEIGHT = "72.5";
    private static final String SEX_MALE = "Male";
    private static final String SEX_FEMALE = "Female";
    private static final String SEX_UNDISCLOSED = "Prefer not to say";

    private final StatusBar status;
    /** Receives a validated entry; returns true if it was stored (the form is then cleared). */
    private final Predicate<DietaryEntry> onSubmit;

    private final TextField firstNameField = input(new TextField());
    private final TextField lastNameField = input(new TextField());
    private final TextField phoneField = input(new TextField());
    private final TextField emailField = input(new TextField());
    private final ToggleGroup sexGroup = new ToggleGroup();
    private final DatePicker datePicker = input(new DatePicker(LocalDate.now()));
    private final Spinner<Integer> waterSpinner =
            input(new Spinner<Integer>(Validators.MIN_WATER, Validators.MAX_WATER, DEFAULT_WATER));
    private final TextField weightField = input(new TextField(DEFAULT_WEIGHT));
    private final Slider mealSlider = new Slider(0, 10, DEFAULT_MEALS);
    private final Label mealValueLabel = new Label(Integer.toString(DEFAULT_MEALS));
    private final ComboBox<String> milesCombo = input(new ComboBox<String>(FXCollections.observableArrayList(
            "Less than 1 Mile", "1 - 2 Miles", "2 - 3 Miles", "More than 3 Miles")));
    private final CheckBox wheatCheck = new CheckBox("Wheat");
    private final CheckBox sugarCheck = new CheckBox("Sugar");
    private final CheckBox dairyCheck = new CheckBox("Dairy");

    private final List<Field> fields = new ArrayList<>();
    private boolean submitAttempted;

    EntryFormPanel(StatusBar status, Predicate<DietaryEntry> onSubmit) {
        super(16);
        this.status = status;
        this.onSubmit = onSubmit;

        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);

        configureControls();
        registerValidation();

        VBox cards = new VBox(16, personalCard(), dailyCard(), dietaryCard());
        cards.setPadding(new Insets(0, 2, 2, 0));

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(false);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(scroll, buttonRow());
        reset();
    }

    // ------------------------------------------------------------------ layout

    private VBox personalCard() {
        FlowPane sexBox = new FlowPane(16, 6);
        sexBox.setAlignment(Pos.CENTER_LEFT);
        for (String option : List.of(SEX_MALE, SEX_FEMALE, SEX_UNDISCLOSED)) {
            RadioButton rb = new RadioButton(option);
            rb.setToggleGroup(sexGroup);
            rb.setUserData(option);
            sexBox.getChildren().add(rb);
        }

        GridPane grid = grid();
        grid.add(field("First Name *", firstNameField), 0, 0);
        grid.add(field("Last Name *", lastNameField), 1, 0);
        grid.add(field("Phone", phoneField), 0, 1);
        grid.add(field("Email", emailField), 1, 1);
        grid.add(field("Sex", sexBox), 0, 2, 2, 1);
        return card("Personal Info", grid);
    }

    private VBox dailyCard() {
        HBox sliderRow = new HBox(12, mealSlider, mealValueLabel);
        sliderRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mealSlider, Priority.ALWAYS);

        GridPane grid = grid();
        grid.add(field("Date", datePicker), 0, 0);
        grid.add(field("Water (cups)", waterSpinner), 1, 0);
        grid.add(field("Meals per day", sliderRow), 0, 1, 2, 1);
        grid.add(field("Walking (miles)", milesCombo), 0, 2);
        grid.add(field("Weight (kg) *", weightField), 1, 2);
        return card("Daily Stats", grid);
    }

    private VBox dietaryCard() {
        Label hint = new Label("Meals regularly contain:");
        hint.getStyleClass().add("input-label");
        FlowPane boxes = new FlowPane(16, 6, wheatCheck, sugarCheck, dairyCheck);
        return card("Dietary Intake", hint, boxes);
    }

    private HBox buttonRow() {
        Button submit = new Button("Submit");
        submit.getStyleClass().addAll("btn", "btn-primary");
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> submit());

        Button clear = new Button("Clear");
        clear.getStyleClass().add("btn");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(e -> {
            reset();
            status.info("Form cleared.");
        });

        HBox row = new HBox(10, submit, clear);
        HBox.setHgrow(submit, Priority.ALWAYS);
        HBox.setHgrow(clear, Priority.ALWAYS);
        return row;
    }

    private static VBox card(String title, Node... content) {
        Label heading = new Label(title.toUpperCase());
        heading.getStyleClass().add("section-title");
        heading.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(12, heading);
        card.getStyleClass().addAll("card", "form-card");
        card.getChildren().addAll(content);
        return card;
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            c.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    private static VBox field(String caption, Node control) {
        Label label = new Label(caption);
        label.getStyleClass().add("input-label");
        VBox box = new VBox(5, label, control);
        box.setMaxWidth(Double.MAX_VALUE);
        if (control instanceof javafx.scene.layout.Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private static <T extends Control> T input(T control) {
        control.getStyleClass().add("input");
        control.setMaxWidth(Double.MAX_VALUE);
        return control;
    }

    // --------------------------------------------------------------- behaviour

    private void configureControls() {
        limitLength(firstNameField, Validators.MAX_NAME_LENGTH);
        limitLength(lastNameField, Validators.MAX_NAME_LENGTH);
        limitLength(phoneField, 25);
        limitLength(emailField, Validators.MAX_EMAIL_LENGTH);
        weightField.setPromptText("e.g. 72.5");
        weightField.setTextFormatter(new TextFormatter<String>(
                c -> c.getControlNewText().matches("\\d{0,3}([.,]\\d{0,2})?") ? c : null));
        firstNameField.setPromptText("Required");
        lastNameField.setPromptText("Required");
        phoneField.setPromptText("+380 50 123 4567");
        emailField.setPromptText("name@example.com");

        // Selecting dates in the future makes no sense for a daily log.
        datePicker.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(!empty && item != null && item.isAfter(LocalDate.now()));
            }
        });

        waterSpinner.setEditable(true);
        waterSpinner.getStyleClass().add("water-spinner");
        TextField waterEditor = waterSpinner.getEditor();
        waterEditor.textProperty().addListener((obs, old, text) -> {
            if (!text.matches("\\d{0,2}")) {
                waterEditor.setText(old);
            }
        });
        waterEditor.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                SpinnerValueFactory<Integer> vf = waterSpinner.getValueFactory();
                Validators.parseWater(waterEditor.getText()).ifPresent(vf::setValue);
            }
        });

        mealSlider.getStyleClass().add("meal-slider");
        mealSlider.setShowTickMarks(true);
        mealSlider.setMajorTickUnit(1);
        mealSlider.setMinorTickCount(0);
        mealSlider.setBlockIncrement(1);
        mealSlider.setSnapToTicks(true);
        mealValueLabel.getStyleClass().add("slider-value");
        mealSlider.valueProperty().addListener((obs, old, val) ->
                mealValueLabel.setText(Integer.toString(val.intValue())));

        milesCombo.setMaxWidth(Double.MAX_VALUE);
    }

    private static void limitLength(TextField tf, int max) {
        tf.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= max ? c : null));
    }

    private void registerValidation() {
        register(firstNameField,
                () -> Validators.name(firstNameField.getText(), "First name"), firstNameField.textProperty());
        register(lastNameField,
                () -> Validators.name(lastNameField.getText(), "Last name"), lastNameField.textProperty());
        register(phoneField, () -> Validators.phone(phoneField.getText()), phoneField.textProperty());
        register(emailField, () -> Validators.email(emailField.getText()), emailField.textProperty());
        register(datePicker, () -> Validators.date(readDate(), LocalDate.now()),
                datePicker.valueProperty(), datePicker.getEditor().textProperty());
        register(waterSpinner, () -> Validators.water(waterSpinner.getEditor().getText()),
                waterSpinner.getEditor().textProperty());
        register(weightField, () -> Validators.weight(weightField.getText()), weightField.textProperty());
    }

    private void register(Control control, Supplier<Optional<String>> check,
                          javafx.beans.value.ObservableValue<?>... triggers) {
        Field f = new Field(control, check);
        fields.add(f);
        for (javafx.beans.value.ObservableValue<?> t : triggers) {
            t.addListener((obs, old, val) -> {
                if (f.touched || submitAttempted) {
                    f.validate();
                }
            });
        }
        // Validate when the user leaves the field (the Spinner / DatePicker keep focus in their editor).
        control.focusWithinProperty().addListener((obs, was, within) -> {
            if (was && !within) {
                f.touched = true;
                f.validate();
            }
        });
    }

    /** Reads the date as typed, so a half-edited DatePicker is never silently ignored. */
    private LocalDate readDate() {
        try {
            return datePicker.getConverter().fromString(datePicker.getEditor().getText());
        } catch (RuntimeException e) { // DateTimeParseException for text that is not a date
            return null;
        }
    }

    private void submit() {
        submitAttempted = true;
        Field firstInvalid = null;
        String firstMessage = null;
        for (Field f : fields) {
            Optional<String> error = f.validate();
            if (error.isPresent() && firstInvalid == null) {
                firstInvalid = f;
                firstMessage = error.get();
            }
        }
        if (firstInvalid != null) {
            status.error("⚠ " + firstMessage);
            firstInvalid.control.requestFocus();
            return;
        }

        if (onSubmit.test(buildEntry())) {
            reset();
        }
    }

    private DietaryEntry buildEntry() {
        Toggle selected = sexGroup.getSelectedToggle();
        String sex = selected != null ? (String) selected.getUserData() : SEX_UNDISCLOSED;
        return new DietaryEntry(
                0,
                readDate().toString(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                phoneField.getText().trim(),
                emailField.getText().trim(),
                sex,
                Validators.parseWater(waterSpinner.getEditor().getText()).orElseThrow(),
                (int) mealSlider.getValue(),
                wheatCheck.isSelected(),
                sugarCheck.isSelected(),
                dairyCheck.isSelected(),
                milesCombo.getValue(),
                Validators.parseWeight(weightField.getText()).orElseThrow());
    }

    /** Restores every control to its default and clears all validation marks. */
    void reset() {
        submitAttempted = false;
        for (Field f : fields) {
            f.touched = false;
            f.clearMark();
        }
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        sexGroup.getToggles().stream()
                .filter(t -> SEX_MALE.equals(t.getUserData()))
                .findFirst().ifPresent(t -> t.setSelected(true));
        datePicker.setValue(LocalDate.now());
        waterSpinner.getValueFactory().setValue(DEFAULT_WATER);
        waterSpinner.getEditor().setText(Integer.toString(DEFAULT_WATER));
        weightField.setText(DEFAULT_WEIGHT);
        mealSlider.setValue(DEFAULT_MEALS);
        milesCombo.getSelectionModel().selectFirst();
        wheatCheck.setSelected(true);
        sugarCheck.setSelected(false);
        dairyCheck.setSelected(true);
    }

    /** A validated control: toggles the red "invalid" style and an explanatory tooltip. */
    private static final class Field {
        final Control control;
        final Supplier<Optional<String>> check;
        boolean touched;

        Field(Control control, Supplier<Optional<String>> check) {
            this.control = control;
            this.check = check;
        }

        Optional<String> validate() {
            Optional<String> error = check.get();
            if (error.isPresent()) {
                if (!control.getStyleClass().contains("invalid")) {
                    control.getStyleClass().add("invalid");
                }
                control.setTooltip(new Tooltip(error.get()));
            } else {
                clearMark();
            }
            return error;
        }

        void clearMark() {
            control.getStyleClass().remove("invalid");
            control.setTooltip(null);
        }
    }
}
