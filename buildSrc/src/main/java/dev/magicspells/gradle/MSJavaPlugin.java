package dev.magicspells.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

public class MSJavaPlugin implements Plugin<Project> {

    private static final String[] MAVEN_URLS = new String[]{
        "https://mvn.lib.co.nz/public/",
        "https://repo.papermc.io/repository/maven-public/",
        "https://jitpack.io",
        "https://repo.codemc.org/repository/maven-public",
        "https://maven.enginehub.org/repo/",
        "https://repo.glaremasters.me/repository/towny/",
        "https://repo.extendedclip.com/releases/",
    };

    @Override
    public void apply(Project target) {
        target.getPlugins().apply(JavaPlugin.class);
        target.getPlugins().apply(JavaLibraryPlugin.class);
        target.getPlugins().apply(MavenPublishPlugin.class);

        target.getExtensions()
            .getByType(JavaPluginExtension.class)
            .getToolchain()
            .getLanguageVersion()
            .set(JavaLanguageVersion.of(25));

        RepositoryHandler repositories = target.getRepositories();
        repositories.mavenCentral();
	    for (String url : MAVEN_URLS) {
            repositories.maven(repo -> repo.setUrl(url));
        }
    }

}
