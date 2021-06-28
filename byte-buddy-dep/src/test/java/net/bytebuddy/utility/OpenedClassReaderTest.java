package net.bytebuddy.utility;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OpenedClassReaderTest {

    @Test
    public void testAsmApiVersion() throws Exception {
        int version = 0;
        Pattern pattern = Pattern.compile("ASM[0-9]+");
        for (Field field : Opcodes.class.getFields()) {
            if (pattern.matcher(field.getName()).matches()) {
                version = Math.max(version, field.getInt(null));
            }
        }
        assertThat(OpenedClassReader.ASM_API, is(version));
    }
}
