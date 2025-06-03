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
    @Override
    public void apply(Project target) {
        target.getPlugins().apply(JavaPlugin.class);
        target.getPlugins().apply(JavaLibraryPlugin.class);
        target.getPlugins().apply(MavenPublishPlugin.class);
        target.getExtensions().configure(JavaPluginExtension.class, (JavaPluginExtension ext) -> {
            ext.toolchain((javaToolchainSpec -> {
                javaToolchainSpec.getLanguageVersion().set(JavaLanguageVersion.of(21));
            }));
        });
        RepositoryHandler repositories = target.getRepositories();
        repositories.mavenCentral();

        String[] mavenUrls = new String[] {
            "https://repo.dmulloy2.net/nexus/repository/public/",
            "https://repo.md-5.net/content/repositories/releases/",
            "https://repo.papermc.io/repository/maven-public/",
            "https://oss.sonatype.org/content/repositories/central",
            "https://oss.sonatype.org/content/repositories/snapshots",
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
            "https://jitpack.io",
            "https://repo.codemc.org/repository/maven-public",
            "https://cdn.rawgit.com/Rayzr522/maven-repo/master/",
            "https://maven.enginehub.org/repo/",
            "https://repo.glaremasters.me/repository/towny/",
            "https://repo.extendedclip.com/content/repositories/placeholderapi",
            "https://repo.md-5.net/content/repositories/snapshots",
        };
        for (String url : mavenUrls) {
            repositories.maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(url));
        }
    }
}
