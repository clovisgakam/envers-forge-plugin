package cm.adorsys.forge.envers;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.forge.test.SingletonAbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;

public class EnversPluginTest extends SingletonAbstractShellTest
{
	@Deployment
	public static JavaArchive getDeployment() {
		return AbstractShellTest.getDeployment().addPackages(true,
				EnversPlugin.class.getPackage());
	}

	@Before
	public void beforeTest() throws Exception {
		super.beforeTest();
		initializeJavaProject();
		if ((getProject() != null)
				&& !getProject().hasFacet(PersistenceFacet.class)) {
			queueInputLines("", "", "");
			getShell()
					.execute(
							"persistence setup --provider HIBERNATE --container JBOSS_AS7");
		}
	}

	@Test
	public void testSetup() throws Exception {
		getShell().execute("set ACCEPT_DEFAULTS true");
		getShell().execute("envers setup");
	}
}
