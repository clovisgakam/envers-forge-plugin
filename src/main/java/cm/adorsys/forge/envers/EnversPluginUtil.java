package cm.adorsys.forge.envers;

import java.io.File;
import java.util.List;

import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class EnversPluginUtil {

	public Node getPropertyWithName(String name,FileResource<?> resource){
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
	public boolean isAuditableEntity(JavaClass javaClass){
		return javaClass.hasAnnotation(Entity.class);
	}

	public void auditEntity(JavaClass javaClass) {
		if(javaClass.hasAnnotation(Audited.class))throw new IllegalStateException("The element '"
				+ javaClass.getName() + "' is already annotated with @"
				+ Audited.class.getSimpleName());
		javaClass.addImport(Audited.class);
		javaClass.addAnnotation(Audited.class);
	}

	public void auditField(Field<JavaClass> field){
		if(field.hasAnnotation(Audited.class))throw new IllegalStateException("The element '"
				+ field.getName() + "' is already annotated with @"
				+ Audited.class.getSimpleName());
		field.getOrigin().addImport(Audited.class);
		field.addAnnotation(Audited.class);
	}

	public void setupDefaultProperties(PipeOut out,Project project)
	{
		FileResource<?> persistencceXml;
		ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		persistencceXml = resourceFacet.getResource("META-INF/persistence.xml");
		File underlyingResourceObject = persistencceXml.getUnderlyingResourceObject();
		for (int i = 0; i < 5; i++) {
		}
		ShellMessages.success(out, "default envers persistence xml properties are configure correctly.");
	}

}
