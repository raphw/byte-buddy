package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Executable;

public class OriginExecutable {

    public Executable executable;

    public Executable intercept(@Origin(cache = false) Executable executable) {
        this.executable = executable;
        return executable;
    }
}
