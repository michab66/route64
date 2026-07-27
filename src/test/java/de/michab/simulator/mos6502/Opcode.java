package de.michab.simulator.mos6502;

public enum Opcode {
    RED(1),
    BLUE(2),
    YELLOW(3);

    private final int value;

    Opcode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    /**
     * Returns the name of the opcode that matches the passed integer.
     * @param i The mapped integer value (1, 2, or 3)
     * @return The name string of the enum (e.g., "RED")
     * @throws IllegalArgumentException if no match is found
     */
    public static String getName(int i) {
        for (Opcode opcode : Opcode.values()) {
            if (opcode.getValue() == i) {
                return opcode.name(); // Returns "RED", "BLUE", or "YELLOW"
            }
        }
        throw new IllegalArgumentException("No Opcode found matching value: " + i);
    }
}
