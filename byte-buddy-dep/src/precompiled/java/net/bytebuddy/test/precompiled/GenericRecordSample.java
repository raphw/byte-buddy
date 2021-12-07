package net.bytebuddy.test.precompiled;

public record GenericRecordSample<T>(T value) {

    public static final GenericRecordSample<String> FOO = new GenericRecordSample<String>("value");
}
