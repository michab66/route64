/* $Id: DisasmROM.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.utils.tools;

import de.michab.simulator.mos6502.Opcodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.smack.application.CliApplication;



/**
 *  Disassemble ROM-Files.
 *
 *  @author Stefan K&uuml;hnel
 */
public class DisasmROM extends CliApplication {

    private byte buffer[]=null;

    /**
     *
     */
    public DisasmROM (String file,int offset) {
        int length=0;
        File f=new File(file);
        FileInputStream fis=null;
        if (f!=null) {
            length=(int)f.length();
            buffer=new byte[length];
            try {
                fis=new FileInputStream(f);
                fis.read(buffer);
                fis.close();

            } catch (IOException ioex) {
                System.err.println ("Aborting due to I/O-Exception...");
                System.exit(1);
            }
            int i=0;
            int opcode=0;
            int ocl=0;
            String op=null;
            int addr=offset;
            String ausgabe=null;
            String codes="";

            while(i<length) {
                op = Opcodes.decode(i,buffer);
                opcode=byte2int(buffer[i]);
                addr=offset+i;

                System.out.print (Integer.toHexString(addr));
                if (op==null) {
                    int val=byte2int(buffer[i]);
                    ausgabe="DATA "+Integer.toHexString(val);
                } else {
                    ausgabe=op;
                }

                ocl=Opcodes.getEncodingLength( opcode );
                codes="";

                for (int j=0;j<3;j++) {
                    if (j<ocl) {
                        codes=codes+"\t"+
                            Integer.toHexString(byte2int(buffer[i+j]));
                    } else {
                        codes=codes+"\t";
                    }
                }

                System.out.println (codes+"\t"+ausgabe);
                if (ocl>0) {
                    i+=ocl;
                } else {
                    i++;
                }

            }

        }
    }



  /**
   * Converts an unsigned byte into an int without propagating the sign.
   *
   * @param b The byte to convert.
   * @return The converted value.
   */
  private static int byte2int(byte b)
  {
    return b & 0xff;
  }



    /**
     *
     */
    public static void main(String argv[]) {
        String file=null;
        String offs=null;
        int offset=0xE000;

        if (argv.length<1) {
            System.out.println ("Usage: DisasmROM <FILE> [<Offset>]");
            System.exit(0);
        } else if(argv.length>1) {
            file=argv[0];
            offs=argv[1];
        } else {
            file=argv[0];
        }
        if (offs!=null) {
            try {
                offset=Integer.parseInt(offs,16);
            } catch (NumberFormatException nfe) {
                System.out.println ("Offset could not be parsed. Using "+Integer.toHexString(offset));
            }
        }
        new DisasmROM(file,offset);

    }
}
