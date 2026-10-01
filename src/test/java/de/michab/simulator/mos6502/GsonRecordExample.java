package de.michab.simulator.mos6502;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class GsonRecordExample {

    // 1. Define Java Records to map your JSON schema structure
    // Use @SerializedName if the JSON snake_case keys differ from camelCase record fields
    public record Address(String city, String zipCode) {}

    public record UserProfile(
        String name,
        int age,
        boolean active,
        @SerializedName("postal_address") Address address
    ) {}

    public static void main(String[] args) {
        // 2. Mock raw JSON text using a Java Text Block
        String rawJson = """
            {
                "name": "Jane Doe",
                "age": 28,
                "active": true,
                "postal_address": {
                    "city": "Berlin",
                    "zipCode": "10115"
                }
            }
            """;

        // 3. Initialize the standard Gson parser instance
        Gson gson = new Gson();

        // 4. Map the raw JSON string directly onto the UserProfile record
        UserProfile profile = gson.fromJson(rawJson, UserProfile.class);

        // 5. Interact with data using native record accessor methods
        System.out.println("User Name: " + profile.name()); // Native getter
        System.out.println("Is Active: " + profile.active());
        System.out.println("City: " + profile.address().city()); // Deep mapping
        
        // Print automated toString() implementation provided by records
        System.out.println("\nFull Record Data:\n" + profile);
    }
}
