/* $Id: C64Vic.java 11 2008-09-20 11:06:39Z binzm $
 *
 * Project: Route64
 *
 * Released under GPL (GNU public license)
 * Copyright (c) 2000-2003 Michael G. Binz
 */
package de.michab.simulator.mos6502.c64;

import de.michab.simulator.Clock;
import de.michab.simulator.Memory;
import de.michab.simulator.mos6502.*;



/**
 * Represents a Commodore 64's VIC.  This adds special character rom
 * addressing.
 *
 * @version $Revision: 11 $
 * @author Michael G. Binz
 */
class C64Vic
  extends
    Vic
{
  /**
   * Create a C64Vic.
   *
   * @param cpu The cpu controlling the system.
   * @param memory The memory the VIC operates on.
   * @param colorRamAddress The address of the color RAM.
   * @see Vic#Vic(Cpu6510, Memory, int, Clock)
   */
  C64Vic( Cpu6510 cpu, Memory memory, int colorRamAddress, Clock clock )
  {
    super( cpu, memory, colorRamAddress, clock );
  }



  /**
   * Implements the c64's special mapping for the character rom.  That means
   * if a character set address of 0x1000 is set, this is really read from
   * RAM below the IO page.  This is hard wired C64 logic.
   *
   * @param charadr The character set address.
   */
  public void setAddresses( int charadr, int bitmap, int videoram )
  {
    int page = 0xf000 & charadr;

    if ( 0x1000 == page || 0x9000 == page )
      charadr = C64Memory.ADR_CHAR + (charadr & 0x0fff);

    super.setAddresses( charadr, bitmap, videoram );
  }
}

