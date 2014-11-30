package net.bytebuddy.android.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.FixedValue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TestActivity extends Activity {

    private static final String FOO = "foo", TEMP = "tmp";

    private static final String LOG_TAG = "net.bytebuddy";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        boolean isAvailable = AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.INSTANCE.isAvailable();
        Button runTest = (Button) findViewById(R.id.run_test);
        TextView noDxJar = (TextView) findViewById(R.id.no_dx_jar);
        if (!isAvailable) {
            noDxJar.setVisibility(0);
        }
        runTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteBuddy byteBuddy;
                try {
                    byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V7);
                } catch (Throwable e) {
                    Log.w(LOG_TAG, e);
                    Toast.makeText(TestActivity.this, "Failure: Could not create Byte Buddy instance (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                File file = null;
                try {
                    file = new File(Environment.getExternalStorageDirectory(), "byte-buddy" + File.separator + UUID.randomUUID().toString());
                    if (!file.mkdirs()) {
                        throw new IOException("Cannot create directory: " + file);
                    }
                    DynamicType.Loaded<?> dynamicType;
                    try {
                        dynamicType = byteBuddy.subclass(Object.class)
                                .method(named("toString")).intercept(FixedValue.value(FOO))
                                .make()
                                .load(TestActivity.class.getClassLoader(), new AndroidClassLoadingStrategy(file));
                    } catch (Throwable e) {
                        Log.w(LOG_TAG, e);
                        Toast.makeText(TestActivity.this, "Failure: Could not load dynamic type (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        String value = dynamicType.getLoaded().newInstance().toString();
                        Toast.makeText(TestActivity.this, FOO.equals(value)
                                ? "Success: Created type and verified instrumentation"
                                : "Failure: Expected different value by instrumented method (was: " + value + ")", Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        Log.w(LOG_TAG, e);
                        Toast.makeText(TestActivity.this, "Failure: Could create dynamic instance (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    }
                } catch (Throwable e) {
                    Log.w(LOG_TAG, e);
                    Toast.makeText(TestActivity.this, "Failure: Could not create temporary file (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                } finally {
                    if (file != null && !file.delete()) {
                        Log.w(LOG_TAG, "Cannot delete folder: " + file);
                    }
                }
            }
        });
        runTest.setEnabled(isAvailable);
    }
}

