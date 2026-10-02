package de.michab.simulator.mos6502;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CpuTestData {
    // A Gson instance able to deserialize the flat JSON cycle arrays into Cycle records.
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Cycle.class, (JsonDeserializer<Cycle>) (json, typeOfT, context) -> {
            var array = json.getAsJsonArray();
            return new Cycle(
                array.get(0).getAsInt(),
                array.get(1).getAsInt(),
                array.get(2).getAsString());
        })
        .create();

    // Represents a single execution cycle: [address, value, operation]
    public record Cycle(int address, int value, String operation) {
        public String toString() {
            return String.format("Cycle[address=0x%04X, value=0x%02X, operation=%s]", address(), value(), operation());
        }
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
        List<Cycle> cycles
    ) {}
}
