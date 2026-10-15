package com.vbforge.ledger;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDate;

/**
 * JavaFX-flavoured wrapper around a Transaction.
 *
 * TableView columns bind to Property objects, not to plain getters, so
 * every displayed field needs a property here. Yes, this is boilerplate —
 * it's the price of a binding-based UI. Keeping it in one class means the
 * rest of the app only sees Transaction.
 */
public class TransactionRow {

    private final SimpleLongProperty id = new SimpleLongProperty();
    private final SimpleObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final SimpleStringProperty description = new SimpleStringProperty();
    private final SimpleDoubleProperty amount = new SimpleDoubleProperty();
    private final SimpleStringProperty category = new SimpleStringProperty();

    public TransactionRow(Transaction t) {
        id.set(t.id());
        date.set(t.occurredOn());
        description.set(t.description());
        amount.set(t.amount());
        category.set(t.categoryName() == null ? "—" : t.categoryName());
    }

    public long getId() { return id.get(); }

    public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
    public SimpleStringProperty descriptionProperty()    { return description; }
    public SimpleDoubleProperty amountProperty()         { return amount; }
    public SimpleStringProperty categoryProperty()       { return category; }
}