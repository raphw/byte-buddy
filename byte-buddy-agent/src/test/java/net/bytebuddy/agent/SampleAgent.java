package net.bytebuddy.agent;

public class SampleAgent {

    public static String argument;

    public static void agentmain(String argument) {
        SampleAgent.argument = argument;
    }
}
