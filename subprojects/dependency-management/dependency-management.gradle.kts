import org.gradle.build.ClasspathManifest
import org.gradle.cleanup.EmptyDirectoryCheck

plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    api(project(":core"))
    api(project(":versionControl"))

    implementation(project(":resources"))
    implementation(project(":resourcesHttp"))

    implementation(Libraries.asm.coordinates)
    implementation(Libraries.asmCommons.coordinates)
    implementation(Libraries.asmUtil.coordinates)
    implementation(Libraries.commonsLang.coordinates)
    implementation(Libraries.commonsIo.coordinates)
    implementation(Libraries.ivy.coordinates)
    implementation(Libraries.slf4jApi.coordinates)
    implementation(Libraries.gson.coordinates)
    implementation(Libraries.jcip.coordinates)
    implementation(Libraries.maven3.coordinates)

    runtimeOnly(Libraries.bouncycastleProvider.coordinates)
    runtimeOnly(project(":installationBeacon"))

    testImplementation(Libraries.nekohtml.coordinates)

    integTestRuntimeOnly(project(":ivy"))
    integTestRuntimeOnly(project(":maven"))
    integTestRuntimeOnly(project(":resourcesS3"))
    integTestRuntimeOnly(project(":resourcesSftp"))
    integTestRuntimeOnly(project(":testKit"))

    testFixturesImplementation(project(":internalIntegTesting"))
}

testFixtures {
    from(":core")
    from(":messaging")
    from(":modelCore")
    from(":versionControl")
}

val verifyTestFilesCleanup by tasks.getting(EmptyDirectoryCheck::class) {
    isErrorWhenNotEmpty = false
}

val classpathManifest by tasks.getting(ClasspathManifest::class) {
    additionalProjects = listOf(project(":runtimeApiInfo"))
}
