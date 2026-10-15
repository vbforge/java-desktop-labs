package com.vbforge.ledger;

import java.time.LocalDate;

/**
 * Immutable snapshot of a transaction row.
 *
 * The JavaFX properties live in TransactionRow (see below) — keeping them
 * out of here means this record is a clean DTO you can pass around, test,
 * and later serialise without pulling JavaFX into the model layer.
 */
public record Transaction(
        long id,
        LocalDate occurredOn,
        String description,
        double amount,
        Long categoryId,
        String categoryName
) {}