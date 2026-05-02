package com.example.cloudcli.util;

public class SensitiveDataMasker {

    public static String mask(String input) {
        if (input == null) return null;

        String masked = input;

        // password=abc or password: abc
        masked = masked.replaceAll("(?i)(password\\s*[=:]\\s*)[^\\s,]+", "$1****");

        // pwd=abc
        masked = masked.replaceAll("(?i)(pwd\\s*[=:]\\s*)[^\\s,]+", "$1****");

        // --password=abc
        masked = masked.replaceAll("(?i)(--password\\s*=\\s*)[^\\s]+", "$1****");

        // MySQL style -pabc
        masked = masked.replaceAll("(?i)(-p)([^\\s]+)", "$1****");

        // URL style: user:password@
        masked = masked.replaceAll("(?i)(://[^:]+:)([^@]+)(@)", "$1****$3");

        return masked;
    }
}