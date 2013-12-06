package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

public abstract class AbstractIdInitializerAppender implements ByteCodeAppender {

    protected static final String UUID_TYPE_NAME = "java/util/UUID";
    protected static final String UUID_TYPE_NAME_INTERNAL_FORM = "Ljava/util/UUID;";
    protected static final String UUID_METHOD_NAME = "fromString";
    protected static final String UUID_METHOD_DESCRIPTION = "(Ljava/lang/String;)Ljava/util/UUID;";

    private final String targetClassName;
    private final String targetFieldName;

    public AbstractIdInitializerAppender(String idClassName, String idFieldName) {
        this.targetClassName = idClassName;
        this.targetFieldName = idFieldName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }
}
