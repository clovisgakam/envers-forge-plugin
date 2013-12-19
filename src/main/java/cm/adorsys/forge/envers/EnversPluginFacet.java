package cm.adorsys.forge.envers;

import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;

@RequiresFacet({PersistenceFacet.class,ResourceFacet.class, DependencyFacet.class, JavaSourceFacet.class})
public class EnversPluginFacet  extends BaseFacet{

	@Override
	public boolean install() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstalled() {
		// TODO Auto-generated method stub
		return false;
	}

}
