package net.bytebuddy.description.module;

import net.bytebuddy.utility.AsmClassWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractModuleDescriptionTest {

    protected static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    protected File jar;

    @Before
    public void setUp() throws Exception {
        jar = File.createTempFile("sample", ".jar");
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar));
        try {
            {
                outputStream.putNextEntry(new JarEntry("module-info.class"));
                AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(0);
                ClassVisitor classVisitor = classWriter.getVisitor();
                classVisitor.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
                ModuleVisitor moduleVisitor = classVisitor.visitModule(FOO, 0, BAR);
                moduleVisitor.visitMainClass(QUX);
                moduleVisitor.visitPackage(FOO);
                moduleVisitor.visitUse(BAR);
                moduleVisitor.visitProvide(QUX, BAZ);
                moduleVisitor.visitExport(FOO, 0, BAR);
                moduleVisitor.visitOpen(QUX, 0, BAZ);
                moduleVisitor.visitRequire(FOO + BAR, 0, QUX + BAZ);
                moduleVisitor.visitEnd();
                classVisitor.visitEnd();
                outputStream.write(classWriter.getBinaryRepresentation());
                outputStream.closeEntry();
            }
            {
                outputStream.putNextEntry(new JarEntry(FOO + "/" + BAR + ".class"));
                AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(0);
                ClassVisitor classVisitor = classWriter.getVisitor();
                classVisitor.visit(Opcodes.V9, Opcodes.ACC_PUBLIC, FOO + "/" + BAR, null, Type.getInternalName(Object.class), null);
                classVisitor.visitEnd();
                outputStream.write(classWriter.getBinaryRepresentation());
                outputStream.closeEntry();
            }
        } finally {
            outputStream.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        assertThat(jar.delete(), is(true));
    }

    protected abstract ModuleDescription toModuleDescription() throws Exception;

    @Test
    public void testModuleDescription() throws Exception {
        ModuleDescription moduleDescription = toModuleDescription();
        assertThat(moduleDescription.getActualName(), is(FOO));
        assertThat(moduleDescription.getMainClass(), is(QUX));
        assertThat(moduleDescription.getPackages(), is(Collections.singleton(FOO)));
        assertThat(moduleDescription.getUses(), is(Collections.singleton(BAR)));
        assertThat(moduleDescription.getProvides(), is(Collections.<String, ModuleDescription.Provides>singletonMap(
                QUX,
                new ModuleDescription.Provides.Simple(Collections.singleton(BAZ)))));
        assertThat(moduleDescription.getExports(), is(Collections.<String, ModuleDescription.Exports>singletonMap(
                FOO,
                new ModuleDescription.Exports.Simple(Collections.singleton(BAR), 0))));
        assertThat(moduleDescription.getOpens(), is(Collections.<String, ModuleDescription.Opens>singletonMap(
                QUX,
                new ModuleDescription.Opens.Simple(Collections.singleton(BAZ), 0))));
        assertThat(moduleDescription.getRequires(), is(Collections.<String, ModuleDescription.Requires>singletonMap(
                FOO + BAR,
                new ModuleDescription.Requires.Simple(QUX + BAZ, 0))));
        assertThat(moduleDescription.hashCode(), is(toModuleDescription().hashCode()));
        assertThat(moduleDescription, is(toModuleDescription()));
        assertThat(moduleDescription.getProvides().hashCode(), is(toModuleDescription().getProvides().hashCode()));
        assertThat(moduleDescription.getProvides(), is(toModuleDescription().getProvides()));
        assertThat(moduleDescription.getExports().hashCode(), is(toModuleDescription().getExports().hashCode()));
        assertThat(moduleDescription.getExports(), is(toModuleDescription().getExports()));
        assertThat(moduleDescription.getOpens().hashCode(), is(toModuleDescription().getOpens().hashCode()));
        assertThat(moduleDescription.getOpens(), is(toModuleDescription().getOpens()));
        assertThat(moduleDescription.getRequires().hashCode(), is(toModuleDescription().getRequires().hashCode()));
        assertThat(moduleDescription.getRequires(), is(toModuleDescription().getRequires()));
        assertThat(moduleDescription, is(toModuleDescription()));
        assertThat(moduleDescription.toString(), is("module " + moduleDescription.getActualName()));
    }
}
