/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.android.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
        TextView mavenInfo = (TextView) findViewById(R.id.maven_info);
        String version = "n/a";
        try {
            InputStream inputStream = TestActivity.class.getClassLoader().getResourceAsStream("maven.properties");
            if (inputStream != null) {
                try {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    version = properties.getProperty("version", version);
                } finally {
                    inputStream.close();
                }
            }
        } catch (Exception exception) {
            Log.i(BYTE_BUDDY_TAG, "Could not read version", exception);
            Toast.makeText(TestActivity.this, "Warning: Could not read version property. (" + exception.getMessage() + ")", Toast.LENGTH_SHORT).show();
        }
        mavenInfo.setText(getResources().getString(R.string.version_info, version));
        Button runTestWrapping = (Button) findViewById(R.id.run_test_wrapping);
        runTestWrapping.setOnClickListener(new TestRun(new StrategyCreator.Wrapping()));
        Button runTestInjecting = (Button) findViewById(R.id.run_test_injecting);
        runTestInjecting.setOnClickListener(new TestRun(new StrategyCreator.Injecting()));
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

    /**
     * A test run for the sample Android application.
     */
    private class TestRun implements View.OnClickListener {

        /**
         * The strategy creator to use.
         */
        private final StrategyCreator strategyCreator;

        /**
         * Creates a new test run listener.
         *
         * @param strategyCreator The strategy creator to use.
         */
        private TestRun(StrategyCreator strategyCreator) {
            this.strategyCreator = strategyCreator;
        }

        @Override
        public void onClick(View view) {
            ByteBuddy byteBuddy;
            try {
                byteBuddy = new ByteBuddy();
            } catch (Throwable throwable) {
                Log.w(BYTE_BUDDY_TAG, throwable);
                Toast.makeText(TestActivity.this, "Failure: Could not create Byte Buddy instance. (" + throwable.getMessage() + ")", Toast.LENGTH_LONG).show();
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
                            .load(TestActivity.class.getClassLoader(), strategyCreator.make(file));
                } catch (Throwable throwable) {
                    Log.w(BYTE_BUDDY_TAG, throwable);
                    Toast.makeText(TestActivity.this, "Failure: Could not load dynamic type. (" + throwable.getMessage() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String value = dynamicType.getLoaded().newInstance().toString();
                    Toast.makeText(TestActivity.this,
                            FOO.equals(value)
                                    ? "Success: Created type and verified instrumentation."
                                    : "Failure: Expected different value by instrumented method. (was: " + value + ")",
                            Toast.LENGTH_LONG).show();
                } catch (Throwable throwable) {
                    Log.w(BYTE_BUDDY_TAG, throwable);
                    Toast.makeText(TestActivity.this, "Failure: Could create dynamic instance. (" + throwable.getMessage() + ")", Toast.LENGTH_LONG).show();
                }
            } catch (Throwable throwable) {
                Log.w(BYTE_BUDDY_TAG, throwable);
                Toast.makeText(TestActivity.this, "Failure: Could not create temporary file. (" + throwable.getMessage() + ")", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * A strategy creator for Android.
     */
    private interface StrategyCreator {

        /**
         * Creates an Android class loading strategy.
         *
         * @param file The private folder to use.
         * @return The class loading strategy to use.
         */
        AndroidClassLoadingStrategy make(File file);

        /**
         * A creator for creating a wrapping strategy.
         */
        class Wrapping implements StrategyCreator {

            @Override
            public AndroidClassLoadingStrategy make(File file) {
                return new AndroidClassLoadingStrategy.Wrapping(file);
            }
        }

        /**
         * A creator for creating an injecting strategy.
         */
        class Injecting implements StrategyCreator {

            @Override
            public AndroidClassLoadingStrategy make(File file) {
                return new AndroidClassLoadingStrategy.Injecting(file);
            }
        }
    }
}

