package de.michab.simulator;

import java.util.Collections;

public class TestMemory implements Memory
{
    // TODO transform to byte[].
    private final int[] _memory;

    public TestMemory( int size )
    {
        _memory = new int[ size ];

        java.util.Arrays.fill( _memory, 0 );
    }
    public TestMemory()
    {
        this( 0x10000 );
    }

    public record IO(int address, int value, boolean isWrite) {
        public String toString() {
            return String.format(
                "Cycle[address=0x%04X, value=0x%02X, operation=%s]",
                address(),
                value(),
                isWrite() ? "write" : "read");
        }
    }

    private final java.util.List<IO> _cycles = new java.util.ArrayList<>();

    public byte read( int address )
    {
        _cycles.add(new IO(address, _memory[address], false));
        return (byte) _memory[ address ];
    }

    @Override
    public void write(int address, byte value) {
        _memory[ address ] = Memory.mask8(value);
        _cycles.add(new IO(address, Memory.mask8(value), true));
    }

    @Override
    public void set(Forwarder f, int where) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'set'");
    }

    public void resetCycles() {
        _cycles.clear();
    }

    public java.util.List<IO> getCycles() {
        return new java.util.ArrayList<>(_cycles);
    }

    @Override
    public int getSize() {
        return _memory.length;
    }

    @Override
    public byte[] getRawMemory() {
        byte[] result = new byte[_memory.length];
        for (int i = 0; i < _memory.length; i++) {
            result[i] = (byte) _memory[i];
        }
        return result;
    }

    @Override
    public int getVectorAt(int address) {

        int lo = read( Memory.mask16( address ) );
        lo &= 0xff;
        int hi = read( Memory.mask16( address+1 ) );
        hi &= 0xff;
        return (hi << 8) | lo;
    }
}
