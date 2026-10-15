package com.vbforge.ledger;

public record Category(long id, String name) {
    @Override public String toString() { return name; }
}