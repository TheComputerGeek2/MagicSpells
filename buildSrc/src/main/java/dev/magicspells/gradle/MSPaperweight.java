package dev.magicspells.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MSPaperweight implements Plugin<Project> {
    // TODO figure out how to apply the plugin
    @Override
    public void apply(Project project) {
        project.getDependencies().add("paperweightDevelopmentBundle", "io.papermc.paper:dev-bundle:1.19.4-R0.1-SNAPSHOT").because("We need a server implementation rather than just an api here.");
    }
}
