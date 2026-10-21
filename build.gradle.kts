plugins {
    java
}

group = "me.slemm"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API — works on Purpur too (Purpur is a Paper fork).
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    // Uses whatever JDK runs Gradle (25+), same as your SiftDimension build.
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
