package edu.uet.battlecode.runner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TestBotMain {
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        if (!mode.equals("no-ready")) {
            System.out.println("READY");
            System.out.flush();
        }
        if (mode.equals("no-ready")) { Thread.sleep(4_000); return; }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (input.readLine() != null) {
            switch (mode) {
                case "malformed" -> System.out.println("not-json");
                case "oversized" -> System.out.println("x".repeat(17_000));
                case "timeout" -> { Thread.sleep(4_000); System.out.println("{}"); }
                case "stderr" -> {
                    System.err.println("debug".repeat(4_000));
                    System.err.flush();
                    System.out.println("{\"actions\":[{\"unit_id\":1,\"type\":\"WAIT\"}]}");
                }
                default -> System.out.println("{\"actions\":[{\"unit_id\":1,\"type\":\"WAIT\"}]}");
            }
            System.out.flush();
        }
    }
}
