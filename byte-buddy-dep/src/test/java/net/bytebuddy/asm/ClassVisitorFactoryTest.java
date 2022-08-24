package net.bytebuddy.asm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ClassVisitorFactoryTest {

    private final Method[] method;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ClassVisitor classVisitor;

    public ClassVisitorFactoryTest(Method[] method) {
        this.method = method;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> parameters = new ArrayList<Object[]>();
        for (Method method : ClassVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{method}});
        }
        for (Method method : AnnotationVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{ClassVisitor.class.getMethod("visitAnnotation", String.class, boolean.class), method}});
        }
        for (Method method : ModuleVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{ClassVisitor.class.getMethod("visitModule", String.class, int.class, String.class), method}});
        }
        for (Method method : RecordComponentVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{ClassVisitor.class.getMethod("visitRecordComponent", String.class, String.class, String.class), method}});
        }
        for (Method method : FieldVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{ClassVisitor.class.getMethod("visitField", int.class, String.class, String.class, String.class, Object.class), method}});
        }
        for (Method method : MethodVisitor.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            parameters.add(new Object[]{new Method[]{ClassVisitor.class.getMethod("visitMethod", int.class, String.class, String.class, String.class, String[].class), method}});
        }
        return parameters;
    }

    @Test
    public void testWrap() throws Exception {
        ClassVisitorFactory<ClassVisitor> factory = ClassVisitorFactory.of(ClassVisitor.class);
        Object current = factory.wrap(classVisitor);
        for (Method method : this.method) {
            Object[] argument = new Object[method.getParameterTypes().length];
            for (int index = 0; index < method.getParameterTypes().length; index++) {
                if (method.getParameterTypes()[index] == boolean.class) {
                    argument[index] = false;
                } else if (method.getParameterTypes()[index] == byte.class) {
                    argument[index] = (byte) 0;
                } else if (method.getParameterTypes()[index] == short.class) {
                    argument[index] = (short) 0;
                } else if (method.getParameterTypes()[index] == char.class) {
                    argument[index] = (char) 0;
                } else if (method.getParameterTypes()[index] == int.class) {
                    argument[index] = 0;
                } else if (method.getParameterTypes()[index] == long.class) {
                    argument[index] = 0L;
                } else if (method.getParameterTypes()[index] == float.class) {
                    argument[index] = 0f;
                } else if (method.getParameterTypes()[index] == double.class) {
                    argument[index] = 0d;
                } else {
                    argument[index] = null;
                }
            }
            current = method.invoke(current, argument);
        }
    }

    @Test
    public void testUnwrap() throws Exception {
        ClassVisitorFactory<ClassVisitor> factory = ClassVisitorFactory.of(ClassVisitor.class);
        Object current = factory.unwrap(classVisitor);
        for (Method method : this.method) {
            Object[] argument = new Object[method.getParameterTypes().length];
            for (int index = 0; index < method.getParameterTypes().length; index++) {
                if (method.getParameterTypes()[index] == boolean.class) {
                    argument[index] = false;
                } else if (method.getParameterTypes()[index] == byte.class) {
                    argument[index] = (byte) 0;
                } else if (method.getParameterTypes()[index] == short.class) {
                    argument[index] = (short) 0;
                } else if (method.getParameterTypes()[index] == char.class) {
                    argument[index] = (char) 0;
                } else if (method.getParameterTypes()[index] == int.class) {
                    argument[index] = 0;
                } else if (method.getParameterTypes()[index] == long.class) {
                    argument[index] = 0L;
                } else if (method.getParameterTypes()[index] == float.class) {
                    argument[index] = 0f;
                } else if (method.getParameterTypes()[index] == double.class) {
                    argument[index] = 0d;
                } else {
                    argument[index] = null;
                }
            }
            current = method.invoke(current, argument);
        }
    }
}
