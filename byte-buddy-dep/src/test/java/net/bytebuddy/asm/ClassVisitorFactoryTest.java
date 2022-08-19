package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//@RunWith(Parameterized.class)
public class ClassVisitorFactoryTest {

    //private final Method method;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClassVisitor classVisitor;

    /*public ClassVisitorFactoryTest(Method method) {
        this.method = method;
    }*/

    //@Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new ArrayList<Object[]>();
        for (Method method : ClassVisitor.class.getMethods()) {
            parameters.add(new Object[]{method});
        }
        return parameters;
    }

    @Test
    public void testMethodDelegation() throws Exception {
        ClassVisitorFactory<ClassVisitor> factory = ClassVisitorFactory.of(ClassVisitor.class);
        factory.wrap(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitParameter(String name, int access) {
                        super.visitParameter(name, access);
                    }
                };
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
            }
        }).visitMethod(1, null, null, null, null).visitParameter(null, 1);


        // method.invoke(factory.wrap(classVisitor));
    }
}