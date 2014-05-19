package net.bytebuddy.test.precompiled;

public interface SingleDefaultMethodManifestOverridingInterface extends SingleDefaultMethodInterface {

    static final String BAZ = "baz";

    @Override
    default Object foo() {
        return BAZ;
    }
}
