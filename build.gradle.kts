plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlin)
}

val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project

version = mod_version
group = maven_group
base.archivesName.set(archives_base_name)

repositories {
    mavenCentral()
    maven("https://maven.parchmentmc.org")
    maven("https://maven.notenoughupdates.org/releases")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.21.10:2025.10.12@zip")
    })
    modImplementation(libs.bundles.fabric)
    modImplementation(include("org.notenoughupdates.moulconfig:modern-1.21.10:4.2.0-beta")!!)
    modApi(libs.modMenu)
    implementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
}

loom {
    accessWidenerPath.set(file("src/main/resources/starredheltix.accesswidener"))
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("aiSecret", libs.versions.aiSecret.get())
    
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
    
    filesMatching("assets/starredheltix/internal/data.bin") {
        expand("aiSecret" to libs.versions.aiSecret.get())
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${archives_base_name}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archives_base_name
            from(components["java"])
        }
    }
}
