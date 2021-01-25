package net.bytebuddy.description.type;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import org.objectweb.asm.Opcodes;

/**
 * A package description represents a Java package. 软件包描述表示Java软件包  描述包的结构的类，典型的会在每个包底下创建package_info.java来存放包的信息
 */
public interface PackageDescription extends NamedElement.WithRuntimeName, AnnotationSource {

    /**
     * The name of a Java class representing a package description. 表示程序包描述的Java类的名称
     */
    String PACKAGE_CLASS_NAME = "package-info";

    /**
     * The modifiers of a Java class representing a package description. 表示软件包描述的Java类的修饰符
     */
    int PACKAGE_MODIFIERS = Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;

    /**
     * A named constant for an undefined package what applies for primitive and array types. 未定义包的命名常量，适用于基本类型和数组类型
     */
    PackageDescription UNDEFINED = null;

    /**
     * Checks if this package contains the provided type. 检查此程序包是否包含提供的类型  判断某个包内是否包含给定的类型
     *
     * @param typeDescription The type to examine.
     * @return {@code true} if the given type contains the provided type.
     */
    boolean contains(TypeDescription typeDescription);

    /**
     * An abstract base implementation of a package description.
     */
    abstract class AbstractBase implements PackageDescription {

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public String getActualName() {
            return getName();
        }

        @Override
        public boolean contains(TypeDescription typeDescription) {
            return this.equals(typeDescription.getPackage());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof PackageDescription && getName().equals(((PackageDescription) other).getName());
        }

        @Override
        public String toString() {
            return "package " + getName();
        }
    }

    /**
     * A simple implementation of a package without annotations.
     */
    class Simple extends AbstractBase {

        /**
         * The name of the package.
         */
        private final String name;

        /**
         * Creates a new simple package.
         *
         * @param name The name of the package.
         */
        public Simple(String name) {
            this.name = name;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Represents a loaded {@link java.lang.Package} wrapped as a
     * {@link PackageDescription}.
     */
    class ForLoadedPackage extends AbstractBase {

        /**
         * The represented package.
         */
        private final Package aPackage;

        /**
         * Creates a new loaded package representation.
         *
         * @param aPackage The represented package.
         */
        public ForLoadedPackage(Package aPackage) {
            this.aPackage = aPackage;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(aPackage.getDeclaredAnnotations());
        }

        @Override
        public String getName() {
            return aPackage.getName();
        }
    }
}
