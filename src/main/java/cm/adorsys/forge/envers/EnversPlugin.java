package cm.adorsys.forge.envers;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
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
import org.jboss.forge.shell.plugins.RequiresPackagingType;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.RequiresResource;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.solder.core.Requires;

import cm.adorsys.forge.envers.exceptions.UnAuditableEntityException;
import cm.adorsys.forge.envers.exceptions.UnAuditableFieldException;

/**
 *@author clovis gakam
 */
@Alias("envers")
@RequiresProject
@Help("This plugin will help you setting up Envers .")
@RequiresFacet({ResourceFacet.class, DependencyFacet.class, JavaSourceFacet.class})
public class EnversPlugin implements Plugin
{
	public static String ENVERS_GROUPID_DEPENDENCY = "org.jboss.envers" ;
	public static String ENVERS_ARTIFACTID_DEPENDENCY = "hibernate-envers" ;
	public static String ENVERS_VERSION_DEPENDENCY = "3.0.1.final" ;
	public static String AUDITABLE_ENTITY_ANNOTATION = "javax.persistence.Entity" ;
	public static String AUDIT_ANNOTATION = "javax.persistence.Entity" ;
	@Inject
	private ShellPrompt prompt;

	@Inject
	private Project project;

	@Inject
	private Shell shell;


	@Inject
	private Event<PickupResource> pickup;
	private DependencyFacet dependencyFacet;

	@SetupCommand
	public void setupCommand( PipeOut out)
	{
		List<ScopeType> bundle =  Arrays.asList(ScopeType.COMPILE,ScopeType.PROVIDED);
		ScopeType selectedScope = prompt.promptChoiceTyped("select envers dependency scope  ", bundle);
		installEnversDependencies(out, selectedScope);
		setupDefaultProperties(out);

	}


	@DefaultCommand
	public void defaultCommand(@PipeIn String in, PipeOut out)
	{
		out.println("Executed default command."); 
	}
	@Command("audit-entity")
	public void auditEntiyCommand(@PipeIn String in, PipeOut out, @Option(name="auditable-entity",required=true,type=PromptType.JAVA_CLASS) JavaResource auditableJavaSource)
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

	@Command("audit-field")
	public void auditFieldCommand(@PipeIn String in, PipeOut out)
	{
		if (prompt.promptBoolean("Do you like writing Forge plugins?"))
			out.println("I am happy.");
		else
			out.println("I am sad.");
	}

	private void installEnversDependencies(PipeOut out,ScopeType scopeType)
	{
		out.println("Executed named command without args.");
		this.dependencyFacet = project.getFacet(DependencyFacet.class);
		DependencyBuilder enversPersistenceDependency =
				DependencyBuilder.create()
				.setGroupId(ENVERS_GROUPID_DEPENDENCY)
				.setArtifactId(ENVERS_ARTIFACTID_DEPENDENCY)
				.setScopeType(scopeType);
		out.println("Executed named command without args.");
		if (!dependencyFacet.hasDirectDependency(enversPersistenceDependency))
		{
			dependencyFacet.setProperty("envers.version", ENVERS_VERSION_DEPENDENCY);
			dependencyFacet.addDirectDependency(enversPersistenceDependency.setVersion("${envers.version}"));
		}
		out.println("enver dependency are installed successfully."); 
	}

	private void setupDefaultProperties(PipeOut out)
	{
		FileResource<?> persistencceXml;
		ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		persistencceXml = resourceFacet.getResource("META-INF/persistence.xml");
		for (int i = 0; i < 5; i++) {
			addPropertyToPersistenceXml("name"+i, "value"+i, persistencceXml);
		}
		out.println("default envers persistence xml properties are configure correctly."); 
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
		if(!isAuditableEntity(javaClass)) throw new UnAuditableEntityException(javaClass.getName()+" is not auditable ,java class must have "+AUDIT_ANNOTATION+" to be audited !");
		if(!javaClass.hasAnnotation(AUDITABLE_ENTITY_ANNOTATION)){
			javaClass.addImport(AUDIT_ANNOTATION);
			javaClass.addAnnotation(AUDIT_ANNOTATION);
		}
	}

	private void auditField(Field<JavaClass> field) throws UnAuditableFieldException{
		if(!isAuditableEntity(field.getOrigin())) throw new UnAuditableFieldException(field.getName()+" is not auditable ,java class must have "+AUDIT_ANNOTATION+" to be audited !");
		if(!field.hasAnnotation(AUDIT_ANNOTATION)) field.addAnnotation(AUDIT_ANNOTATION);
	}

	public boolean isAuditableEntity(JavaClass javaClass){
		return javaClass.hasAnnotation(AUDITABLE_ENTITY_ANNOTATION);
	}
}
