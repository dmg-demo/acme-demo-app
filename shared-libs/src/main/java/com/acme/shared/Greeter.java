package com.acme.shared;

public final class Greeter {
    private Greeter() {}

    public static String greet(String name) {
        return "Hello, " + name + " (shared-libs 1.2.0)";
    }
}
