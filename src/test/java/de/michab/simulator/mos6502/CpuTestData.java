package de.michab.simulator.mos6502;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CpuTestData {
    // Represents a single execution cycle: [address (Integer), value (Integer), operation (String)]
    // Using List<Object> because the elements inside the JSON array have mixed types.
    public record Cycle(List<Object> details) {
        public int address() { return ((Number) details.get(0)).intValue(); }
        public int value() { return ((Number) details.get(1)).intValue(); }
        public String operation() { return (String) details.get(2); }
    }

    // Represents a processor state snapshot (initial or final)
    public record CpuState(
        // Program counter.
        int pc,
        // Stack pointer.
        int s,
        // Accumulator.
        int a,
        // Index registers.
        int x,
        int y,
        // Processor status flags.
        int p,
        // Memory state as a list of [address, value] pairs.
        List<List<Integer>> ram // A list of [address, value] pairs
    ) {}

    // Represents the root object wrapper inside the top-level array
    public record TestRecord(
        String name,
        CpuState initial,
        @SerializedName("final") CpuState finalState, // 'final' is a reserved keyword in Java
        List<List<Object>> cycles // Holds raw cycle arrays
    ) {}
}
