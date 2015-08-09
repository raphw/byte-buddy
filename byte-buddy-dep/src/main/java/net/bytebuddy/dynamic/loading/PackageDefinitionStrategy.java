package net.bytebuddy.dynamic.loading;

import java.io.IOException;
import java.io.InputStream;
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
     * @param packageName The name of the package.
     * @param classLoader The class loader for which this package is being defined.
     * @param typeName    The name of the type being loaded when the package is defined
     * @return A definition of the package.
     */
    Definition define(String packageName, ClassLoader classLoader, String typeName) throws IOException;

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
         * Returns the package specification's title or {@code null} if no such title exists.
         *
         * @return The package specification's title.
         */
        String getSpecificationTitle();

        /**
         * Returns the package specification's version or {@code null} if no such version exists.
         *
         * @return The package specification's version.
         */
        String getSpecificationVersion();

        /**
         * Returns the package specification's vendor or {@code null} if no such vendor exists.
         *
         * @return The package specification's vendor.
         */
        String getSpecificationVendor();

        /**
         * Returns the package implementation's title or {@code null} if no such title exists.
         *
         * @return The package implementation's title.
         */
        String getImplementationTitle();

        /**
         * Returns the package implementation's version or {@code null} if no such version exists.
         *
         * @return The package implementation's version.
         */
        String getImplementationVersion();

        /**
         * Returns the package implementation's vendor or {@code null} if no such vendor exists.
         *
         * @return The package implementation's vendor.
         */
        String getImplementationVendor();

        /**
         * The URL representing the seal base.
         *
         * @return The seal base of the package.
         */
        URL getSealBase();

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
            public String toString() {
                return "PackageDefinitionStrategy.Definition.Undefined." + name();
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
            public String toString() {
                return "PackageDefinitionStrategy.Definition.Trivial." + name();
            }
        }

        /**
         * A simple package definition where any property is represented by a value.
         */
        class Simple implements Definition {

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
             * The seal base or {@code null} if the package is not sealed.
             */
            private final URL sealBase;

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

            @Override
            public String toString() {
                return "PackageDefinitionStrategy.Definition.Simple{" +
                        "specificationTitle='" + specificationTitle + '\'' +
                        ", specificationVersion='" + specificationVersion + '\'' +
                        ", specificationVendor='" + specificationVendor + '\'' +
                        ", implementationTitle='" + implementationTitle + '\'' +
                        ", implementationVersion='" + implementationVersion + '\'' +
                        ", implementationVendor='" + implementationVendor + '\'' +
                        ", sealBase=" + sealBase +
                        '}';
            }
        }
    }

    /**
     * A package definer that does not define any package.
     */
    enum NoOp implements PackageDefinitionStrategy {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Definition define(String packageName, ClassLoader classLoader, String typeName) {
            return Definition.Undefined.INSTANCE;
        }

        @Override
        public String toString() {
            return "PackageDefinitionStrategy.NoOp." + name();
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
        public Definition define(String packageName, ClassLoader classLoader, String typeName) {
            return Definition.Trivial.INSTANCE;
        }

        @Override
        public String toString() {
            return "PackageDefinitionStrategy.Trivial." + name();
        }
    }

    /**
     * A package definer that reads a class loader's manifest file.
     */
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
         * Creates a new package definer that reads a class loader's manifest file.
         *
         * @param sealBaseLocator A locator for a sealed package's URL.
         */
        public ManifestReading(SealBaseLocator sealBaseLocator) {
            this.sealBaseLocator = sealBaseLocator;
        }

        @Override
        public Definition define(String packageName, ClassLoader classLoader, String typeName) throws IOException {
            InputStream inputStream = classLoader.getResourceAsStream(MANIFEST_FILE);
            if (inputStream != null) {
                try {
                    Manifest manifest = new Manifest(inputStream);
                    Map<Attributes.Name, String> values = new HashMap<Attributes.Name, String>(ATTRIBUTE_NAMES.length);
                    Attributes attributes = manifest.getMainAttributes();
                    if (attributes != null) {
                        for (Attributes.Name attributeName : ATTRIBUTE_NAMES) {
                            values.put(attributeName, attributes.getValue(attributeName));
                        }
                    }
                    attributes = manifest.getAttributes(packageName.replace('.', '/').concat("/"));
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
                                    ? sealBaseLocator.findSealBase(packageName, classLoader, typeName)
                                    : NOT_SEALED);
                } finally {
                    inputStream.close();
                }
            } else {
                return Definition.Trivial.INSTANCE;
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && sealBaseLocator.equals(((ManifestReading) other).sealBaseLocator);
        }

        @Override
        public int hashCode() {
            return sealBaseLocator.hashCode();
        }

        @Override
        public String toString() {
            return "PackageDefinitionStrategy.ManifestReading{sealBaseLocator=" + sealBaseLocator + '}';
        }

        /**
         * A locator for a seal base URL.
         */
        public interface SealBaseLocator {

            /**
             * Locates the URL that should be used for sealing a package.
             *
             * @param packageName The name of the package.
             * @param classLoader The class loader loading the package.
             * @param typeName    The name of the type being loaded when defining this package.
             * @return The URL that is used for sealing a package or {@code null} if the package should not be sealed.
             */
            URL findSealBase(String packageName, ClassLoader classLoader, String typeName);

            /**
             * A seal base locator that never seals a package.
             */
            enum NonSealing implements SealBaseLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public URL findSealBase(String packageName, ClassLoader classLoader, String typeName) {
                    return NOT_SEALED;
                }

                @Override
                public String toString() {
                    return "PackageDefinitionStrategy.ManifestReading.CodeSourceLocator.NonSealing." + name();
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
                public URL findSealBase(String packageName, ClassLoader classLoader, String typeName) {
                    return sealBase;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && sealBase.equals(((ForFixedValue) other).sealBase);
                }

                @Override
                public int hashCode() {
                    return sealBase.hashCode();
                }

                @Override
                public String toString() {
                    return "PackageDefinitionStrategy.ManifestReading.CodeSourceLocator.ForFixedValue{" +
                            "sealBase=" + sealBase +
                            '}';
                }
            }
        }
    }
}
