module route64
{
//    exports de.michab.apps.route64;
    exports de.michab.simulator;
    exports de.michab.simulator.mos6502;
    exports de.michab.swingx to framework.smack;
//    exports de.michab.utils.tools;
//    exports de.michab.apps.route64.actions;
    exports de.michab.simulator.mos6502.c64;

    // Open for resource loading.
    opens de.michab.simulator.mos6502.c64.roms;
    opens de.michab.apps.route64.resources;

    requires framework.smack;

    requires java.desktop;
    requires java.logging;
}
