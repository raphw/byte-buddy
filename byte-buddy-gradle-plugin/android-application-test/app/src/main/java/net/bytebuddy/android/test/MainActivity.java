package net.bytebuddy.android.test;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import net.bytebuddy.android.test.aar.lib.SomeAarClass;

public class MainActivity extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTextTo(R.id.text_from_local_java_class, new SomeClass().someMethod());
        setTextTo(R.id.text_from_aar_dependency, new SomeAarClass().someMethod());
        setTextTo(R.id.text_instrumented_from_aar, new AnotherClass().someMethod());
    }

    private void setTextTo(@IdRes int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }
}