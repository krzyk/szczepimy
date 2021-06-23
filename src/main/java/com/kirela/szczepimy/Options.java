package com.kirela.szczepimy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import picocli.CommandLine;

public class Options {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;


    @CommandLine.Option(names = "-v", split = ",", description = "voivodeships, valid: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    Set<Voivodeship> voivodeships = new HashSet<>(Arrays.asList(Voivodeship.values()));

    @CommandLine.Option(names = "-u", split = ",", description = "vaccine types, valid: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    Set<VaccineType> vaccineTypes = new HashSet<>(Arrays.asList(VaccineType.values()));

    @CommandLine.Option(required = true, names = "--credentials", split = ",", converter = Creds.PicoConverter.class, description = "credentials (default: ${DEFAULT-VALUE})")
    List<Creds> credentials;

    @CommandLine.Option(required = true, names = "-t", description = "Target directory for files (default: ${DEFAULT-VALUE})")
    String output;

    @CommandLine.Option(names = "-l", description = "Store json response logs? (default: ${DEFAULT-VALUE})")
    boolean storeLogs = false;

    @CommandLine.Option(names = "-m", description = "Max retries if limited (default: ${DEFAULT-VALUE})")
    int retries = 50;

    @CommandLine.Option(names = "--wait", description = "Wait time between searches (default: ${DEFAULT-VALUE})")
    int wait = 1000;

    @CommandLine.Option(required = true, names = "--bot-key", description = "Telegram bot key (default: ${DEFAULT-VALUE})")
    String botKey;

    @CommandLine.Option(required = true, names = "--chat-id", description = "Telegram chat id (default: ${DEFAULT-VALUE})")
    String chatId;
}
