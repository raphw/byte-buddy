package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Executable;

public class OriginExecutableWithCache {

    public Executable executable;

    public Executable intercept(@Origin(cache = true) Executable executable) {
        this.executable = executable;
        return executable;
    }
}
