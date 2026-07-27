package de.michab.simulator;

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

    public byte read( int address )
    {
        return (byte) _memory[ address ];
    }

    @Override
    public void write(int address, byte value) {
        _memory[ address ] = value;
    }

    @Override
    public void set(Forwarder f, int where) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'set'");
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

        int hi = read( Memory.mask16( address+1 ) );
        hi &= 0xff;
        int lo = read( Memory.mask16( address ) );
        lo &= 0xff;
        return (hi << 8) | lo;
    }
}
