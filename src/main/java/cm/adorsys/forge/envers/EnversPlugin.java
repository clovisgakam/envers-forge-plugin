package cm.adorsys.forge.envers;

import java.util.Arrays;

import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellPrompt;
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

/**
 *@author clovis gakam
 */
@Alias("envers")
@RequiresProject
@Help("This plugin will help you setting up Envers .")
@RequiresFacet({ResourceFacet.class, DependencyFacet.class, JavaSourceFacet.class})
public class EnversPlugin implements Plugin
{
   @Inject
   private ShellPrompt prompt;
   
   @Inject
   private Project project;
   
   @Inject
   private Shell shell;

   @SetupCommand
   public void setupCommand(@PipeIn String in, PipeOut out, @Option String... args)
   {
      out.println("Executed default command.");
   }
   
   @DefaultCommand
   public void defaultCommand(@PipeIn String in, PipeOut out)
   {
      out.println("Executed default command.");
   }

   @Command("audit-entity")
   public void auditEntiyCommand(@PipeIn String in, PipeOut out, @Option String... args)
   {
      if (args == null)
         out.println("Executed named command without args.");
      else
         out.println("Executed named command with args: " + Arrays.asList(args));
   }

   @Command("audit-field")
   public void auditFieldCommand(@PipeIn String in, PipeOut out)
   {
      if (prompt.promptBoolean("Do you like writing Forge plugins?"))
         out.println("I am happy.");
      else
         out.println("I am sad.");
   }
}
