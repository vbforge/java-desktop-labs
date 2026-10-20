package com.vbforge.temptrack;

public record City(long id, String name, String country, double lat, double lon) {
    @Override
    public String toString() {
        return name + (country == null || country.isEmpty() ? "" : ", " + country);
    }
}