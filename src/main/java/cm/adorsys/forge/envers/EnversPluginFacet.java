package cm.adorsys.forge.envers;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 * @author clovis gakam
 *
 */
@RequiresFacet({PersistenceFacet.class})
public class EnversPluginFacet  extends BaseFacet{
	public static final String ENVERS_GROUPID_DEPENDENCY = "org.hibernate";
	public static final String ENVERS_ARTIFACTID_DEPENDENCY = "hibernate-envers";
	public static final String ENVERS_VERSION_DEPENDENCY = "4.2.0.Final";

	@Inject
	private ShellPrompt prompt;

	private final DependencyInstaller installer;

	public  List<Dependency> getRequiredDependencies() {
		return Arrays.asList((Dependency) DependencyBuilder.create()
				.setGroupId(ENVERS_GROUPID_DEPENDENCY)
				.setArtifactId(ENVERS_ARTIFACTID_DEPENDENCY)
				.setVersion(ENVERS_VERSION_DEPENDENCY));

	}

	@Inject
	public EnversPluginFacet(DependencyInstaller installer) {
		this.installer = installer ;
	}

	

	@Override
	public boolean install() {
		for (Dependency requirement : getRequiredDependencies()) {
			if (!installer.isInstalled(getProject(), requirement)) {
				DependencyFacet deps = project.getFacet(DependencyFacet.class);
				if (!deps.hasDirectDependency(requirement)) {
					installer.install(getProject(), requirement,ScopeType.PROVIDED);
				}
			}
		}
		
		return true ;
	}

	@Override
	public boolean isInstalled() {
		DependencyFacet deps = getProject().getFacet(DependencyFacet.class);
		for (Dependency requirement : getRequiredDependencies()) {
			if (!deps.hasDirectDependency(requirement))
			{
				return false;
			}
		}
		return true;
	}

	

}
