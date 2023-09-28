package net.bytebuddy.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.service.definition.ServiceDefinition;

import net.bytebuddy.android.test.aar.lib.SomeAarClass;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ByteBuddyInstrumentedTest {

    @Test
    public void bytecodeInstrumentation() {
        // On local java
        assertEquals("bar", new SomeClass().method());
        // On dependency class
        assertEquals("bar", new SomeAarClass().method());
        // From aar
        assertEquals("bar", new AnotherClass().method());
    }

    @Test
    public void verifySPIsAreNotMissing() {
        List<String> values = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : ServiceLoader.load(ServiceDefinition.class)) {
            values.add(serviceDefinition.getValue());
        }

        assertEquals(3, values.size());
        assertTrue(values.contains("Target service impl"));
        assertTrue(values.contains("Service implementation"));
        assertTrue(values.contains("Service implementation2"));
    }
}