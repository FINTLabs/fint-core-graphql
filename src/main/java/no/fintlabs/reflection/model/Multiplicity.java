package no.fintlabs.reflection.model;

public enum Multiplicity {
    ONE_TO_ONE("1"),
    ZERO_TO_MANY("0..*"),
    ONE_TO_MANY("1..*"),
    ZERO_TO_ONE("0..1");


    private final String label;

    Multiplicity(String label) {
        this.label = label;
    }

    public static Multiplicity fromLabel(String label) {
        for (Multiplicity m : Multiplicity.values()) {
            if (m.label.equals(label)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unexpected multiplicity: " + label);
    }
}
