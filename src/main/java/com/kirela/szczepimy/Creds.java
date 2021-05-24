package com.kirela.szczepimy;

import picocli.CommandLine;

record Creds(String prescriptionId, String sid, String csrf) {
    public static class PicoConverter implements CommandLine.ITypeConverter<Creds> {

        @Override
        public Creds convert(String colonSeparated) {
            String[] split = colonSeparated.split(":");
            final int len = 3;
            if (split.length != len) {
                throw new IllegalArgumentException(
                    "Expecting %d args, got %d from : %s".formatted(len, split.length, colonSeparated)
                );
            }
            return new Creds(split[0], split[1], split[2]);
        }
    }
}
