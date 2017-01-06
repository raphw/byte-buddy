package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A package definer is responsible for defining a package's properties when a class of a new package is loaded. Also,
 * a package definer can choose not to define a package at all.
 */
public interface PackageDefinitionStrategy {

    /**
     * Returns a package definition for a given package.
     *
     * @param classLoader The class loader for which this package is being defined.
     * @param packageName The name of the package.
     * @param typeName    The name of the type being loaded that triggered the package definition.
     * @return A definition of the package.
     */
    Definition define(ClassLoader classLoader, String packageName, String typeName);

    /**
     * A package definer that does not define any package.
     */
    enum NoOp implements PackageDefinitionStrategy {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Definition define(ClassLoader classLoader, String packageName, String typeName) {
            return Definition.Undefined.INSTANCE;
        }
    }

    /**
     * A package definer that only defines packages without any meta data.
     */
    enum Trivial implements PackageDefinitionStrategy {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Definition define(ClassLoader classLoader, String packageName, String typeName) {
            return Definition.Trivial.INSTANCE;
        }
    }

    /**
     * A definition of a package.
     */
    interface Definition {

        /**
         * Indicates if a package should be defined at all.
         *
         * @return {@code true} if the package is to be defined.
         */
        boolean isDefined();

        /**
         * Returns the package specification's title or {@code null} if no such title exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package specification's title.
         */
        String getSpecificationTitle();

        /**
         * Returns the package specification's version or {@code null} if no such version exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package specification's version.
         */
        String getSpecificationVersion();

        /**
         * Returns the package specification's vendor or {@code null} if no such vendor exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package specification's vendor.
         */
        String getSpecificationVendor();

        /**
         * Returns the package implementation's title or {@code null} if no such title exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package implementation's title.
         */
        String getImplementationTitle();

        /**
         * Returns the package implementation's version or {@code null} if no such version exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package implementation's version.
         */
        String getImplementationVersion();

        /**
         * Returns the package implementation's vendor or {@code null} if no such vendor exists. This method must only be called
         * for defined package definitions.
         *
         * @return The package implementation's vendor.
         */
        String getImplementationVendor();

        /**
         * The URL representing the seal base. This method must only be called for defined package definitions.
         *
         * @return The seal base of the package.
         */
        URL getSealBase();

        /**
         * Validates that this package definition is compatible to a previously defined package. This method must only be
         * called for defined package definitions.
         *
         * @param definedPackage The previously defined package.
         * @return {@code false} if this package and the defined package's sealing information are not compatible.
         */
        boolean isCompatibleTo(Package definedPackage);

        /**
         * A canonical implementation of an undefined package.
         */
        enum Undefined implements Definition {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isDefined() {
                return false;
            }

            @Override
            public String getSpecificationTitle() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public String getSpecificationVersion() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public String getSpecificationVendor() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public String getImplementationTitle() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public String getImplementationVersion() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public String getImplementationVendor() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public URL getSealBase() {
                throw new IllegalStateException("Cannot read property of undefined package");
            }

            @Override
            public boolean isCompatibleTo(Package definedPackage) {
                throw new IllegalStateException("Cannot check compatibility to undefined package");
            }
        }

        /**
         * A package definer that defines packages without any meta data.
         */
        enum Trivial implements Definition {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * An empty value of a package's property.
             */
            private static final String NO_VALUE = null;

            /**
             * Represents an unsealed package.
             */
            private static final URL NOT_SEALED = null;

            @Override
            public boolean isDefined() {
                return true;
            }

            @Override
            public String getSpecificationTitle() {
                return NO_VALUE;
            }

            @Override
            public String getSpecificationVersion() {
                return NO_VALUE;
            }

            @Override
            public String getSpecificationVendor() {
                return NO_VALUE;
            }

            @Override
            public String getImplementationTitle() {
                return NO_VALUE;
            }

            @Override
            public String getImplementationVersion() {
                return NO_VALUE;
            }

            @Override
            public String getImplementationVendor() {
                return NO_VALUE;
            }

            @Override
            public URL getSealBase() {
                return NOT_SEALED;
            }

            @Override
            public boolean isCompatibleTo(Package definedPackage) {
                return true;
            }
        }

        /**
         * A simple package definition where any property is represented by a value.
         */
        class Simple implements Definition {

            /**
             * The seal base or {@code null} if the package is not sealed.
             */
            protected final URL sealBase;

            /**
             * The package specification's title or {@code null} if no such title exists.
             */
            private final String specificationTitle;

            /**
             * The package specification's version or {@code null} if no such version exists.
             */
            private final String specificationVersion;

            /**
             * The package specification's vendor or {@code null} if no such vendor exists.
             */
            private final String specificationVendor;

            /**
             * The package implementation's title or {@code null} if no such title exists.
             */
            private final String implementationTitle;

            /**
             * The package implementation's version or {@code null} if no such version exists.
             */
            private final String implementationVersion;

            /**
             * The package implementation's vendor or {@code null} if no such vendor exists.
             */
            private final String implementationVendor;

            /**
             * Creates a new simple package definition.
             *
             * @param specificationTitle    The package specification's title or {@code null} if no such title exists.
             * @param specificationVersion  The package specification's version or {@code null} if no such version exists.
             * @param specificationVendor   The package specification's vendor or {@code null} if no such vendor exists.
             * @param implementationTitle   The package implementation's title or {@code null} if no such title exists.
             * @param implementationVersion The package implementation's version or {@code null} if no such version exists.
             * @param implementationVendor  The package implementation's vendor or {@code null} if no such vendor exists.
             * @param sealBase              The seal base or {@code null} if the package is not sealed.
             */
            public Simple(String specificationTitle,
                          String specificationVersion,
                          String specificationVendor,
                          String implementationTitle,
                          String implementationVersion,
                          String implementationVendor,
                          URL sealBase) {
                this.specificationTitle = specificationTitle;
                this.specificationVersion = specificationVersion;
                this.specificationVendor = specificationVendor;
                this.implementationTitle = implementationTitle;
                this.implementationVersion = implementationVersion;
                this.implementationVendor = implementationVendor;
                this.sealBase = sealBase;
            }

            @Override
            public boolean isDefined() {
                return true;
            }

            @Override
            public String getSpecificationTitle() {
                return specificationTitle;
            }

            @Override
            public String getSpecificationVersion() {
                return specificationVersion;
            }

            @Override
            public String getSpecificationVendor() {
                return specificationVendor;
            }

            @Override
            public String getImplementationTitle() {
                return implementationTitle;
            }

            @Override
            public String getImplementationVersion() {
                return implementationVersion;
            }

            @Override
            public String getImplementationVendor() {
                return implementationVendor;
            }

            @Override
            public URL getSealBase() {
                return sealBase;
            }

            @Override
            public boolean isCompatibleTo(Package definedPackage) {
                if (sealBase == null) {
                    return !definedPackage.isSealed();
                } else {
                    return definedPackage.isSealed(sealBase);
                }
            }

            @Override
            @SuppressFBWarnings(value = "DMI_BLOCKING_METHODS_ON_URL", justification = "Package sealing relies on URL equality")
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Simple simple = (Simple) other;
                return !(specificationTitle != null ? !specificationTitle.equals(simple.specificationTitle) : simple.specificationTitle != null)
                        && !(specificationVersion != null ? !specificationVersion.equals(simple.specificationVersion) : simple.specificationVersion != null)
                        && !(specificationVendor != null ? !specificationVendor.equals(simple.specificationVendor) : simple.specificationVendor != null)
                        && !(implementationTitle != null ? !implementationTitle.equals(simple.implementationTitle) : simple.implementationTitle != null)
                        && !(implementationVersion != null ? !implementationVersion.equals(simple.implementationVersion) : simple.implementationVersion != null)
                        && !(implementationVendor != null ? !implementationVendor.equals(simple.implementationVendor) : simple.implementationVendor != null)
                        && !(sealBase != null ? !sealBase.equals(simple.sealBase) : simple.sealBase != null);
            }

            @Override
            @SuppressFBWarnings(value = "DMI_BLOCKING_METHODS_ON_URL", justification = "Package sealing relies on URL equality")
            public int hashCode() {
                int result = specificationTitle != null ? specificationTitle.hashCode() : 0;
                result = 31 * result + (specificationVersion != null ? specificationVersion.hashCode() : 0);
                result = 31 * result + (specificationVendor != null ? specificationVendor.hashCode() : 0);
                result = 31 * result + (implementationTitle != null ? implementationTitle.hashCode() : 0);
                result = 31 * result + (implementationVersion != null ? implementationVersion.hashCode() : 0);
                result = 31 * result + (implementationVendor != null ? implementationVendor.hashCode() : 0);
                result = 31 * result + (sealBase != null ? sealBase.hashCode() : 0);
                return result;
            }
        }
    }

    /**
     * A package definer that reads a class loader's manifest file.
     */
    @EqualsAndHashCode
    class ManifestReading implements PackageDefinitionStrategy {

        /**
         * The path to the manifest file.
         */
        private static final String MANIFEST_FILE = "/META-INF/MANIFEST.MF";

        /**
         * A URL defined a non-sealed package.
         */
        private static final URL NOT_SEALED = null;

        /**
         * Contains all attributes that are relevant for defining a package.
         */
        private static final Attributes.Name[] ATTRIBUTE_NAMES = new Attributes.Name[]{
                Attributes.Name.SPECIFICATION_TITLE,
                Attributes.Name.SPECIFICATION_VERSION,
                Attributes.Name.SPECIFICATION_VENDOR,
                Attributes.Name.IMPLEMENTATION_TITLE,
                Attributes.Name.IMPLEMENTATION_VERSION,
                Attributes.Name.IMPLEMENTATION_VENDOR,
                Attributes.Name.SEALED
        };

        /**
         * A locator for a sealed package's URL.
         */
        private final SealBaseLocator sealBaseLocator;

        /**
         * Creates a manifest reading package definition strategy that attempts to extract sealing information from a defined class's URL.
         */
        public ManifestReading() {
            this(new SealBaseLocator.ForTypeResourceUrl());
        }

        /**
         * Creates a new package definer that reads a class loader's manifest file.
         *
         * @param sealBaseLocator A locator for a sealed package's URL.
         */
        public ManifestReading(SealBaseLocator sealBaseLocator) {
            this.sealBaseLocator = sealBaseLocator;
        }

        @Override
        public Definition define(ClassLoader classLoader, String packageName, String typeName) {
            InputStream inputStream = classLoader.getResourceAsStream(MANIFEST_FILE);
            if (inputStream != null) {
                try {
                    try {
                        Manifest manifest = new Manifest(inputStream);
                        Map<Attributes.Name, String> values = new HashMap<Attributes.Name, String>();
                        Attributes mainAttributes = manifest.getMainAttributes();
                        if (mainAttributes != null) {
                            for (Attributes.Name attributeName : ATTRIBUTE_NAMES) {
                                values.put(attributeName, mainAttributes.getValue(attributeName));
                            }
                        }
                        Attributes attributes = manifest.getAttributes(packageName.replace('.', '/').concat("/"));
                        if (attributes != null) {
                            for (Attributes.Name attributeName : ATTRIBUTE_NAMES) {
                                String value = attributes.getValue(attributeName);
                                if (value != null) {
                                    values.put(attributeName, value);
                                }
                            }
                        }
                        return new Definition.Simple(values.get(Attributes.Name.SPECIFICATION_TITLE),
                                values.get(Attributes.Name.SPECIFICATION_VERSION),
                                values.get(Attributes.Name.SPECIFICATION_VENDOR),
                                values.get(Attributes.Name.IMPLEMENTATION_TITLE),
                                values.get(Attributes.Name.IMPLEMENTATION_VERSION),
                                values.get(Attributes.Name.IMPLEMENTATION_VENDOR),
                                Boolean.parseBoolean(values.get(Attributes.Name.SEALED))
                                        ? sealBaseLocator.findSealBase(classLoader, typeName)
                                        : NOT_SEALED);
                    } finally {
                        inputStream.close();
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Error while reading manifest file", exception);
                }
            } else {
                return Definition.Trivial.INSTANCE;
            }
        }

        /**
         * A locator for a seal base URL.
         */
        public interface SealBaseLocator {

            /**
             * Locates the URL that should be used for sealing a package.
             *
             * @param classLoader The class loader loading the package.
             * @param typeName    The name of the type being loaded that triggered the package definition.
             * @return The URL that is used for sealing a package or {@code null} if the package should not be sealed.
             */
            URL findSealBase(ClassLoader classLoader, String typeName);

            /**
             * A seal base locator that never seals a package.
             */
            enum NonSealing implements SealBaseLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public URL findSealBase(ClassLoader classLoader, String typeName) {
                    return NOT_SEALED;
                }
            }

            /**
             * A seal base locator that seals all packages with a fixed URL.
             */
            class ForFixedValue implements SealBaseLocator {

                /**
                 * The seal base URL.
                 */
                private final URL sealBase;

                /**
                 * Creates a new seal base locator for a fixed URL.
                 *
                 * @param sealBase The seal base URL.
                 */
                public ForFixedValue(URL sealBase) {
                    this.sealBase = sealBase;
                }

                @Override
                public URL findSealBase(ClassLoader classLoader, String typeName) {
                    return sealBase;
                }

                @Override
                @SuppressFBWarnings(value = "DMI_BLOCKING_METHODS_ON_URL", justification = "Package sealing relies on URL equality")
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && sealBase.equals(((ForFixedValue) other).sealBase);
                }

                @Override
                @SuppressFBWarnings(value = "DMI_BLOCKING_METHODS_ON_URL", justification = "Package sealing relies on URL equality")
                public int hashCode() {
                    return sealBase.hashCode();
                }
            }

            /**
             * A seal base locator that imitates the behavior of a {@link java.net.URLClassLoader}, i.e. tries
             * to deduct the base from a class's resource URL.
             */
            @EqualsAndHashCode
            class ForTypeResourceUrl implements SealBaseLocator {

                /**
                 * An index to indicate to a {@link String} manipulation that the initial slash should be excluded.
                 */
                private static final int EXCLUDE_INITIAL_SLASH = 1;

                /**
                 * The file extension for a class file.
                 */
                private static final String CLASS_FILE_EXTENSION = ".class";

                /**
                 * The protocol name of a jar file.
                 */
                private static final String JAR_FILE = "jar";

                /**
                 * The protocol name of a file system link.
                 */
                private static final String FILE_SYSTEM = "file";

                /**
                 * The protocol name of a Java 9 runtime image.
                 */
                private static final String RUNTIME_IMAGE = "jrt";

                /**
                 * The seal base locator to fallback to when a resource is not found or an unexpected URL protocol is discovered.
                 */
                private final SealBaseLocator fallback;

                /**
                 * Creates a new seal base locator that attempts deduction from a type's URL while using a
                 * {@link net.bytebuddy.dynamic.loading.PackageDefinitionStrategy.ManifestReading.SealBaseLocator.NonSealing} seal base locator
                 * as a fallback.
                 */
                public ForTypeResourceUrl() {
                    this(NonSealing.INSTANCE);
                }

                /**
                 * Creates a new seal base locator that attempts deduction from a type's URL.
                 *
                 * @param fallback The seal base locator to fallback to when a resource is not found or an unexpected URL protocol is discovered.
                 */
                public ForTypeResourceUrl(SealBaseLocator fallback) {
                    this.fallback = fallback;
                }

                @Override
                public URL findSealBase(ClassLoader classLoader, String typeName) {
                    URL url = classLoader.getResource(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
                    if (url != null) {
                        try {
                            if (url.getProtocol().equals(JAR_FILE)) {
                                return new URL(url.getPath().substring(0, url.getPath().indexOf('!')));
                            } else if (url.getProtocol().equals(FILE_SYSTEM)) {
                                return url;
                            } else if (url.getProtocol().equals(RUNTIME_IMAGE)) {
                                String path = url.getPath();
                                int modulePathIndex = path.indexOf('/', EXCLUDE_INITIAL_SLASH);
                                return modulePathIndex == -1
                                        ? url
                                        : new URL(RUNTIME_IMAGE + ":" + path.substring(0, modulePathIndex));
                            }
                        } catch (MalformedURLException exception) {
                            throw new IllegalStateException("Unexpected URL: " + url, exception);
                        }
                    }
                    return fallback.findSealBase(classLoader, typeName);
                }
            }
        }
    }
}
