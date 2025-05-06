import net.bytebuddy.ByteBuddy;
import net.bytebuddy.utility.AsmClassReader;
import net.bytebuddy.utility.AsmClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;

public class Bla {

    public static void main(String[] args) throws Exception{
        new ByteBuddy()
                .with(AsmClassReader.Factory.Default.CLASS_FILE_API_FIRST)
                .with(AsmClassWriter.Factory.Default.CLASS_FILE_API_FIRST);
    }
}
