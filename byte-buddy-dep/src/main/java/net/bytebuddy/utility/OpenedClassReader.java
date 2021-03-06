package net.bytebuddy.utility;

import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.security.AccessController;

/**
 * A {@link ClassReader} that does not apply a class file version check if the {@code net.bytebuddy.experimental} property is set. 不应用类文件版本的{@link ClassReader}检查是否设置了{@code net.bytebuddy.experimental}属性
 */
public class OpenedClassReader {

    /**
     * Indicates that Byte Buddy should not validate the maximum supported class file version.
     */
    public static final String EXPERIMENTAL_PROPERTY = "net.bytebuddy.experimental";

    /**
     * {@code true} if Byte Buddy is executed in experimental mode.
     */
    public static final boolean EXPERIMENTAL;

    /**
     * Indicates the ASM API version that is used throughout Byte Buddy.
     */
    public static final int ASM_API;

    /*
     * Checks the experimental property.
     */
    static {
        boolean experimental;
        try {
            experimental = Boolean.parseBoolean(AccessController.doPrivileged(new GetSystemPropertyAction(EXPERIMENTAL_PROPERTY)));
        } catch (Exception ignored) {
            experimental = false;
        }
        EXPERIMENTAL = experimental;
        @SuppressWarnings("deprecation")
        int asm7Experimental = Opcodes.ASM7_EXPERIMENTAL;
        ASM_API = experimental ? asm7Experimental : Opcodes.ASM6;
    }

    /**
     * Not intended for construction.
     */
    private OpenedClassReader() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
    }

    /**
     * Creates a class reader for the given binary representation of a class file. 为类文件的给定二进制表示形式创建类读取器
     *
     * @param binaryRepresentation The binary representation of a class file to read. 要读取的类文件的二进制表示
     * @return An appropriate class reader. 一个合适的类读取器
     */
    public static ClassReader of(byte[] binaryRepresentation) {
        if (EXPERIMENTAL) {
            byte[] actualVersion = new byte[]{binaryRepresentation[4], binaryRepresentation[5], binaryRepresentation[6], binaryRepresentation[7]};
            binaryRepresentation[4] = (byte) (Opcodes.V11 >>> 24);
            binaryRepresentation[5] = (byte) (Opcodes.V11 >>> 16);
            binaryRepresentation[6] = (byte) (Opcodes.V11 >>> 8);
            binaryRepresentation[7] = (byte) Opcodes.V11;
            ClassReader classReader = new ClassReader(binaryRepresentation);
            System.arraycopy(actualVersion, 0, binaryRepresentation, 4, actualVersion.length);
            return classReader;
        } else {
            return new ClassReader(binaryRepresentation);
        }
    }
}
