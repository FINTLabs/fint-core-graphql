package no.fintlabs.reflection.model;

public enum Multiplicity {
    ONE_TO_ONE("ONE_TO_ONE"),
    ZERO_TO_MANY("NONE_TO_MANY"),
    ONE_TO_MANY("ONE_TO_MANY"),
    ZERO_TO_ONE("NONE_TO_ONE");


    private final String label;

    Multiplicity(String label) {
        this.label = label;
    }

    public static Multiplicity fromLabel(String label) {
        for (Multiplicity multiplicity : Multiplicity.values()) {
            if (multiplicity.label.equals(label)) {
                return multiplicity;
            }
        }
        throw new IllegalArgumentException("Unexpected multiplicity: " + label);
    }
}
