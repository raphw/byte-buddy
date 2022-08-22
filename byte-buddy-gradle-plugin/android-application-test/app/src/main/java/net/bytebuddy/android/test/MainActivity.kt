package net.bytebuddy.android.test

import android.os.Bundle
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTextTo(R.id.text_from_local_java_class, SomeClass().someMethod())
//        setTextTo(R.id.text_from_local_kotlin_class, SomeKotlinClass().someMethod())
//        setTextTo(R.id.text_from_aar_dependency, AppCompatActivity().toString())
    }

    private fun setTextTo(@IdRes id: Int, text: String) {
        findViewById<TextView>(id).text = text
    }
}