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

        Gson gson = new Gson();

        CpuTestData.TestRecord record = gson.fromJson(rawJson, CpuTestData.TestRecord.class);

        System.out.println("Test Case: " + record.name());
        System.out.println("Initial PC: " + record.initial().pc());
        System.out.println("Final PC: " + record.finalState().pc());

        processTestRecord(record);
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
                    [59083, 40, "read"],
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

    // json 00 - 0f

    @Test
    public void json00() throws Exception
    {
        streamJson( "00.json" );
    }

    @Test
    public void json01() throws Exception
    {
        streamJson( "01.json" );
    }

    @Test
    public void json02() throws Exception
    {
        streamJson( "02.json" );
    }

    @Test
    public void json03() throws Exception
    {
        streamJson( "03.json" );
    }

    @Test
    public void json04() throws Exception
    {
        streamJson( "04.json" );
    }

    @Test
    public void json05() throws Exception
    {
        streamJson( "05.json" );
    }

    @Test
    public void json06() throws Exception
    {
        streamJson( "06.json" );
    }

    @Test
    public void json07() throws Exception
    {
        streamJson( "07.json" );
    }

    @Test
    public void json08() throws Exception
    {
        streamJson( "08.json" );
    }

    @Test
    public void json09() throws Exception
    {
        streamJson( "09.json" );
    }

    @Test
    public void json0a() throws Exception
    {
        streamJson( "0a.json" );
    }

    @Test
    public void json0b() throws Exception
    {
        streamJson( "0b.json" );
    }

    @Test
    public void json0c() throws Exception
    {
        streamJson( "0c.json" );
    }

    @Test
    public void json0d() throws Exception
    {
        streamJson( "0d.json" );
    }

    @Test
    public void json0e() throws Exception
    {
        streamJson( "0e.json" );
    }

    @Test
    public void json0f() throws Exception
    {
        streamJson( "0f.json" );
    }

    // json 10 - 1f

    @Test
    public void json10() throws Exception
    {
        streamJson( "10.json" );
    }

    @Test
    public void json11() throws Exception
    {
        streamJson( "11.json" );
    }

    @Test
    public void json12() throws Exception
    {
        streamJson( "12.json" );
    }

    @Test
    public void json13() throws Exception
    {
        streamJson( "13.json" );
    }

    @Test
    public void json14() throws Exception
    {
        streamJson( "14.json" );
    }

    @Test
    public void json15() throws Exception
    {
        streamJson( "15.json" );
    }

    @Test
    public void json16() throws Exception
    {
        streamJson( "16.json" );
    }

    @Test
    public void json17() throws Exception
    {
        streamJson( "17.json" );
    }

    @Test
    public void json18() throws Exception
    {
        streamJson( "18.json" );
    }

    @Test
    public void json19() throws Exception
    {
        streamJson( "19.json" );
    }

    @Test
    public void json1a() throws Exception
    {
        streamJson( "1a.json" );
    }

    @Test
    public void json1b() throws Exception
    {
        streamJson( "1b.json" );
    }

    @Test
    public void json1c() throws Exception
    {
        streamJson( "1c.json" );
    }

    @Test
    public void json1d() throws Exception
    {
        streamJson( "1d.json" );
    }

    @Test
    public void json1e() throws Exception
    {
        streamJson( "1e.json" );
    }

    @Test
    public void json1f() throws Exception
    {
        streamJson( "1f.json" );
    }

    // json 20 - 2f

    @Test
    public void json20() throws Exception
    {
        streamJson( "20.json" );
    }

    @Test
    public void json21() throws Exception
    {
        streamJson( "21.json" );
    }

    @Test
    public void json22() throws Exception
    {
        streamJson( "22.json" );
    }

    @Test
    public void json23() throws Exception
    {
        streamJson( "23.json" );
    }

    @Test
    public void json24() throws Exception
    {
        streamJson( "24.json" );
    }

    @Test
    public void json25() throws Exception
    {
        streamJson( "25.json" );
    }

    @Test
    public void json26() throws Exception
    {
        streamJson( "26.json" );
    }

    @Test
    public void json27() throws Exception
    {
        streamJson( "27.json" );
    }

    @Test
    public void json28() throws Exception
    {
        streamJson( "28.json" );
    }

    @Test
    public void json29() throws Exception
    {
        streamJson( "29.json" );
    }

    @Test
    public void json2a() throws Exception
    {
        streamJson( "2a.json" );
    }

    @Test
    public void json2b() throws Exception
    {
        streamJson( "2b.json" );
    }

    @Test
    public void json2c() throws Exception
    {
        streamJson( "2c.json" );
    }

    @Test
    public void json2d() throws Exception
    {
        streamJson( "2d.json" );
    }

    @Test
    public void json2e() throws Exception
    {
        streamJson( "2e.json" );
    }

    @Test
    public void json2f() throws Exception
    {
        streamJson( "2f.json" );
    }

    // json 30 - 3f

    @Test
    public void json30() throws Exception
    {
        streamJson( "30.json" );
    }

    @Test
    public void json31() throws Exception
    {
        streamJson( "31.json" );
    }

    @Test
    public void json32() throws Exception
    {
        streamJson( "32.json" );
    }

    @Test
    public void json33() throws Exception
    {
        streamJson( "33.json" );
    }

    @Test
    public void json34() throws Exception
    {
        streamJson( "34.json" );
    }

    @Test
    public void json35() throws Exception
    {
        streamJson( "35.json" );
    }

    @Test
    public void json36() throws Exception
    {
        streamJson( "36.json" );
    }

    @Test
    public void json37() throws Exception
    {
        streamJson( "37.json" );
    }

    @Test
    public void json38() throws Exception
    {
        streamJson( "38.json" );
    }

    @Test
    public void json39() throws Exception
    {
        streamJson( "39.json" );
    }

    @Test
    public void json3a() throws Exception
    {
        streamJson( "3a.json" );
    }

    @Test
    public void json3b() throws Exception
    {
        streamJson( "3b.json" );
    }

    @Test
    public void json3c() throws Exception
    {
        streamJson( "3c.json" );
    }

    @Test
    public void json3d() throws Exception
    {
        streamJson( "3d.json" );
    }

    @Test
    public void json3e() throws Exception
    {
        streamJson( "3e.json" );
    }

    @Test
    public void json3f() throws Exception
    {
        streamJson( "3f.json" );
    }

    // json 40 - 4f

    @Test
    public void json40() throws Exception
    {
        streamJson( "40.json" );
    }

    @Test
    public void json41() throws Exception
    {
        streamJson( "41.json" );
    }

    @Test
    public void json42() throws Exception
    {
        streamJson( "42.json" );
    }

    @Test
    public void json43() throws Exception
    {
        streamJson( "43.json" );
    }

    @Test
    public void json44() throws Exception
    {
        streamJson( "44.json" );
    }

    @Test
    public void json45() throws Exception
    {
        streamJson( "45.json" );
    }

    @Test
    public void json46() throws Exception
    {
        streamJson( "46.json" );
    }

    @Test
    public void json47() throws Exception
    {
        streamJson( "47.json" );
    }

    @Test
    public void json48() throws Exception
    {
        streamJson( "48.json" );
    }

    @Test
    public void json49() throws Exception
    {
        streamJson( "49.json" );
    }

    @Test
    public void json4a() throws Exception
    {
        streamJson( "4a.json" );
    }

    @Test
    public void json4b() throws Exception
    {
        streamJson( "4b.json" );
    }

    @Test
    public void json4c() throws Exception
    {
        streamJson( "4c.json" );
    }

    @Test
    public void json4d() throws Exception
    {
        streamJson( "4d.json" );
    }

    @Test
    public void json4e() throws Exception
    {
        streamJson( "4e.json" );
    }

    @Test
    public void json4f() throws Exception
    {
        streamJson( "4f.json" );
    }

    // json 50 - 5f

    @Test
    public void json50() throws Exception
    {
        streamJson( "50.json" );
    }

    @Test
    public void json51() throws Exception
    {
        streamJson( "51.json" );
    }

    @Test
    public void json52() throws Exception
    {
        streamJson( "52.json" );
    }

    @Test
    public void json53() throws Exception
    {
        streamJson( "53.json" );
    }

    @Test
    public void json54() throws Exception
    {
        streamJson( "54.json" );
    }

    @Test
    public void json55() throws Exception
    {
        streamJson( "55.json" );
    }

    @Test
    public void json56() throws Exception
    {
        streamJson( "56.json" );
    }

    @Test
    public void json57() throws Exception
    {
        streamJson( "57.json" );
    }

    @Test
    public void json58() throws Exception
    {
        streamJson( "58.json" );
    }

    @Test
    public void json59() throws Exception
    {
        streamJson( "59.json" );
    }

    @Test
    public void json5a() throws Exception
    {
        streamJson( "5a.json" );
    }

    @Test
    public void json5b() throws Exception
    {
        streamJson( "5b.json" );
    }

    @Test
    public void json5c() throws Exception
    {
        streamJson( "5c.json" );
    }

    @Test
    public void json5d() throws Exception
    {
        streamJson( "5d.json" );
    }

    @Test
    public void json5e() throws Exception
    {
        streamJson( "5e.json" );
    }

    @Test
    public void json5f() throws Exception
    {
        streamJson( "5f.json" );
    }

    // json 60 - 6f

    @Test
    public void json60() throws Exception
    {
        streamJson( "60.json" );
    }

    @Test
    public void json61() throws Exception
    {
        streamJson( "61.json" );
    }

    @Test
    public void json62() throws Exception
    {
        streamJson( "62.json" );
    }

    @Test
    public void json63() throws Exception
    {
        streamJson( "63.json" );
    }

    @Test
    public void json64() throws Exception
    {
        streamJson( "64.json" );
    }

    @Test
    public void json65() throws Exception
    {
        streamJson( "65.json" );
    }

    @Test
    public void json66() throws Exception
    {
        streamJson( "66.json" );
    }

    @Test
    public void json67() throws Exception
    {
        streamJson( "67.json" );
    }

    @Test
    public void json68() throws Exception
    {
        streamJson( "68.json" );
    }

    @Test
    public void json69() throws Exception
    {
        streamJson( "69.json" );
    }

    @Test
    public void json6a() throws Exception
    {
        streamJson( "6a.json" );
    }

    @Test
    public void json6b() throws Exception
    {
        streamJson( "6b.json" );
    }

    @Test
    public void json6c() throws Exception
    {
        streamJson( "6c.json" );
    }

    @Test
    public void json6d() throws Exception
    {
        streamJson( "6d.json" );
    }

    @Test
    public void json6e() throws Exception
    {
        streamJson( "6e.json" );
    }

    @Test
    public void json6f() throws Exception
    {
        streamJson( "6f.json" );
    }

    // json 70 - 7f

    @Test
    public void json70() throws Exception
    {
        streamJson( "70.json" );
    }
        @Test
    public void json71() throws Exception
    {
        streamJson( "71.json" );
    }
    @Test
    public void json72() throws Exception
    {
        streamJson( "72.json" );
    }
    @Test
    public void json73() throws Exception
    {
        streamJson( "73.json" );
    }
    @Test
    public void json74() throws Exception
    {
        streamJson( "74.json" );
    }
    @Test
    public void json75() throws Exception
    {
        streamJson( "75.json" );
    }
    @Test
    public void json76() throws Exception
    {
        streamJson( "76.json" );
    }
    @Test
    public void json77() throws Exception
    {
        streamJson( "77.json" );
    }
    @Test
    public void json78() throws Exception
    {
        streamJson( "78.json" );
    }
    @Test
    public void json79() throws Exception
    {
        streamJson( "79.json" );
    }
    @Test
    public void json7a() throws Exception
    {
        streamJson( "7a.json" );
    }
    @Test
    public void json7b() throws Exception
    {
        streamJson( "7b.json" );
    }
    @Test
    public void json7c() throws Exception
    {
        streamJson( "7c.json" );
    }
    @Test
    public void json7d() throws Exception
    {
        streamJson( "7d.json" );
    }
    @Test
    public void json7e() throws Exception
    {
        streamJson( "7e.json" );
    }
    @Test
    public void json7f() throws Exception
    {
        streamJson( "7f.json" );
    }

    // json 80 - 8f

    @Test
    public void json80() throws Exception
    {
        streamJson( "80.json" );
    }
    @Test
    public void json81() throws Exception
    {
        streamJson( "81.json" );
    }
    @Test
    public void json82() throws Exception
    {
        streamJson( "82.json" );
    }
    @Test
    public void json83() throws Exception
    {
        streamJson( "83.json" );
    }
    @Test
    public void json84() throws Exception
    {
        streamJson( "84.json" );
    }
    @Test
    public void json85() throws Exception
    {
        streamJson( "85.json" );
    }
    @Test
    public void json86() throws Exception
    {
        streamJson( "86.json" );
    }
    @Test
    public void json87() throws Exception
    {
        streamJson( "87.json" );
    }
    @Test
    public void json88() throws Exception
    {
        streamJson( "88.json" );
    }
    @Test
    public void json89() throws Exception
    {
        streamJson( "89.json" );
    }
    @Test
    public void json8a() throws Exception
    {
        streamJson( "8a.json" );
    }
    @Test
    public void json8b() throws Exception
    {
        streamJson( "8b.json" );
    }
    @Test
    public void json8c() throws Exception
    {
        streamJson( "8c.json" );
    }
    @Test
    public void json8d() throws Exception
    {
        streamJson( "8d.json" );
    }
    @Test
    public void json8e() throws Exception
    {
        streamJson( "8e.json" );
    }
    @Test
    public void json8f() throws Exception
    {
        streamJson( "8f.json" );
    }

    // json 90 - 9f

    @Test
    public void json90() throws Exception
    {
        streamJson( "90.json" );
    }
    @Test
    public void json91() throws Exception
    {
        streamJson( "91.json" );
    }
    @Test
    public void json92() throws Exception
    {
        streamJson( "92.json" );
    }
    @Test
    public void json93() throws Exception
    {
        streamJson( "93.json" );
    }
    @Test
    public void json94() throws Exception
    {
        streamJson( "94.json" );
    }
    @Test
    public void json95() throws Exception
    {
        streamJson( "95.json" );
    }
    @Test
    public void json96() throws Exception
    {
        streamJson( "96.json" );
    }
    @Test
    public void json97() throws Exception
    {
        streamJson( "97.json" );
    }
    @Test
    public void json98() throws Exception
    {
        streamJson( "98.json" );
    }
    @Test
    public void json99() throws Exception
    {
        streamJson( "99.json" );
    }
    @Test
    public void json9a() throws Exception
    {
        streamJson( "9a.json" );
    }
    @Test
    public void json9b() throws Exception
    {
        streamJson( "9b.json" );
    }
    @Test
    public void json9c() throws Exception
    {
        streamJson( "9c.json" );
    }
    @Test
    public void json9d() throws Exception
    {
        streamJson( "9d.json" );
    }
    @Test
    public void json9e() throws Exception
    {
        streamJson( "9e.json" );
    }
    @Test
    public void json9f() throws Exception
    {
        streamJson( "9f.json" );
    }

    // json a0 - af

    @Test
    public void jsona0() throws Exception
    {
        streamJson( "a0.json" );
    }
    @Test
    public void jsona1() throws Exception
    {
        streamJson( "a1.json" );
    }
    @Test
    public void jsona2() throws Exception
    {
        streamJson( "a2.json" );
    }
    @Test
    public void jsona3() throws Exception
    {
        streamJson( "a3.json" );
    }
    @Test
    public void jsona4() throws Exception
    {
        streamJson( "a4.json" );
    }
    @Test
    public void jsona5() throws Exception
    {
        streamJson( "a5.json" );
    }
    @Test
    public void jsona6() throws Exception
    {
        streamJson( "a6.json" );
    }
    @Test
    public void jsona7() throws Exception
    {
        streamJson( "a7.json" );
    }
    @Test
    public void jsona8() throws Exception
    {
        streamJson( "a8.json" );
    }
    @Test
    public void jsona9() throws Exception
    {
        streamJson( "a9.json" );
    }
    @Test
    public void jsonaa() throws Exception
    {
        streamJson( "aa.json" );
    }
    @Test
    public void jsonab() throws Exception
    {
        streamJson( "ab.json" );
    }
    @Test
    public void jsonac() throws Exception
    {
        streamJson( "ac.json" );
    }
    @Test
    public void jsonad() throws Exception
    {
        streamJson( "ad.json" );
    }
    @Test
    public void jsonae() throws Exception
    {
        streamJson( "ae.json" );
    }
    @Test
    public void jsonaf() throws Exception
    {
        streamJson( "af.json" );
    }

    // json b0 - bf

    @Test
    public void jsonb0() throws Exception
    {
        streamJson( "b0.json" );
    }
    @Test
    public void jsonb1() throws Exception
    {
        streamJson( "b1.json" );
    }
    @Test
    public void jsonb2() throws Exception
    {
        streamJson( "b2.json" );
    }
    @Test
    public void jsonb3() throws Exception
    {
        streamJson( "b3.json" );
    }
    @Test
    public void jsonb4() throws Exception
    {
        streamJson( "b4.json" );
    }
    @Test
    public void jsonb5() throws Exception
    {
        streamJson( "b5.json" );
    }
    @Test
    public void jsonb6() throws Exception
    {
        streamJson( "b6.json" );
    }
    @Test
    public void jsonb7() throws Exception
    {
        streamJson( "b7.json" );
    }
    @Test
    public void jsonb8() throws Exception
    {
        streamJson( "b8.json" );
    }
    @Test
    public void jsonb9() throws Exception
    {
        streamJson( "b9.json" );
    }
    @Test
    public void jsonba() throws Exception
    {
        streamJson( "ba.json" );
    }
    @Test
    public void jsonbb() throws Exception
    {
        streamJson( "bb.json" );
    }
    @Test
    public void jsonbc() throws Exception
    {
        streamJson( "bc.json" );
    }
    @Test
    public void jsonbd() throws Exception
    {
        streamJson( "bd.json" );
    }
    @Test
    public void jsonbe() throws Exception
    {
        streamJson( "be.json" );
    }
    @Test
    public void jsonbf() throws Exception
    {
        streamJson( "bf.json" );
    }

    // json c0 - cf

    @Test
    public void jsonc0() throws Exception
    {
        streamJson( "c0.json" );
    }
    @Test
    public void jsonc1() throws Exception
    {
        streamJson( "c1.json" );
    }
    @Test
    public void jsonc2() throws Exception
    {
        streamJson( "c2.json" );
    }
    @Test
    public void jsonc3() throws Exception
    {
        streamJson( "c3.json" );
    }
    @Test
    public void jsonc4() throws Exception
    {
        streamJson( "c4.json" );
    }
    @Test
    public void jsonc5() throws Exception
    {
        streamJson( "c5.json" );
    }
    @Test
    public void jsonc6() throws Exception
    {
        streamJson( "c6.json" );
    }
    @Test
    public void jsonc7() throws Exception
    {
        streamJson( "c7.json" );
    }
    @Test
    public void jsonc8() throws Exception
    {
        streamJson( "c8.json" );
    }
    @Test
    public void jsonc9() throws Exception
    {
        streamJson( "c9.json" );
    }
    @Test
    public void jsonca() throws Exception
    {
        streamJson( "ca.json" );
    }
    @Test
    public void jsoncb() throws Exception
    {
        streamJson( "cb.json" );
    }
    @Test
    public void jsoncc() throws Exception
    {
        streamJson( "cc.json" );
    }
    @Test
    public void jsoncd() throws Exception
    {
        streamJson( "cd.json" );
    }
    @Test
    public void jsonce() throws Exception
    {
        streamJson( "ce.json" );
    }
    @Test
    public void jsoncf() throws Exception
    {
        streamJson( "cf.json" );
    }

    // json d0 - df

    @Test
    public void jsond0() throws Exception
    {
        streamJson( "d0.json" );
    }
    @Test
    public void jsond1() throws Exception
    {
        streamJson( "d1.json" );
    }
    @Test
    public void jsond2() throws Exception
    {
        streamJson( "d2.json" );
    }
    @Test
    public void jsond3() throws Exception
    {
        streamJson( "d3.json" );
    }
    @Test
    public void jsond4() throws Exception
    {
        streamJson( "d4.json" );
    }
    @Test
    public void jsond5() throws Exception
    {
        streamJson( "d5.json" );
    }
    @Test
    public void jsond6() throws Exception
    {
        streamJson( "d6.json" );
    }
    @Test
    public void jsond7() throws Exception
    {
        streamJson( "d7.json" );
    }
    @Test
    public void jsond8() throws Exception
    {
        streamJson( "d8.json" );
    }
    @Test
    public void jsond9() throws Exception
    {
        streamJson( "d9.json" );
    }
    @Test
    public void jsonda() throws Exception
    {
        streamJson( "da.json" );
    }
    @Test
    public void jsondb() throws Exception
    {
        streamJson( "db.json" );
    }
    @Test
    public void jsondc() throws Exception
    {
        streamJson( "dc.json" );
    }
    @Test
    public void jsondd() throws Exception
    {
        streamJson( "dd.json" );
    }
    @Test
    public void jsonde() throws Exception
    {
        streamJson( "de.json" );
    }
    @Test
    public void jsondf() throws Exception
    {
        streamJson( "df.json" );
    }

    // json e0 - ef

    @Test
    public void jsone0() throws Exception
    {
        streamJson( "e0.json" );
    }
    @Test
    public void jsone1() throws Exception
    {
        streamJson( "e1.json" );
    }
    @Test
    public void jsone2() throws Exception
    {
        streamJson( "e2.json" );
    }
    @Test
    public void jsone3() throws Exception
    {
        streamJson( "e3.json" );
    }
    @Test
    public void jsone4() throws Exception
    {
        streamJson( "e4.json" );
    }
    @Test
    public void jsone5() throws Exception
    {
        streamJson( "e5.json" );
    }
    @Test
    public void jsone6() throws Exception
    {
        streamJson( "e6.json" );
    }
    @Test
    public void jsone7() throws Exception
    {
        streamJson( "e7.json" );
    }
    @Test
    public void jsone8() throws Exception
    {
        streamJson( "e8.json" );
    }
    @Test
    public void jsone9() throws Exception
    {
        streamJson( "e9.json" );
    }
    @Test
    public void jsonea() throws Exception
    {
        streamJson( "ea.json" );
    }
    @Test
    public void jsoneb() throws Exception
    {
        streamJson( "eb.json" );
    }
    @Test
    public void jsonec() throws Exception
    {
        streamJson( "ec.json" );
    }
    @Test
    public void jsoned() throws Exception
    {
        streamJson( "ed.json" );
    }
    @Test
    public void jsonee() throws Exception
    {
        streamJson( "ee.json" );
    }
    @Test
    public void jsonef() throws Exception
    {
        streamJson( "ef.json" );
    }

    // json f0 - ff

    @Test
    public void jsonf0() throws Exception
    {
        streamJson( "f0.json" );
    }
    @Test
    public void jsonf1() throws Exception
    {
        streamJson( "f1.json" );
    }
    @Test
    public void jsonf2() throws Exception
    {
        streamJson( "f2.json" );
    }
    @Test
    public void jsonf3() throws Exception
    {
        streamJson( "f3.json" );
    }
    @Test
    public void jsonf4() throws Exception
    {
        streamJson( "f4.json" );
    }
    @Test
    public void jsonf5() throws Exception
    {
        streamJson( "f5.json" );
    }
    @Test
    public void jsonf6() throws Exception
    {
        streamJson( "f6.json" );
    }
    @Test
    public void jsonf7() throws Exception
    {
        streamJson( "f7.json" );
    }
    @Test
    public void jsonf8() throws Exception
    {
        streamJson( "f8.json" );
    }
    @Test
    public void jsonf9() throws Exception
    {
        streamJson( "f9.json" );
    }
    @Test
    public void jsonfa() throws Exception
    {
        streamJson( "fa.json" );
    }
    @Test
    public void jsonfb() throws Exception
    {
        streamJson( "fb.json" );
    }
    @Test
    public void jsonfc() throws Exception
    {
        streamJson( "fc.json" );
    }
    @Test
    public void jsonfd() throws Exception
    {
        streamJson( "fd.json" );
    }
    @Test
    public void jsonfe() throws Exception
    {
        streamJson( "fe.json" );
    }
    @Test
    public void jsonff() throws Exception
    {
        streamJson( "ff.json" );
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

    public void streamJson( String filename ) throws Exception
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
        Gson gson = new Gson();

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

                    processTestRecord(record);

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

    private static void processTestRecord(CpuTestData.TestRecord record)
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

//        System.err.println("Processed: " + name + " | Cycles: " + record.cycles().size());
    }
}
