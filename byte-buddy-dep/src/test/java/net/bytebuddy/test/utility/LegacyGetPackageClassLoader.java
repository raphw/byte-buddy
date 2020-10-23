package net.bytebuddy.test.utility;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Manifest;

/**
 * Simulates a classloader that checks if a package is already defined with a call to getPackage. This causes
 * definePackage to throw if an ancestor classloader already has it, and getDefinedPackage (JDK9+) is available since
 * getDefinedPackage is the preferred way to check if the package can be defined.
 */
public class LegacyGetPackageClassLoader extends URLClassLoader {
    public LegacyGetPackageClassLoader() {
        super(new URL[0], new URLClassLoader(new URL[0], null));
    }

    @Override
    protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        if (getPackage(name) != null) {
            throw new IllegalArgumentException(name);
        }
        return super.definePackage(name, man, url);
    }

    @Override
    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
        if (getPackage(name) != null) {
            throw new IllegalArgumentException(name);
        }
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }
}