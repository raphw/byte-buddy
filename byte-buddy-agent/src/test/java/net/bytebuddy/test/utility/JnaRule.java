package net.bytebuddy.test.utility;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.logging.Logger;

public class JnaRule implements TestRule {

    private final boolean available;

    @SuppressWarnings("deprecation")
    public JnaRule() {
        boolean available;
        try {
            Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), CLibrary.class);
            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        this.available = available;
    }

    public Statement apply(Statement base, Description description) {
        return available
                ? base
                : new NoOpStatement();
    }

    private static class NoOpStatement extends Statement {

        public void evaluate() {
            Logger.getLogger("net.bytebuddy").info("Omitting test case: JNA not available");
        }
    }

    public interface CLibrary extends Library {

        @SuppressWarnings("unused")
        void printf(String format, Object... args);
    }
}
