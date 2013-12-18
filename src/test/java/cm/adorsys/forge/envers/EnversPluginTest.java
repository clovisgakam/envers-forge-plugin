package cm.adorsys.forge.envers;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

public class EnversPluginTest extends AbstractShellTest
{
   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment()
            .addPackages(true, EnversPlugin.class.getPackage());
   }

   @Test
   public void testDefaultCommand() throws Exception
   {
      getShell().execute("envers");
   }

   @Test
   public void testCommand() throws Exception
   {
      getShell().execute("envers command");
   }

   @Test
   public void testPrompt() throws Exception
   {
      queueInputLines("y");
      getShell().execute("envers prompt foo bar");
   }
}
