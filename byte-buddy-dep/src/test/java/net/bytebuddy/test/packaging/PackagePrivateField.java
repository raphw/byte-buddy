package net.bytebuddy.test.packaging;

@SuppressWarnings("unused")
public class PackagePrivateField {

    public static final String PROTECTED_FIELD_NAME = "foo";

    public static final String PACKAGE_PRIVATE_FIELD_NAME = "bar";

    public static final String PRIVATE_FIELD_NAME = "qux";

    public static final String FIELD_VALUE = "baz";

    protected String foo = FIELD_VALUE;

    String bar = FIELD_VALUE;

    private String qux = FIELD_VALUE;
}
