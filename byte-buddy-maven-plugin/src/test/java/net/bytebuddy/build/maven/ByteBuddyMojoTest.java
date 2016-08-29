package net.bytebuddy.build.maven;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class ByteBuddyMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void testEmptyTransformation() throws Exception {
        execute("transform", "empty");
    }

    @Test
    public void testSimpleTransformation() throws Exception {
//        execute("transform", "simple");
    }

    private void execute(String goal, String target) throws Exception {
        Mojo mojo = mojoRule.lookupMojo(goal, new File("src/test/resources/" + target + ".pom.xml"));
        mojo.setLog(new SilentLog());
        mojo.execute();
    }
}
