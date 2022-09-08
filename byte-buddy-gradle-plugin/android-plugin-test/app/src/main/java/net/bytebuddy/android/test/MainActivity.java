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

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import net.bytebuddy.android.test.aar.lib.SomeAarClass;

/**
 * The main activity to use under test.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * {@inheritDoc}
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTextTo(R.id.text_from_local_java_class, new SomeClass().someMethod());
        setTextTo(R.id.text_from_aar_dependency, new SomeAarClass().someMethod());
        setTextTo(R.id.text_instrumented_from_aar, new AnotherClass().someMethod());
    }

    /**
     * Sets a resource text to a given value.
     *
     * @param id   The text id.
     * @param text The text to set.
     */
    private void setTextTo(@IdRes int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }
}
