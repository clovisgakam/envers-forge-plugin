package cm.adorsys.forge.envers;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
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
@RequiresFacet({PersistenceFacet.class,ResourceFacet.class, DependencyFacet.class, JavaSourceFacet.class})
public class EnversPlugin implements Plugin
{

	public static String ENVERS_GROUPID_DEPENDENCY = "org.hibernate" ;
	public static String ENVERS_ARTIFACTID_DEPENDENCY = "hibernate-envers" ;
	public static String ENVERS_VERSION_DEPENDENCY = "3.6.6.Final" ;
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

	@SetupCommand
	public void setupCommand( PipeOut out)
	{
		if(!project.hasFacet(PersistenceFacet.class)){
			ShellMessages.error(out, "you must setup persistence before use Envers ");
			return ;
		}
			List<ScopeType> bundle =  Arrays.asList(ScopeType.COMPILE,ScopeType.PROVIDED);
			ScopeType selectedScope = prompt.promptChoiceTyped("select envers dependency scope  ", bundle);
			installEnversDependencies(out, selectedScope);
			//setupDefaultProperties(out);
	}


	@DefaultCommand
	public void defaultCommand(@PipeIn String in, PipeOut out)
	{
		ShellMessages.success(out, "Enver pluggin are setup successfuly ");
	}

	@Command(value="audit-entity",help="add @Audited annotation on Entity class")
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

	@Command(value="audit-field",help="add @Audited annotation on Entity field")
	public void auditFieldCommand(@PipeIn String in, PipeOut out,@Option(name="auditable-entity",required=true,type=PromptType.JAVA_CLASS) JavaResource auditableJavaSource , 
			@Option(name="fielname",required=true) String fieldname )
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

	private void installEnversDependencies(PipeOut out,ScopeType scopeType)
	{
		out.println("Executed named command without args.");
		this.dependencyFacet = project.getFacet(DependencyFacet.class);
		DependencyBuilder enversPersistenceDependency =
				DependencyBuilder.create()
				.setGroupId(ENVERS_GROUPID_DEPENDENCY)
				.setArtifactId(ENVERS_ARTIFACTID_DEPENDENCY)
				.setScopeType(scopeType);
		if (!dependencyFacet.hasDirectDependency(enversPersistenceDependency))
		{
			dependencyFacet.setProperty("envers.version", ENVERS_VERSION_DEPENDENCY);
			dependencyFacet.addDirectDependency(enversPersistenceDependency.setVersion("${envers.version}"));
		}
		ShellMessages.success(out, "enver dependency are installed successfully.");
	}

	private void setupDefaultProperties(PipeOut out)
	{
		FileResource<?> persistencceXml;
		ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		persistencceXml = resourceFacet.getResource("META-INF/persistence.xml");
		for (int i = 0; i < 5; i++) {
			addPropertyToPersistenceXml("name"+i, "value"+i, persistencceXml);
		}
		ShellMessages.success(out, "default envers persistence xml properties are configure correctly.");
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
		if(!isAuditableEntity(javaClass)) throw new UnAuditableEntityException(javaClass.getName()+" is not auditable ,java class must have "+Entity.class.getSimpleName()+" to be audited !");
		javaClass.addImport(Audited.class);
		if(!javaClass.hasAnnotation(Audited.class)) javaClass.addAnnotation(Audited.class);
	}

	private void auditField(Field<JavaClass> field) throws UnAuditableFieldException{
		if(!isAuditableEntity(field.getOrigin())) throw new UnAuditableFieldException(field.getName()+" is not auditable ,java class must have "+Audited.class.getSimpleName()+" to be audited !");
		if(!field.hasAnnotation(Audited.class))field.addAnnotation(Audited.class);
		field.getOrigin().addImport(Audited.class);
	}

	public boolean isAuditableEntity(JavaClass javaClass){
		return javaClass.hasAnnotation(Entity.class);
	}
}
