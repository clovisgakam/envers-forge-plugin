
package cm.adorsys.forge.envers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.completer.PropertyCompleter;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.PersistenceFacet;
/**
 *@author clovis gakam
 */
@Alias("envers")
@RequiresProject
@Help("This plugin will help you setting up Envers .")
@RequiresFacet({EnversPluginFacet.class})
public class EnversPlugin implements Plugin
{
	public static final String ENVER_AUDITED_ENTITY_TABLE_SUFFIX = "envers.audited.table.prefix";


	@Inject
	private ShellPrompt prompt;

	@Inject
	private Project project;

	@Inject
	private Shell shell;


	@Inject
	private Event<PickupResource> pickup;

	@Inject
	private Event<InstallFacets> event;

	private DependencyFacet dependencyFacet;

	@Inject
	@Current
	private Resource<?> currentResource;

	@Inject
	EnversPluginUtil pluginUtil;

	@Inject
	private ConfigurationFactory configurationFactory;

	// Do not refer this field directly. Use the getProjectConfiguration()
	// method instead.
	private Configuration configuration;

	@SetupCommand
	public void setupCommand(final PipeOut out)
	{
		if(!project.hasFacet(PersistenceFacet.class)){
			ShellMessages.error(out, "you must setup persistence before use Envers ");
			return ;
		}
		if (!project.hasFacet(EnversPluginFacet.class)) {
			event.fire(new InstallFacets(EnversPluginFacet.class));
		}
		configureEnvers();
		ShellMessages.success(out, "envers audit service installed.");

	}

	@Command(value="audit-entity",help="add @Audited annotation on Entity class")
	public void auditEntiiesCommand(final PipeOut out, @Option(required=false) JavaResource... targets) throws FileNotFoundException
	{
		if (((targets == null) || (targets.length < 1))
				&& (currentResource instanceof JavaResource)) {
			targets = new JavaResource[] { (JavaResource) currentResource };
		}

		List<JavaResource> javaTargets = selectTargets(out, targets);
		if (javaTargets.isEmpty()) {
			ShellMessages.error(out, "Must specify a domain @Entity on which to operate.");
			return;
		}
		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		for (JavaResource javaResource : javaTargets) {
			JavaClass entity = (JavaClass) (javaResource).getJavaSource();
			pluginUtil.auditEntity(entity);
			java.saveJavaSource(entity);
			pickup.fire(new PickupResource(java.getJavaResource(entity)));

		}


	}

	@Command(value="audit-field",help="add @Audited annotation on Entity field")
	public void auditFieldCommand(final PipeOut out,@Option(name="fieldname",required=true ,completer=PropertyCompleter.class ) String fieldname,
			@Option(required=false,type=PromptType.JAVA_CLASS) JavaResource targets ) throws FileNotFoundException
			{
		if ((targets == null)&& (currentResource instanceof JavaResource)) {
			targets = (JavaResource) currentResource ;
		}
		if (targets==null) {
			ShellMessages.error(out, "Must specify a domain @Entity on which to operate.");
			return;
		}

		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		JavaClass auditableClass = (JavaClass) targets.getJavaSource();
		Field<JavaClass> fieldToAudited = auditableClass.getField(fieldname);
		pluginUtil.auditField(fieldToAudited);
		java.saveJavaSource(auditableClass);
		pickup.fire(new PickupResource(java.getJavaResource(auditableClass)));


			}




	private void addPropertyToPersistenceXml(String name, String value,FileResource<?> resource){
		Node xml = XMLParser.parse(resource.getResourceInputStream());
		Node propertyWithName = pluginUtil.getPropertyWithName(name, resource);
		if(propertyWithName!=null){
			propertyWithName.attribute("value", value);
		}else {
			Node config = xml.getOrCreate("properties");
			config.createChild("property")
			.attribute("name", name)
			.attribute("value", value);
		}
		resource.setContents(XMLParser.toXMLInputStream(xml));
	}

	

	

	/**
	 * Important: Use this method always to obtain the configuration. Do not
	 * invoke this inside a constructor since the returned {@link Configuration}
	 * instance would not be the project scoped one.
	 * 
	 * @return The project scoped {@link Configuration} instance
	 */
	private Configuration getProjectConfiguration() {
		if (this.configuration == null) {
			this.configuration = configurationFactory.getProjectConfig(project);
		}
		return this.configuration;
	}

	public void configureEnvers(){
		Configuration projectConfiguration = getProjectConfiguration();
		String tableSuffix = prompt.prompt(
				"How do you want to name the Audited Table suffix?", "AUD");
		projectConfiguration.setProperty(ENVER_AUDITED_ENTITY_TABLE_SUFFIX, tableSuffix);
	}

	private List<JavaResource> selectTargets(final PipeOut out,
			Resource<?>[] targets) throws FileNotFoundException {
		List<JavaResource> results = new ArrayList<JavaResource>();
		if (targets == null) {
			targets = new Resource<?>[] {};
		}
		for (Resource<?> r : targets) {
			if (r instanceof JavaResource) {
				JavaSource<?> entity = ((JavaResource) r).getJavaSource();
				if (entity instanceof JavaClass) {
					if (entity.hasAnnotation(Entity.class)) {
						results.add((JavaResource) r);
					} else {
						displaySkippingResourceMsg(out, entity);
					}
				} else {
					displaySkippingResourceMsg(out, entity);
				}
			}
		}
		return results;
	}

	private void displaySkippingResourceMsg(final PipeOut out,
			final JavaSource<?> entity) {
		if (!out.isPiped()) {
			ShellMessages.info(out, "Skipped non-@Entity Java resource ["
					+ entity.getQualifiedName() + "]");
		}
	}
}
