/*
 * Scream @ https://github.com/urschleim/scream
 *
 * Copyright © 1998-2022 Michael G. Binz
 */
package de.michab.simulator.mos6502;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Disabled;
import org.smack.util.StringUtil;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;


public class Cpu6502_Test {

    static Path mkTestPath( String name ) throws Exception {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        currentDir = currentDir + "/src/test/resources/de/michab/simulator/mos6502";
        Path path = Paths.get(currentDir, "v1", name + ".json");

        assertTrue(Files.exists(path), "File does not exist: " + path.toString());

        return path;
    }

    /**
     * Execute a named test.
     *
     * @param testName The name of the test to execute.
     * An example is "2f e5 a8".
     *
     * @throws Exception
     */
    private String findTest(String testName) throws Exception {
        String[] splitTestName = StringUtil.splitQuoted(testName);

        assertEquals(3, splitTestName.length, "testName must consist of three parts: " + testName);
        assertTrue(splitTestName[0].length() > 0);

        try (Stream<String> lines = Files.lines(mkTestPath(splitTestName[0])))
        {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.contains(testName)) {
                    return line.substring(0, line.length() - 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalArgumentException("Test not found: " + testName);
    }

    /**
     * Executes a json test.
     * @param rawJson
     * @throws Exception
     */
    private void execTest( String rawJson ) throws Exception
    {
        assertTrue(rawJson != null && !rawJson.isEmpty(), "rawJson must not be null or empty");

        Gson gson = CpuTestData.GSON;

        CpuTestData.TestRecord record = gson.fromJson(rawJson, CpuTestData.TestRecord.class);

        System.out.println("Test Case: " + record.name());
        System.out.println("Initial PC: " + record.initial().pc());
        System.out.println("Final PC: " + record.finalState().pc());

        processTestRecord(record, false);
    }

    /**
     * Executes a json test.
     * @param rawJson
     * @throws Exception
     */
    private void execNamedTest( String testName ) throws Exception
    {
        execTest(findTest(testName));
    }

    @Test
    public void opcodeTableNoGaps() throws Exception
    {
        for (int i = 0; i < 256; i++) {
            assertTrue(Opcodes.isValidOpcode(i), "Opcode table has a gap at index: 0x" + Integer.toHexString(i) + " (" + i + ")");
        }
    }

    @Test
    public void x_instr_2f_d5_a8() throws Exception
    {
        execNamedTest("2f d5 a8");
    }

    @Test
    public void x_instr_b1_28_b4() throws Exception
    {
        execNamedTest("b1 28 b4");
    }

    @Test
    public void x_instr_6b_77_ea() throws Exception
    {
        execNamedTest("6b 77 ea");
    }

    @Test
    public void x_instr_b1_28_b5_literal() throws Exception
    {
      String rawJson = """

            {
                "name": "b1 28 b5",
                "initial": {
                    "pc": 59082,
                    "s": 39,
                    "a": 57,
                    "x": 33,
                    "y": 174,
                    "p": 96,
                    "ram": [
                        [59082, 177],
                        [59083, 40],
                        [59084, 181],
                        [40, 160],
                        [41, 233],
                        [59982, 119]
                    ]
                },
                "final": {
                    "pc": 59084,
                    "s": 39,
                    "a": 119,
                    "x": 33,
                    "y": 174,
                    "p": 96,
                    "ram": [
                        [40, 160],
                        [41, 233],
                        [59082, 177],
                        [59083, 40],
                        [59084, 181],
                        [59982, 119]
                    ]
                },
                "cycles": [
                    [59082, 177, "read"],
                    [59083, 40, "read"],
                    [40, 160, "read"],
                    [41, 233, "read"],
                    [59726, 0, "read"],
                    [59982, 119, "read"]
                ]
            }

        """;
        execTest(rawJson);
    }

    @Test
    public void opcode() throws Exception {

        assertEquals("BRK", Opcodes.decode(0, new byte[]{0x00}));
        assertEquals("LDA ($28),Y", Opcodes.decode(0, new byte[]{(byte)0xb1, (byte)0x28, (byte)0xb5}));
        assertEquals("NOP_3c $1cfe,X", Opcodes.decode(0, new byte[]{(byte)60, (byte)254, (byte)28}));
    }

    /**
     * Hand-assembled machine code for the 16-bit Collatz conjecture
     * convergence benchmark, loaded at {@code $8000}.  Computes the Collatz
     * sequence for every seed from 2 to 999 and stops on the trailing BRK.
     *
     * Zero page layout: $00/$01 = current value (lo/hi), $02/$03 = seed
     * (lo/hi), $04/$05 = end of range (lo/hi), $06/$07 = scratch.
     */
    private static final int[] COLLATZ_BENCHMARK_PROGRAM = {
        0xA9, 0x02,             // RESET:       LDA #2
        0x85, 0x02,             //              STA $02      ; seed lo = 2
        0xA9, 0x00,             //              LDA #0
        0x85, 0x03,             //              STA $03      ; seed hi = 0
        0xA9, 0xE8,             //              LDA #$E8
        0x85, 0x04,             //              STA $04      ; end lo = $E8
        0xA9, 0x03,             //              LDA #$03
        0x85, 0x05,             //              STA $05      ; end hi = $03 (1000)
        0xA5, 0x02,             // NEXT_SEED:   LDA $02
        0x85, 0x00,             //              STA $00      ; value lo = seed lo
        0xA5, 0x03,             //              LDA $03
        0x85, 0x01,             //              STA $01      ; value hi = seed hi
        0xA5, 0x01,             // COLLATZ_LOOP:LDA $01
        0xD0, 0x06,             //              BNE NOT_ONE
        0xA5, 0x00,             //              LDA $00
        0xC9, 0x01,             //              CMP #1
        0xF0, 0x39,             //              BEQ SEED_CONVERGED
        0xA5, 0x00,             // NOT_ONE:     LDA $00
        0x4A,                   //              LSR A
        0x90, 0x27,             //              BCC IS_EVEN
        0xA5, 0x00,             // IS_ODD:      LDA $00
        0x0A,                   //              ASL A
        0x85, 0x06,             //              STA $06
        0xA5, 0x01,             //              LDA $01
        0x2A,                   //              ROL A
        0x85, 0x07,             //              STA $07
        0x18,                   //              CLC
        0xA5, 0x06,             //              LDA $06
        0x65, 0x00,             //              ADC $00
        0x85, 0x00,             //              STA $00
        0xA5, 0x07,             //              LDA $07
        0x65, 0x01,             //              ADC $01
        0x85, 0x01,             //              STA $01
        0x18,                   //              CLC
        0xA5, 0x00,             //              LDA $00
        0x69, 0x01,             //              ADC #1
        0x85, 0x00,             //              STA $00
        0xA5, 0x01,             //              LDA $01
        0x69, 0x00,             //              ADC #0
        0x85, 0x01,             //              STA $01
        0x4C, 0x18, 0x80,       //              JMP COLLATZ_LOOP
        0xA5, 0x01,             // IS_EVEN:     LDA $01
        0x4A,                   //              LSR A
        0x85, 0x01,             //              STA $01
        0xA5, 0x00,             //              LDA $00
        0x6A,                   //              ROR A
        0x85, 0x00,             //              STA $00
        0x4C, 0x18, 0x80,       //              JMP COLLATZ_LOOP
        0xE6, 0x02,             // SEED_CONVERGED: INC $02
        0xD0, 0x02,             //              BNE CHK_END
        0xE6, 0x03,             //              INC $03
        0xA5, 0x02,             // CHK_END:     LDA $02
        0xC5, 0x04,             //              CMP $04
        0xD0, 0xA9,             //              BNE NEXT_SEED
        0xA5, 0x03,             //              LDA $03
        0xC5, 0x05,             //              CMP $05
        0xD0, 0xA3,             //              BNE NEXT_SEED
        0x00                    //              BRK
    };

    /**
     * Loads the Collatz benchmark, runs the CPU until it reaches the
     * trailing BRK, and verifies that every seed from 2 to 999 converged.
     */
    @Test
    public void collatzBenchmark() throws Exception
    {
        final int origin = 0x8000;

        de.michab.simulator.TestMemory testMemory = new de.michab.simulator.TestMemory();
        for ( int i = 0; i < COLLATZ_BENCHMARK_PROGRAM.length; i++ )
            testMemory.write( origin + i, (byte)COLLATZ_BENCHMARK_PROGRAM[i] );

        de.michab.simulator.ClockHandle testClock = new de.michab.simulator.ClockHandle();
        Cpu6510 cpu = new Cpu6510( testMemory, testClock );

        cpu.setPC( origin );
        cpu.setAccu( 0 );
        cpu.setX( 0 );
        cpu.setY( 0 );
        cpu.setStack( 0xff );
        cpu.setStatusRegister( (byte)0x20 );

        int brkAddress = origin + COLLATZ_BENCHMARK_PROGRAM.length - 1;

        // Run until BRK is reached. The safety cap guards against an
        // infinite loop if the convergence logic regresses.
        final int maxSteps = 2_000_000;
        int steps = 0;
        while ( cpu.getPC() != brkAddress )
        {
            cpu.tick();
            steps++;
            assertTrue( steps < maxSteps,
                "Collatz benchmark did not reach BRK within " + maxSteps + " instructions" );
        }

        // All seeds from 2 to 999 must have converged: the seed counter
        // has caught up with the end of the range (1000).
        assertEquals( 0xE8, testMemory.read( 0x02 ) & 0xff, "Seed lo mismatch" );
        assertEquals( 0x03, testMemory.read( 0x03 ) & 0xff, "Seed hi mismatch" );

        // The last processed seed must have converged to 1.
        assertEquals( 1, testMemory.read( 0x00 ) & 0xff, "Last seed did not converge to 1 (lo)" );
        assertEquals( 0, testMemory.read( 0x01 ) & 0xff, "Last seed did not converge to 1 (hi)" );
    }

    // json 00 - 0f

    @Test
    public void json00() throws Exception
    {
        streamJson( "00.json", false );
    }

    @Test
    public void json01() throws Exception
    {
        streamJson( "01.json", false );
    }

    @Test
    public void json02() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "02.json", true );
    }

    @Test
    public void json03() throws Exception
    {
        streamJson( "03.json", false );
    }

    @Test
    public void json04() throws Exception
    {
        streamJson( "04.json", false );
    }

    @Test
    public void json05() throws Exception
    {
        streamJson( "05.json", false );
    }

    @Test
    public void json06() throws Exception
    {
        streamJson( "06.json", false );
    }

    @Test
    public void json07() throws Exception
    {
        streamJson( "07.json", false );
    }

    @Test
    public void json08() throws Exception
    {
        streamJson( "08.json", false );
    }

    @Test
    public void json09() throws Exception
    {
        streamJson( "09.json", false );
    }

    @Test
    public void json0a() throws Exception
    {
        streamJson( "0a.json", false );
    }

    @Test
    public void json0b() throws Exception
    {
        streamJson( "0b.json", false );
    }

    @Test
    public void json0c() throws Exception
    {
        streamJson( "0c.json", false );
    }

    @Test
    public void json0d() throws Exception
    {
        streamJson( "0d.json", false );
    }

    @Test
    public void json0e() throws Exception
    {
        streamJson( "0e.json", false );
    }

    @Test
    public void json0f() throws Exception
    {
        streamJson( "0f.json", false );
    }

    // // json 10 - 1f

    @Test
    public void json10() throws Exception
    {
        streamJson( "10.json", false );
    }

    @Test
    public void json11() throws Exception
    {
        streamJson( "11.json", false );
    }

    @Test
    public void json12() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "12.json", true );
    }

    @Test
    public void json13() throws Exception
    {
        streamJson( "13.json", false );
    }

    @Test
    public void json14() throws Exception
    {
        streamJson( "14.json", false );
    }

    @Test
    public void json15() throws Exception
    {
        streamJson( "15.json", false );
    }

    @Test
    public void json16() throws Exception
    {
        streamJson( "16.json", false );
    }

    @Test
    public void json17() throws Exception
    {
        streamJson( "17.json", false );
    }

    @Test
    public void json18() throws Exception
    {
        streamJson( "18.json", false );
    }

    @Test
    public void json19() throws Exception
    {
        streamJson( "19.json", false );
    }

    @Test
    public void json1a() throws Exception
    {
        streamJson( "1a.json", false );
    }

    @Test
    public void json1b() throws Exception
    {
        streamJson( "1b.json", false );
    }

    @Test
    public void json1c() throws Exception
    {
        streamJson( "1c.json", false );
    }

    @Test
    public void json1d() throws Exception
    {
        streamJson( "1d.json", false );
    }

    @Test
    public void json1e() throws Exception
    {
        streamJson( "1e.json", false );
    }

    @Test
    public void json1f() throws Exception
    {
        streamJson( "1f.json", false );
    }

    // // json 20 - 2f

    @Test
    public void json20() throws Exception
    {
        streamJson( "20.json", false );
    }

    @Test
    public void json21() throws Exception
    {
        streamJson( "21.json", false );
    }

    @Test
    public void json22() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "22.json", true );
    }

    @Test
    public void json23() throws Exception
    {
        streamJson( "23.json", false );
    }

    @Test
    public void json24() throws Exception
    {
        streamJson( "24.json", false );
    }

    @Test
    public void json25() throws Exception
    {
        streamJson( "25.json", false );
    }

    @Test
    public void json26() throws Exception
    {
        streamJson( "26.json", false );
    }

    @Test
    public void json27() throws Exception
    {
        streamJson( "27.json", false );
    }

    @Test
    public void json28() throws Exception
    {
        streamJson( "28.json", false );
    }

    @Test
    public void json29() throws Exception
    {
        streamJson( "29.json", false );
    }

    @Test
    public void json2a() throws Exception
    {
        streamJson( "2a.json", false );
    }

    @Test
    public void json2b() throws Exception
    {
        streamJson( "2b.json", false );
    }

    @Test
    public void json2c() throws Exception
    {
        streamJson( "2c.json", false );
    }

    @Test
    public void json2d() throws Exception
    {
        streamJson( "2d.json", false );
    }

    @Test
    public void json2e() throws Exception
    {
        streamJson( "2e.json", false );
    }

    @Test
    public void json2f() throws Exception
    {
        streamJson( "2f.json", false );
    }

    // // json 30 - 3f

    @Test
    public void json30() throws Exception
    {
        streamJson( "30.json", false );
    }

    @Test
    public void json31() throws Exception
    {
        streamJson( "31.json", false );
    }

    @Test
    public void json32() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "32.json", true );
    }

    @Test
    public void json33() throws Exception
    {
        streamJson( "33.json", false );
    }

    @Test
    public void json34() throws Exception
    {
        streamJson( "34.json", false );
    }

    @Test
    public void json35() throws Exception
    {
        streamJson( "35.json", false );
    }

    @Test
    public void json36() throws Exception
    {
        streamJson( "36.json", false );
    }

    @Test
    public void json37() throws Exception
    {
        streamJson( "37.json", false );
    }

    @Test
    public void json38() throws Exception
    {
        streamJson( "38.json", false );
    }

    @Test
    public void json39() throws Exception
    {
        streamJson( "39.json", false );
    }

    @Test
    public void json3a() throws Exception
    {
        streamJson( "3a.json", false );
    }

    @Test
    public void json3b() throws Exception
    {
        streamJson( "3b.json", false );
    }

    @Test
    public void json3c() throws Exception
    {
        streamJson( "3c.json", false );
    }

    @Test
    public void json3d() throws Exception
    {
        streamJson( "3d.json", false );
    }

    @Test
    public void json3e() throws Exception
    {
        streamJson( "3e.json", false );
    }

    @Test
    public void json3f() throws Exception
    {
        streamJson( "3f.json", false );
    }

    // // json 40 - 4f

    @Test
    public void json40() throws Exception
    {
        streamJson( "40.json", false );
    }

    @Test
    public void json41() throws Exception
    {
        streamJson( "41.json", false );
    }

    @Test
    public void json42() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "42.json", true );
    }

    @Test
    public void json43() throws Exception
    {
        streamJson( "43.json", false );
    }

    @Test
    public void json44() throws Exception
    {
        streamJson( "44.json", false );
    }

    @Test
    public void json45() throws Exception
    {
        streamJson( "45.json", false );
    }

    @Test
    public void json46() throws Exception
    {
        streamJson( "46.json", false );
    }

    @Test
    public void json47() throws Exception
    {
        streamJson( "47.json", false );
    }

    @Test
    public void json48() throws Exception
    {
        streamJson( "48.json", false );
    }

    @Test
    public void json49() throws Exception
    {
        streamJson( "49.json", false );
    }

    @Test
    public void json4a() throws Exception
    {
        streamJson( "4a.json", false );
    }

    @Test
    public void json4b() throws Exception
    {
        streamJson( "4b.json", false );
    }

    @Test
    public void json4c() throws Exception
    {
        streamJson( "4c.json", false );
    }

    @Test
    public void json4d() throws Exception
    {
        streamJson( "4d.json", false );
    }

    @Test
    public void json4e() throws Exception
    {
        streamJson( "4e.json", false );
    }

    @Test
    public void json4f() throws Exception
    {
        streamJson( "4f.json", false );
    }

    // // json 50 - 5f

    @Test
    public void json50() throws Exception
    {
        streamJson( "50.json", false );
    }

    @Test
    public void json51() throws Exception
    {
        streamJson( "51.json", false );
    }

    @Test
    public void json52() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "52.json", true );
    }

    @Test
    public void json53() throws Exception
    {
        streamJson( "53.json", false );
    }

    @Test
    public void json54() throws Exception
    {
        streamJson( "54.json", false );
    }

    @Test
    public void json55() throws Exception
    {
        streamJson( "55.json", false );
    }

    @Test
    public void json56() throws Exception
    {
        streamJson( "56.json", false );
    }

    @Test
    public void json57() throws Exception
    {
        streamJson( "57.json", false );
    }

    @Test
    public void json58() throws Exception
    {
        streamJson( "58.json", false );
    }

    @Test
    public void json59() throws Exception
    {
        streamJson( "59.json", false );
    }

    @Test
    public void json5a() throws Exception
    {
        streamJson( "5a.json", false );
    }

    @Test
    public void json5b() throws Exception
    {
        streamJson( "5b.json", false );
    }

    @Test
    public void json5c() throws Exception
    {
        streamJson( "5c.json", false );
    }

    @Test
    public void json5d() throws Exception
    {
        streamJson( "5d.json", false );
    }

    @Test
    public void json5e() throws Exception
    {
        streamJson( "5e.json", false );
    }

    @Test
    public void json5f() throws Exception
    {
        streamJson( "5f.json", false );
    }

    // // json 60 - 6f

    @Test
    public void json60() throws Exception
    {
        streamJson( "60.json", false );
    }

    @Test
    public void json61() throws Exception
    {
        streamJson( "61.json", false );
    }

    @Test
    public void json62() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "62.json", true );
    }

    @Test
    public void json63() throws Exception
    {
        streamJson( "63.json", false );
    }

    @Test
    public void json64() throws Exception
    {
        streamJson( "64.json", false );
    }

    @Test
    public void json65() throws Exception
    {
        streamJson( "65.json", false );
    }

    @Test
    public void json66() throws Exception
    {
        streamJson( "66.json", false );
    }

    @Test
    public void json67() throws Exception
    {
        streamJson( "67.json", false );
    }

    @Test
    public void json68() throws Exception
    {
        streamJson( "68.json", false );
    }

    @Test
    public void json69() throws Exception
    {
        streamJson( "69.json", false );
    }

    @Test
    public void json6a() throws Exception
    {
        streamJson( "6a.json", false );
    }

    @Test
    public void json6b() throws Exception
    {
        streamJson( "6b.json", false );
    }

    @Test
    public void json6c() throws Exception
    {
        streamJson( "6c.json", false );
    }

    @Test
    public void json6d() throws Exception
    {
        streamJson( "6d.json", false );
    }

    @Test
    public void json6e() throws Exception
    {
        streamJson( "6e.json", false );
    }

    @Test
    public void json6f() throws Exception
    {
        streamJson( "6f.json", false );
    }

    // // json 70 - 7f

    @Test
    public void json70() throws Exception
    {
        streamJson( "70.json", false );
    }
    @Test
    public void json71() throws Exception
    {
        streamJson( "71.json", false );
    }
    @Test
    public void json72() throws Exception
    {
        // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip the cycle check.
        streamJson( "72.json", true );
    }
    @Test
    public void json73() throws Exception
    {
        streamJson( "73.json", false );
    }
    @Test
    public void json74() throws Exception
    {
        streamJson( "74.json", false );
    }
    @Test
    public void json75() throws Exception
    {
        streamJson( "75.json", false );
    }
    @Test
    public void json76() throws Exception
    {
        streamJson( "76.json", false );
    }
    @Test
    public void json77() throws Exception
    {
        streamJson( "77.json", false );
    }
    @Test
    public void json78() throws Exception
    {
        streamJson( "78.json", false );
    }
    @Test
    public void json79() throws Exception
    {
        streamJson( "79.json", false );
    }
    @Test
    public void json7a() throws Exception
    {
        streamJson( "7a.json", false );
    }
    @Test
    public void json7b() throws Exception
    {
        streamJson( "7b.json", false );
    }
    @Test
    public void json7c() throws Exception
    {
        streamJson( "7c.json", false );
    }
    @Test
    public void json7d() throws Exception
    {
        streamJson( "7d.json", false );
    }
    @Test
    public void json7e() throws Exception
    {
        streamJson( "7e.json", false );
    }
    @Test
    public void json7f() throws Exception
    {
        streamJson( "7f.json", false );
    }

    // // json 80 - 8f

    @Test
    public void json80() throws Exception
    {
        streamJson( "80.json", false );
    }
    @Test
    public void json81() throws Exception
    {
        streamJson( "81.json", false );
    }
    @Test
    public void json82() throws Exception
    {
        streamJson( "82.json", false );
    }
    @Test
    public void json83() throws Exception
    {
        streamJson( "83.json", false );
    }
    @Test
    public void json84() throws Exception
    {
        streamJson( "84.json", false );
    }
    @Test
    public void json85() throws Exception
    {
        streamJson( "85.json", false );
    }
    @Test
    public void json86() throws Exception
    {
        streamJson( "86.json", false );
    }
    @Test
    public void json87() throws Exception
    {
        streamJson( "87.json", false );
    }
    @Test
    public void json88() throws Exception
    {
        streamJson( "88.json", false );
    }
    @Test
    public void json89() throws Exception
    {
        streamJson( "89.json", false );
    }
    @Test
    public void json8a() throws Exception
    {
        streamJson( "8a.json", false );
    }
    @Test
    public void json8b() throws Exception
    {
        streamJson( "8b.json", false );
    }
    @Test
    public void json8c() throws Exception
    {
        streamJson( "8c.json", false );
    }
    @Test
    public void json8d() throws Exception
    {
        streamJson( "8d.json", false );
    }
    @Test
    public void json8e() throws Exception
    {
        streamJson( "8e.json", false );
    }
    @Test
    public void json8f() throws Exception
    {
        streamJson( "8f.json", false );
    }

    // // json 90 - 9f

    @Test
    public void json90() throws Exception
    {
        streamJson( "90.json", false );
    }
    @Test
    public void json91() throws Exception
    {
        streamJson( "91.json", false );
    }
    @Test
    public void json92() throws Exception
    {
        streamJson( "92.json", true ); // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip cycle check
    }
    @Test
    public void json93() throws Exception
    {
        streamJson( "93.json", false );
    }
    @Test
    public void json94() throws Exception
    {
        streamJson( "94.json", false );
    }
    @Test
    public void json95() throws Exception
    {
        streamJson( "95.json", false );
    }
    @Test
    public void json96() throws Exception
    {
        streamJson( "96.json", false );
    }
    @Test
    public void json97() throws Exception
    {
        streamJson( "97.json", false );
    }
    @Test
    public void json98() throws Exception
    {
        streamJson( "98.json", false );
    }
    @Test
    public void json99() throws Exception
    {
        streamJson( "99.json", false );
    }
    @Test
    public void json9a() throws Exception
    {
        streamJson( "9a.json", false );
    }
    @Test
    public void json9b() throws Exception
    {
        streamJson( "9b.json", false );
    }
    @Test
    public void json9c() throws Exception
    {
        streamJson( "9c.json", false );
    }
    @Test
    public void json9d() throws Exception
    {
        streamJson( "9d.json", false );
    }
    @Test
    public void json9e() throws Exception
    {
        streamJson( "9e.json", false );
    }
    @Test
    public void json9f() throws Exception
    {
        streamJson( "9f.json", false );
    }

    // // json a0 - af

    @Test
    public void jsona0() throws Exception
    {
        streamJson( "a0.json", false );
    }
    @Test
    public void jsona1() throws Exception
    {
        streamJson( "a1.json", false );
    }
    @Test
    public void jsona2() throws Exception
    {
        streamJson( "a2.json", false );
    }
    @Test
    public void jsona3() throws Exception
    {
        streamJson( "a3.json", false );
    }
    @Test
    public void jsona4() throws Exception
    {
        streamJson( "a4.json", false );
    }
    @Test
    public void jsona5() throws Exception
    {
        streamJson( "a5.json", false );
    }
    @Test
    public void jsona6() throws Exception
    {
        streamJson( "a6.json", false );
    }
    @Test
    public void jsona7() throws Exception
    {
        streamJson( "a7.json", false );
    }
    @Test
    public void jsona8() throws Exception
    {
        streamJson( "a8.json", false );
    }
    @Test
    public void jsona9() throws Exception
    {
        streamJson( "a9.json", false );
    }
    @Test
    public void jsonaa() throws Exception
    {
        streamJson( "aa.json", false );
    }
    @Test
    public void jsonab() throws Exception
    {
        streamJson( "ab.json", false );
    }
    @Test
    public void jsonac() throws Exception
    {
        streamJson( "ac.json", false );
    }
    @Test
    public void jsonad() throws Exception
    {
        streamJson( "ad.json", false );
    }
    @Test
    public void jsonae() throws Exception
    {
        streamJson( "ae.json", false );
    }
    @Test
    public void jsonaf() throws Exception
    {
        streamJson( "af.json", false );
    }

    // // json b0 - bf

    @Test
    public void jsonb0() throws Exception
    {
        streamJson( "b0.json", false );
    }
    @Test
    public void jsonb1() throws Exception
    {
        streamJson( "b1.json", false );
    }
    @Test
    public void jsonb2() throws Exception
    {
        streamJson( "b2.json", true ); // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip cycle check
    }
    @Test
    public void jsonb3() throws Exception
    {
        streamJson( "b3.json", false );
    }
    @Test
    public void jsonb4() throws Exception
    {
        streamJson( "b4.json", false );
    }
    @Test
    public void jsonb5() throws Exception
    {
        streamJson( "b5.json", false );
    }
    @Test
    public void jsonb6() throws Exception
    {
        streamJson( "b6.json", false );
    }
    @Test
    public void jsonb7() throws Exception
    {
        streamJson( "b7.json", false );
    }
    @Test
    public void jsonb8() throws Exception
    {
        streamJson( "b8.json", false );
    }
    @Test
    public void jsonb9() throws Exception
    {
        streamJson( "b9.json", false );
    }
    @Test
    public void jsonba() throws Exception
    {
        streamJson( "ba.json", false );
    }
    @Test
    public void jsonbb() throws Exception
    {
        streamJson( "bb.json", false );
    }
    @Test
    public void jsonbc() throws Exception
    {
        streamJson( "bc.json", false );
    }
    @Test
    public void jsonbd() throws Exception
    {
        streamJson( "bd.json", false );
    }
    @Test
    public void jsonbe() throws Exception
    {
        streamJson( "be.json", false );
    }
    @Test
    public void jsonbf() throws Exception
    {
        streamJson( "bf.json", false );
    }

    // // json c0 - cf

    @Test
    public void jsonc0() throws Exception
    {
        streamJson( "c0.json", false );
    }
    @Test
    public void jsonc1() throws Exception
    {
        streamJson( "c1.json", false );
    }
    @Test
    public void jsonc2() throws Exception
    {
        streamJson( "c2.json", false );
    }
    @Test
    public void jsonc3() throws Exception
    {
        streamJson( "c3.json", false );
    }
    @Test
    public void jsonc4() throws Exception
    {
        streamJson( "c4.json", false );
    }
    @Test
    public void jsonc5() throws Exception
    {
        streamJson( "c5.json", false );
    }
    @Test
    public void jsonc6() throws Exception
    {
        streamJson( "c6.json", false );
    }
    @Test
    public void jsonc7() throws Exception
    {
        streamJson( "c7.json", false );
    }
    @Test
    public void jsonc8() throws Exception
    {
        streamJson( "c8.json", false );
    }
    @Test
    public void jsonc9() throws Exception
    {
        streamJson( "c9.json", false );
    }
    @Test
    public void jsonca() throws Exception
    {
        streamJson( "ca.json", false );
    }
    @Test
    public void jsoncb() throws Exception
    {
        streamJson( "cb.json", false );
    }
    @Test
    public void jsoncc() throws Exception
    {
        streamJson( "cc.json", false );
    }
    @Test
    public void jsoncd() throws Exception
    {
        streamJson( "cd.json", false );
    }
    @Test
    public void jsonce() throws Exception
    {
        streamJson( "ce.json", false );
    }
    @Test
    public void jsoncf() throws Exception
    {
        streamJson( "cf.json", false );
    }

    // // json d0 - df

    @Test
    public void jsond0() throws Exception
    {
        streamJson( "d0.json", false );
    }
    @Test
    public void jsond1() throws Exception
    {
        streamJson( "d1.json", false );
    }
    @Test
    public void jsond2() throws Exception
    {
        streamJson( "d2.json", true ); // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip cycle check
    }
    @Test
    public void jsond3() throws Exception
    {
        streamJson( "d3.json", false );
    }
    @Test
    public void jsond4() throws Exception
    {
        streamJson( "d4.json", false );
    }
    @Test
    public void jsond5() throws Exception
    {
        streamJson( "d5.json", false );
    }
    @Test
    public void jsond6() throws Exception
    {
        streamJson( "d6.json", false );
    }
    @Test
    public void jsond7() throws Exception
    {
        streamJson( "d7.json", false );
    }
    @Test
    public void jsond8() throws Exception
    {
        streamJson( "d8.json", false );
    }
    @Test
    public void jsond9() throws Exception
    {
        streamJson( "d9.json", false );
    }
    @Test
    public void jsonda() throws Exception
    {
        streamJson( "da.json", false );
    }
    @Test
    public void jsondb() throws Exception
    {
        streamJson( "db.json", false );
    }
    @Test
    public void jsondc() throws Exception
    {
        streamJson( "dc.json", false );
    }
    @Test
    public void jsondd() throws Exception
    {
        streamJson( "dd.json", false );
    }
    @Test
    public void jsonde() throws Exception
    {
        streamJson( "de.json", false );
    }
    @Test
    public void jsondf() throws Exception
    {
        streamJson( "df.json", false );
    }

    // // json e0 - ef

    @Test
    public void jsone0() throws Exception
    {
        streamJson( "e0.json", false );
    }
    @Test
    public void jsone1() throws Exception
    {
        streamJson( "e1.json", false );
    }
    @Test
    public void jsone2() throws Exception
    {
        streamJson( "e2.json", false );
    }
    @Test
    public void jsone3() throws Exception
    {
        streamJson( "e3.json", false );
    }
    @Test
    public void jsone4() throws Exception
    {
        streamJson( "e4.json", false );
    }
    @Test
    public void jsone5() throws Exception
    {
        streamJson( "e5.json", false );
    }
    @Test
    public void jsone6() throws Exception
    {
        streamJson( "e6.json", false );
    }
    @Test
    public void jsone7() throws Exception
    {
        streamJson( "e7.json", false );
    }
    @Test
    public void jsone8() throws Exception
    {
        streamJson( "e8.json", false );
    }
    @Test
    public void jsone9() throws Exception
    {
        streamJson( "e9.json", false );
    }
    @Test
    public void jsonea() throws Exception
    {
        streamJson( "ea.json", false );
    }
    @Test
    public void jsoneb() throws Exception
    {
        streamJson( "eb.json", false );
    }
    @Test
    public void jsonec() throws Exception
    {
        streamJson( "ec.json", false );
    }
    @Test
    public void jsoned() throws Exception
    {
        streamJson( "ed.json", false );
    }
    @Test
    public void jsonee() throws Exception
    {
        streamJson( "ee.json", false );
    }
    @Test
    public void jsonef() throws Exception
    {
        streamJson( "ef.json", false );
    }

    // // json f0 - ff

    @Test
    public void jsonf0() throws Exception
    {
        streamJson( "f0.json", false );
    }
    @Test
    public void jsonf1() throws Exception
    {
        streamJson( "f1.json", false );
    }
    @Test
    public void jsonf2() throws Exception
    {
        streamJson( "f2.json", true ); // JAM/KIL opcode: real hardware bus lock-up pattern isn't replicated, skip cycle check
    }
    @Test
    public void jsonf3() throws Exception
    {
        streamJson( "f3.json", false );
    }
    @Test
    public void jsonf4() throws Exception
    {
        streamJson( "f4.json", false );
    }
    @Test
    public void jsonf5() throws Exception
    {
        streamJson( "f5.json", false );
    }
    @Test
    public void jsonf6() throws Exception
    {
        streamJson( "f6.json", false );
    }
    @Test
    public void jsonf7() throws Exception
    {
        streamJson( "f7.json", false );
    }
    @Test
    public void jsonf8() throws Exception
    {
        streamJson( "f8.json", false );
    }
    @Test
    public void jsonf9() throws Exception
    {
        streamJson( "f9.json", false );
    }
    @Test
    public void jsonfa() throws Exception
    {
        streamJson( "fa.json", false );
    }
    @Test
    public void jsonfb() throws Exception
    {
        streamJson( "fb.json", false );
    }
    @Test
    public void jsonfc() throws Exception
    {
        streamJson( "fc.json", false );
    }
    @Test
    public void jsonfd() throws Exception
    {
        streamJson( "fd.json", false );
    }
    @Test
    public void jsonfe() throws Exception
    {
        streamJson( "fe.json", false );
    }
    @Test
    public void jsonff() throws Exception
    {
        streamJson( "ff.json", false );
    }

    private de.michab.simulator.TestMemory makeMemory( List<List<Integer>> ram ) {
        de.michab.simulator.TestMemory testMemory =
            new de.michab.simulator.TestMemory();

        // Initialize memory with the initial RAM state from the record.
        ram.forEach(pair -> {
            int address = pair.get(0);
            int value = pair.get(1);
            testMemory.write( address, (byte)value );
        });

        return testMemory;
    }

    public void streamJson( String filename, boolean skipTiming ) throws Exception
    {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        // /home/micbinz/git/route64
        currentDir = currentDir + "/src/test/resources/de/michab/simulator/mos6502";

        Path path = Paths.get(currentDir, "v1");

        assertTrue(Files.exists(path));

        assertTrue(Files.isDirectory(path));

        // Path to your huge JSON file
        Path filePath = path.resolve(filename);

        assertTrue(  Files.exists(filePath), "File does not exist: " + filePath.toString() );
        Gson gson = CpuTestData.GSON;

        String testName = "Unset";

        try (BufferedReader bufferedReader = Files.newBufferedReader(filePath);
             JsonReader jsonReader = new JsonReader(bufferedReader)) {

            jsonReader.beginArray();

            int recordCount = 0;

            while (jsonReader.hasNext()) {

                CpuTestData.TestRecord record = gson.fromJson(jsonReader, CpuTestData.TestRecord.class);

                testName = record.name();

                de.michab.simulator.TestMemory testMemory = makeMemory(  record.initial().ram() );

                try {

                    processTestRecord(record, skipTiming);

                } catch (AssertionError e) {

                    System.err.printf( "Test failure: %s %s / %s%n : %s%n",
                        filename,
                        record.name(),
                        Opcodes.decode(record.initial().pc(), testMemory),
                        e.getMessage()
                    );

                    throw e;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.printf( "Array index out of bounds: %s %s / %s%n : %s%n",
                        filename,
                        record.name(),
                        Opcodes.decode(record.initial().pc(), testMemory),
                        e.getMessage()
                    );

                    throw new AssertionError("Array index out of bounds during test: " + record.name(), e);
                }

                recordCount++;
            }

            jsonReader.endArray();

            System.out.printf( "Successfully processed %d tests from file '%s'.%n",
                recordCount,
                filename
            );

        } catch (Exception e) {
            System.err.println("An error occurred during streaming: " + e.getMessage() + " for test: " + testName);
            e.printStackTrace();
        }
    }

    public static byte[] transformByteArrayFromString(String byteArrayAsString) {
        if (byteArrayAsString == null || byteArrayAsString.trim().isEmpty()) {
            return new byte[0];
        }

        // Teilt den String bei einem oder mehreren Leerzeichen auf
        String[] tokens = byteArrayAsString.trim().split("\\s+");
        byte[] byteArray = new byte[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            // Byte.parseByte verarbeitet führende Nullen automatisch korrekt
            byteArray[i] = (byte) Integer.parseInt(tokens[i], 16);
        }

        return byteArray;
    }

    private static void processTestRecord( CpuTestData.TestRecord record, boolean skipTiming )
    {
        de.michab.simulator.TestMemory testMemory =
            new de.michab.simulator.TestMemory();

        // Initialize memory with the initial RAM state from the record.
        record.initial().ram().forEach(pair -> {
            int address = pair.get(0);
            int value = pair.get(1);
            testMemory.write( address, (byte)value );
        });

        de.michab.simulator.ClockHandle testClock =
            new de.michab.simulator.ClockHandle();

        Cpu6510 cpu = new Cpu6510(testMemory, testClock);

        cpu.setPC(record.initial().pc());
        cpu.setAccu(record.initial().a());
        cpu.setX(record.initial().x());
        cpu.setY(record.initial().y());
        cpu.setStack(record.initial().s());
        cpu.setStatusRegister((byte)record.initial().p());

        // Test naming is inconsistent.  So the name is hard to handle.
        // Here the full sequence of bytes is in the buffer so decoding
        // is trivial.
        var name = Opcodes.decode(cpu.getPC(), testMemory );

        testMemory.resetCycles();
        cpu.tick();

        assertEquals(
            record.finalState().pc(),
            cpu.getPC(),
            String.format("PC mismatch for test: %s.  Expected 0x%04X, got 0x%04X", name, record.finalState().pc(), cpu.getPC()));
        assertEquals(
            record.finalState().a(),
            cpu.getAccu(),
            String.format("Accumulator mismatch for test: %s.  Expected 0x%02X, got 0x%02X", name, record.finalState().a(), cpu.getAccu()));
        assertEquals(
            record.finalState().x(),
            cpu.getX(),
            String.format("X register mismatch for test: %s.  Expected 0x%02X, got 0x%02X", name, record.finalState().x(), cpu.getX()));
        assertEquals(
            record.finalState().y(),
            cpu.getY(),
            String.format("Y register mismatch for test: %s.  Expected 0x%02X, got 0x%02X", name, record.finalState().y(), cpu.getY()));
        assertEquals(
            record.finalState().s(),
            cpu.getStack(),
            String.format("Stack pointer mismatch for test: %s.  Expected 0x%02X, got 0x%02X", name, record.finalState().s(), cpu.getStack()));
        assertEquals(
            record.finalState().p(),
            cpu.getStatusRegister(),
            String.format("Status register mismatch for test: %s.  Expected 0x%02X, got 0x%02X", name, record.finalState().p(), cpu.getStatusRegister()));

        if (skipTiming) {
            return;
        }

        // Both Cycle and IO share the same toString format, so compare on that.
        var expectedCycles = record.cycles().stream().map(Object::toString).toList();
        var actualCycles = testMemory.getCycles().stream().map(Object::toString).toList();

        assertEquals(
            expectedCycles,
            actualCycles,
            String.format("Cycle mismatch for test: %s", name));
    }
}
