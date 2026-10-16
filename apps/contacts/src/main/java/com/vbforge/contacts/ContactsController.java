package com.vbforge.contacts;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class ContactsController {

    // ---- FXML ----
    @FXML private ComboBox<BackendType> backendCombo;
    @FXML private Button importBtn, exportBtn;
    @FXML private TextField nameField, phoneField, emailField;
    @FXML private Button addBtn, updateBtn, deleteBtn;
    @FXML private TextField searchField;
    @FXML private TableView<Contact> table;
    @FXML private TableColumn<Contact, String> nameCol, phoneCol, emailCol;
    @FXML private Label statusLabel;

    // ---- state ----
    private final AppConfig config = AppConfig.open();
    private final ObservableList<Contact> contacts = FXCollections.observableArrayList();
    private FilteredList<Contact> filtered;

    private BackendType currentBackend;
    private ContactRepository repo;

    @FXML
    private void initialize() {
        currentBackend = config.backend();
        repo = RepositoryFactory.forBackend(currentBackend, config.baseDir());

        setupTable();
        setupBackendCombo();
        setupSearch();
        setupButtons();

        loadFromBackend();
    }

    // ---------- setup ----------

    private void setupTable() {
        nameCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().name()));
        phoneCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().phone()));
        emailCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().email()));

        filtered = new FilteredList<>(contacts, c -> true);
        table.setItems(filtered);

        // Populate the form on selection — this is what fixes the
        // "Edit can't see the selected contact" bug from the original.
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> populateForm(sel));

        // Double-click a row to jump into the form and focus Name.
        table.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<Contact>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    populateForm(row.getItem());
                    nameField.requestFocus();
                }
            });
            return row;
        });
    }

    private void setupBackendCombo() {
        backendCombo.setItems(FXCollections.observableArrayList(BackendType.values()));
        backendCombo.getSelectionModel().select(currentBackend);
        backendCombo.setOnAction(e -> onBackendSelected());
    }

    private void setupSearch() {
        searchField.textProperty().addListener(
                (obs, old, q) -> filtered.setPredicate(c -> c.matches(q)));
    }

    private void setupButtons() {
        addBtn.setOnAction(e -> onAdd());
        updateBtn.setOnAction(e -> onUpdate());
        deleteBtn.setOnAction(e -> onDelete());
        importBtn.setOnAction(e -> onImport());
        exportBtn.setOnAction(e -> onExport());

        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    boolean has = sel != null;
                    updateBtn.setDisable(!has);
                    deleteBtn.setDisable(!has);
                });
    }

    // ---------- data ----------

    private void loadFromBackend() {
        try {
            List<Contact> loaded = repo.findAll();
            contacts.setAll(loaded);
            status("Loaded " + loaded.size() + " contact"
                    + (loaded.size() == 1 ? "" : "s")
                    + " from " + repo.describe());
        } catch (Exception e) {
            contacts.clear();
            error("Could not load contacts", e);
        }
    }

    private void persist() {
        try {
            repo.saveAll(contacts);
        } catch (Exception e) {
            error("Could not save contacts", e);
        }
    }

    // ---------- CRUD ----------

    private void onAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { warn("Name is required."); return; }

        Contact k = new Contact(nextId(),
                name,
                phoneField.getText().trim(),
                emailField.getText().trim());

        contacts.add(k);
        persist();
        table.getSelectionModel().select(k);
        table.scrollTo(k);
        clearForm();
        status("Added " + k.name());
    }

    private void onUpdate() {
        Contact sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) { warn("Name is required."); return; }

        Contact updated = sel.withFields(name,
                phoneField.getText().trim(),
                emailField.getText().trim());

        int idx = contacts.indexOf(sel);
        if (idx < 0) return;

        contacts.set(idx, updated);
        persist();
        // Select by index — the instance changed, but the position didn't.
        table.getSelectionModel().select(idx);
        status("Updated " + updated.name());
    }

    private void onDelete() {
        Contact sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.name() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;

        contacts.remove(sel);
        persist();
        clearForm();
        status("Deleted " + sel.name());
    }

    // ---------- backend switching ----------

    private void onBackendSelected() {
        BackendType picked = backendCombo.getSelectionModel().getSelectedItem();
        if (picked == null || picked == currentBackend) return;

        ChoiceDialog<String> dlg = new ChoiceDialog<>(
                "Move current contacts",
                "Move current contacts",
                "Just switch");
        dlg.setTitle("Switch storage");
        dlg.setHeaderText("Switch storage to " + picked.display() + "?");
        dlg.setContentText("Choose:");

        Optional<String> choice = dlg.showAndWait();
        if (choice.isEmpty()) {
            backendCombo.getSelectionModel().select(currentBackend);
            return;
        }

        ContactRepository newRepo =
                RepositoryFactory.forBackend(picked, config.baseDir());

        if (choice.get().startsWith("Move")) {
            // Write the in-memory list to the new backend first, so
            // nothing is lost if the write fails.
            try {
                newRepo.saveAll(contacts);
            } catch (Exception e) {
                error("Could not migrate contacts", e);
                backendCombo.getSelectionModel().select(currentBackend);
                return;
            }
            currentBackend = picked;
            repo = newRepo;
            config.setBackend(picked);
            status("Switched to " + picked.display() + " and migrated "
                    + contacts.size() + " contacts");
        } else {
            // Switch without migrating — load whatever the new backend has.
            currentBackend = picked;
            repo = newRepo;
            config.setBackend(picked);
            table.getSelectionModel().clearSelection();
            clearForm();
            loadFromBackend();
        }
    }

    // ---------- import / export ----------

    private void onImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import contacts from…");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All supported", "*.csv", "*.json", "*.db"),
                new FileChooser.ExtensionFilter("CSV",    "*.csv"),
                new FileChooser.ExtensionFilter("JSON",   "*.json"),
                new FileChooser.ExtensionFilter("SQLite", "*.db"));

        File picked = fc.showOpenDialog(window());
        if (picked == null) return;

        Optional<ContactRepository> maybe =
                RepositoryFactory.forFile(picked.toPath());
        if (maybe.isEmpty()) {
            warn("Unrecognised file type: " + picked.getName());
            return;
        }

        List<Contact> incoming;
        try {
            incoming = maybe.get().findAll();
        } catch (Exception e) {
            error("Could not read file", e);
            return;
        }

        if (incoming.isEmpty()) {
            warn("No contacts found in that file.");
            return;
        }

        ChoiceDialog<String> dlg = new ChoiceDialog<>(
                "Merge with current",
                "Merge with current",
                "Replace current");
        dlg.setTitle("Import contacts");
        dlg.setHeaderText("Found " + incoming.size() + " contact"
                + (incoming.size() == 1 ? "" : "s") + " in " + picked.getName());
        dlg.setContentText("Choose:");

        Optional<String> choice = dlg.showAndWait();
        if (choice.isEmpty()) return;

        if (choice.get().startsWith("Replace")) {
            contacts.setAll(incoming);
        } else {
            // Merge — assign fresh IDs so nothing collides.
            long id = nextId();
            for (Contact k : incoming) {
                contacts.add(k.withId(id++));
            }
        }
        persist();
        status("Imported " + incoming.size() + " contact"
                + (incoming.size() == 1 ? "" : "s"));
    }

    private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export contacts to…");
        fc.setInitialFileName(currentBackend.defaultFileName());
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV",    "*.csv"),
                new FileChooser.ExtensionFilter("JSON",   "*.json"),
                new FileChooser.ExtensionFilter("SQLite", "*.db"));

        File picked = fc.showSaveDialog(window());
        if (picked == null) return;

        Optional<ContactRepository> maybe =
                RepositoryFactory.forFile(picked.toPath());
        if (maybe.isEmpty()) {
            warn("Could not determine format from the file extension.\n"
                    + "Use .csv, .json or .db.");
            return;
        }

        try {
            maybe.get().saveAll(contacts);
        } catch (Exception e) {
            error("Could not write file", e);
            return;
        }
        status("Exported " + contacts.size() + " contact"
                + (contacts.size() == 1 ? "" : "s")
                + " to " + picked.getName());
    }

    // ---------- helpers ----------

    private long nextId() {
        return contacts.stream().mapToLong(Contact::id).max().orElse(0L) + 1L;
    }

    private void populateForm(Contact k) {
        if (k == null) {
            nameField.clear();
            phoneField.clear();
            emailField.clear();
        } else {
            nameField.setText(k.name());
            phoneField.setText(k.phone());
            emailField.setText(k.email());
        }
    }

    private void clearForm() {
        populateForm(null);
        nameField.requestFocus();
    }

    private void status(String s) {
        statusLabel.setText(s);
    }

    private void warn(String s) {
        Alert a = new Alert(Alert.AlertType.WARNING, s, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void error(String header, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(header);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    private Window window() {
        return table == null || table.getScene() == null
                ? null
                : table.getScene().getWindow();
    }
}