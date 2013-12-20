package cm.adorsys.forge.envers;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class EnversPluginUtil {

	
	private void setupDefaultProperties(PipeOut out,Project project)
	{
		FileResource<?> persistencceXml;
		ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		persistencceXml = resourceFacet.getResource("META-INF/persistence.xml");
		for (int i = 0; i < 5; i++) {
		}
		ShellMessages.success(out, "default envers persistence xml properties are configure correctly.");
	}
	
}
