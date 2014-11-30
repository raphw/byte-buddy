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
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TestActivity extends Activity {

    private static final String FOO = "foo";

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
                    Toast.makeText(TestActivity.this, "Failure: Could not create Byte Buddy instance ("
                            + e.getMessage() + ")", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(TestActivity.this, "Failure: Could not load dynamic type ("
                                + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        String value = dynamicType.getLoaded().newInstance().toString();
                        Toast.makeText(TestActivity.this,
                                FOO.equals(value)
                                        ? "Success: Created type and verified instrumentation"
                                        : "Failure: Expected different value by instrumented method (was: " + value + ")",
                                Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        Log.w(BYTE_BUDDY_TAG, e);
                        Toast.makeText(TestActivity.this, "Failure: Could create dynamic instance ("
                                + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    }
                } catch (Throwable e) {
                    Log.w(BYTE_BUDDY_TAG, e);
                    Toast.makeText(TestActivity.this, "Failure: Could not create temporary file ("
                            + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static class Interceptor {

        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            String toString = zuper.call();
            if (toString.equals(FOO)) {
                throw new IllegalStateException("Super call proxy invocation did not derive in its value");
            }
            return FOO;
        }
    }
}

