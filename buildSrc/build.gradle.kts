plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

gradlePlugin {
    plugins {
        create("msjava", Action {
            id = "dev.magicspells.msjava"
            implementationClass = "dev.magicspells.gradle.MSJavaPlugin"
        })
    }
}
