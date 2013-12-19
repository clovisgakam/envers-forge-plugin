package cm.adorsys.forge.envers;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

public class EnversPluginTest extends AbstractShellTest
{
	@Inject
	private DependencyResolver resolver;

	@Deployment
	public static JavaArchive getDeployment()
	{
		return AbstractShellTest.getDeployment()
				.addPackages(true, EnversPlugin.class.getPackage());
	}

	@Test
	public void testSetupCommand() throws Exception
	{
		Project p = initializeJavaProject();
		 queueInputLines("y");
		getShell().execute("envers setup");
	}
	
	@Test
	public void testAuditEntityCommand() throws Exception
	{
		Project p = initializeJavaProject();
		 queueInputLines("y");
		getShell().execute("envers setup");
	}


}