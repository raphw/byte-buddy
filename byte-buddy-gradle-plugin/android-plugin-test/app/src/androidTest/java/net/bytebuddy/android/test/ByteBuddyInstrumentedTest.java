package net.bytebuddy.android.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.service.definition.ServiceDefinition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@RunWith(AndroidJUnit4.class)
public class ByteBuddyInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void bytecodeInstrumentation() {
        // On local java
        onView(withId(R.id.text_from_local_java_class)).check(matches(withText("bar")));
        // On dependency class
        onView(withId(R.id.text_from_aar_dependency)).check(matches(withText("bar")));
        // From aar
        onView(withId(R.id.text_instrumented_from_aar)).check(matches(withText("bar")));
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