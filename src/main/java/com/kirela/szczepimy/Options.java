package com.kirela.szczepimy;

import picocli.CommandLine;

public class Options {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @CommandLine.Option(names = "-p", description = "prescription ID (default: ${DEFAULT-VALUE})")
    String prescriptionId;

    @CommandLine.Option(names = "-s", description = "SID (default: ${DEFAULT-VALUE})")
    String sid;

    @CommandLine.Option(names = "-c", description = "CSRF (default: ${DEFAULT-VALUE})")
    String csrf;
}
