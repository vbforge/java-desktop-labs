package com.vbforge.animalpedia.model;

/** The nine IUCN Red List categories, with kid-friendly explanations. */
public enum ConservationStatus {
    LC("Least Concern", "Not in danger right now", "#2EA44F"),
    NT("Near Threatened", "Could be in danger soon", "#A87B00"),
    VU("Vulnerable", "At risk in the wild", "#E07B1A"),
    EN("Endangered", "In serious danger of dying out", "#E5484D"),
    CR("Critically Endangered", "In extreme danger - very few are left", "#B42318"),
    EW("Extinct in the Wild", "Only alive in zoos and care centres", "#6B7280"),
    EX("Extinct", "Not alive anywhere any more", "#6B7280"),
    DD("Data Deficient", "Scientists need to learn more", "#8B93A1"),
    NE("Not Evaluated", "Scientists haven't checked yet", "#8B93A1");

    private final String displayName;
    private final String kidExplanation;
    private final String colorHex;

    ConservationStatus(String displayName, String kidExplanation, String colorHex) {
        this.displayName = displayName;
        this.kidExplanation = kidExplanation;
        this.colorHex = colorHex;
    }

    public String displayName() { return displayName; }
    public String kidExplanation() { return kidExplanation; }
    public String colorHex() { return colorHex; }

    /** Parses a two-letter code such as "vu"; anything unknown becomes {@link #NE}. */
    public static ConservationStatus fromCode(String code) {
        if (code == null) return NE;
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NE;
        }
    }
}
