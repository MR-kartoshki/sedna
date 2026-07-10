plugins {
    java
}

val semver: String by project
val packageGroup = "li.cil.sedna"

fun getGitRef(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}

version = "${semver}+${getGitRef()}"
group = packageGroup

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/fnuecke/ceres")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
        content { includeGroup("li.cil.ceres") }
    }
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")

    implementation("commons-io:commons-io:2.11.0")
    implementation("it.unimi.dsi:fastutil:8.5.6")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.logging.log4j:log4j-api:2.15.0")
    implementation("org.apache.logging.log4j:log4j-core:2.15.0")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm:9.8")

    implementation("li.cil.ceres:ceres:0.0.4")

    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

tasks.test {
    useJUnitPlatform()
}
