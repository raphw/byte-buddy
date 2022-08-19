package net.bytebuddy.android.test

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.text).text = SomeClass().someMethod()
    }
}