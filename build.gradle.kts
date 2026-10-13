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
    maven("https://maven.notenoughupdates.org/releases")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.bundles.fabric)
    implementation(include("org.notenoughupdates.moulconfig:modern-26.2:4.7.2")!!)
    compileOnly(libs.modMenu)
    implementation(include(libs.caffeine.get())!!)
    implementation(include(libs.rhino.get())!!)
    implementation(include(libs.jna.get())!!)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

loom {
    accessWidenerPath.set(file("src/main/resources/starredheltix.accesswidener"))
}

tasks.processResources {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile> {
    options.release = 25
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
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
