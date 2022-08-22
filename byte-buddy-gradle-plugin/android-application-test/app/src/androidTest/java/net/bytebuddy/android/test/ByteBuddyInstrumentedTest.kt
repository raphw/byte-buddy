package net.bytebuddy.android.test

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ByteBuddyInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun bytecodeInstrumentation_on_local_java() {
        onView(withId(R.id.text_from_local_java_class)).check(matches(withText("Instrumented message in lib")))
    }

    @Test
    fun bytecodeInstrumentation_on_local_kotlin() {
        onView(withId(R.id.text_from_local_kotlin_class)).check(matches(withText("Instrumented message in lib")))
    }

    @Test
    fun bytecodeInstrumentation_on_dependency_class() {
        onView(withId(R.id.text_from_aar_dependency)).check(matches(withText("Instrumented message in lib")))
    }

    @Test
    fun bytecodeInstrumentation_from_aar_plugin() {
        onView(withId(R.id.text_instrumented_from_aar)).check(matches(withText("Instrumented message in lib")))
    }
}