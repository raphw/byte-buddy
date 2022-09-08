package net.bytebuddy.android.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
class ByteBuddyInstrumentedTest {

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
}