
package cm.adorsys.forge.envers;

import java.io.FileNotFoundException;
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
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.PersistenceFacet;

import cm.adorsys.forge.envers.exceptions.UnAuditableEntityException;
import cm.adorsys.forge.envers.exceptions.UnAuditableFieldException;
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
	public void auditEntiyCommand(@PipeIn String in, PipeOut out, @Option(name="auditable-class",required=true,type=PromptType.JAVA_CLASS) JavaResource auditableJavaSource)
	{
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		try {
			JavaClass auditableClass = (JavaClass) auditableJavaSource.getJavaSource();
			auditEntity(auditableClass);
			javaSourceFacet.saveJavaSource(auditableClass);
			pickup.fire(new PickupResource(javaSourceFacet.getJavaResource(auditableClass)));

		} catch (UnAuditableEntityException e) {
			ShellMessages.error(out, e.getMessage());
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, e.getMessage());
		} catch (Exception e) {
			ShellMessages.error(out, e.getMessage());
		}
	}

	@Command(value="audit-field",help="add @Audited annotation on Entity field")
	public void auditFieldCommand(@PipeIn String in, PipeOut out,@Option(name="auditable-class",required=true,type=PromptType.JAVA_CLASS) JavaResource auditableJavaSource , 
			@Option(name="fieldname",required=true) String fieldname )
	{
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		try {
			JavaClass auditableClass = (JavaClass) auditableJavaSource.getJavaSource();
			Field<JavaClass> fieldToAudited = auditableClass.getField(fieldname);
			auditField(fieldToAudited);
			javaSourceFacet.saveJavaSource(auditableClass);
			pickup.fire(new PickupResource(javaSourceFacet.getJavaResource(auditableClass)));

		} catch (UnAuditableFieldException e) {
			ShellMessages.error(out, e.getMessage());
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, e.getMessage());
		} catch (Exception e) {
			ShellMessages.error(out, e.getMessage());
		}
	}

	
	

	private void addPropertyToPersistenceXml(String name, String value,FileResource<?> resource){
		Node xml = XMLParser.parse(resource.getResourceInputStream());
		Node propertyWithName = getPropertyWithName(name, resource);
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

	private Node getPropertyWithName(String name,FileResource<?> resource){
		Node matchedNode = null ;
		Node resource2 = XMLParser.parse(resource.getResourceInputStream());
		List<Node> list = resource2.get("property");
		for (Node node : list) {
			String attribute = node.getAttribute("name");
			if(StringUtils.equals(attribute, name)){
				matchedNode = node ;
				break ;
			}

		}

		return matchedNode ;
	}

	private void auditEntity(JavaClass javaClass) throws UnAuditableEntityException{
		if(!isAuditableEntity(javaClass)) throw new UnAuditableEntityException(javaClass.getName()+" is not auditable ,java class must have @Entity annotation  to be audited !");
		javaClass.addImport(Audited.class);
		if(!javaClass.hasAnnotation(Audited.class)) javaClass.addAnnotation(Audited.class);
	}

	private void auditField(Field<JavaClass> field) throws UnAuditableFieldException{
		if(!isAuditableEntity(field.getOrigin())) throw new UnAuditableFieldException(field.getName()+" is not auditable ,java class must have @Audited annotation to be audited !");
		if(!field.hasAnnotation(Audited.class))field.addAnnotation(Audited.class);
		field.getOrigin().addImport(Audited.class);
	}

	public boolean isAuditableEntity(JavaClass javaClass){
		return javaClass.hasAnnotation(Entity.class);
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
}
