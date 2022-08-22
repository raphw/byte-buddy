package net.bytebuddy.android.test

open class AnotherClass {

    open fun someMethod(): String {
        return "Not instrumented"
    }
}