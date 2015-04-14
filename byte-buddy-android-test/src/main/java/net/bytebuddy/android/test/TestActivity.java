package net.bytebuddy.android.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This activity allows to run a code generation on an Android device.
 */
public class TestActivity extends Activity {

    /**
     * A sample String to be returned by an instrumented {@link Object#toString()} method.
     */
    private static final String FOO = "foo";

    /**
     * The tag to be used for Android's log messages.
     */
    private static final String BYTE_BUDDY_TAG = "net.bytebuddy";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button runTest = (Button) findViewById(R.id.run_test);
        runTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteBuddy byteBuddy;
                try {
                    byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V6);
                } catch (Throwable e) {
                    Log.w(BYTE_BUDDY_TAG, e);
                    Toast.makeText(TestActivity.this, "Failure: Could not create Byte Buddy instance. (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    File file = TestActivity.this.getDir(RandomString.make(), Context.MODE_PRIVATE);
                    if (!file.isDirectory()) {
                        throw new IOException("Not a directory: " + file);
                    }
                    DynamicType.Loaded<?> dynamicType;
                    try {
                        dynamicType = byteBuddy.subclass(Object.class)
                                .method(named("toString")).intercept(MethodDelegation.to(Interceptor.class))
                                .make()
                                .load(TestActivity.class.getClassLoader(), new AndroidClassLoadingStrategy(file));
                    } catch (Throwable e) {
                        Log.w(BYTE_BUDDY_TAG, e);
                        Toast.makeText(TestActivity.this, "Failure: Could not load dynamic type. (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        String value = dynamicType.getLoaded().newInstance().toString();
                        Toast.makeText(TestActivity.this,
                                FOO.equals(value)
                                        ? "Success: Created type and verified instrumentation."
                                        : "Failure: Expected different value by instrumented method. (was: " + value + ")",
                                Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        Log.w(BYTE_BUDDY_TAG, e);
                        Toast.makeText(TestActivity.this, "Failure: Could create dynamic instance. (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    }
                } catch (Throwable e) {
                    Log.w(BYTE_BUDDY_TAG, e);
                    Toast.makeText(TestActivity.this, "Failure: Could not create temporary file. (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * An interceptor to be used in the instrumentation of the {@link Object#toString()} method. Of course, this
     * could also be achieved by using a {@link net.bytebuddy.implementation.FixedValue} instrumentation. However,
     * the instrumentation should generate an {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}
     * to validate their functionality.
     */
    public static class Interceptor {

        /**
         * The interception method to be applied.
         *
         * @param zuper A proxy to call the super method to validate the functioning og creating an auxiliary type.
         * @return The value to be returned by the instrumented {@link Object#toString()} method.
         * @throws Exception If an exception occurs.
         */
        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            String toString = zuper.call();
            if (toString.equals(FOO)) {
                throw new IllegalStateException("Super call proxy invocation did not derive in its value");
            }
            return FOO;
        }
    }
}

