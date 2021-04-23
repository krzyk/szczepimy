package com.kirela.szczepimy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import picocli.CommandLine;

public class Options {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @CommandLine.Option(required = true, names = "-p", description = "prescription ID (default: ${DEFAULT-VALUE})")
    String prescriptionId;

    @CommandLine.Option(names = "-v", description = "voivodeships, valid: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    Set<Voivodeship> voivodeships = new HashSet<>(Arrays.asList(Voivodeship.values()));

    @CommandLine.Option(required = true, names = "-s", description = "SID (default: ${DEFAULT-VALUE})")
    String sid;

    @CommandLine.Option(required = true, names = "-c", description = "CSRF (default: ${DEFAULT-VALUE})")
    String csrf;

    @CommandLine.Option(required = true, names = "-t", description = "Target directory for files (default: ${DEFAULT-VALUE})")
    String output;

    @CommandLine.Option(names = "-l", description = "Store json response logs? (default: ${DEFAULT-VALUE})")
    boolean storeLogs = false;
}
