import org.gradle.cleanup.EmptyDirectoryCheck

plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":dependencyManagement"))
    api(project(":plugins"))
    api(project(":pluginUse"))
    api(project(":publish"))

    implementation(Libraries.slf4jApi.coordinates)
    implementation(Libraries.groovy.coordinates)
    implementation(Libraries.ivy.coordinates)
    implementation(Libraries.maven3.coordinates)
    implementation(Libraries.pmavenCommon.coordinates)
    implementation(Libraries.pmavenGroovy.coordinates)
    implementation(Libraries.maven3WagonFile.coordinates)
    implementation(Libraries.maven3WagonHttp.coordinates)
    implementation(Libraries.plexusContainer.coordinates)
    implementation(Libraries.aetherConnector.coordinates)

    testImplementation(TestLibraries.xmlunit)

    integTestImplementation(project(":ear"))
    integTestRuntimeOnly(project(":resourcesS3"))
    integTestRuntimeOnly(project(":resourcesSftp"))

    testFixturesImplementation(project(":internalIntegTesting"))
}

testFixtures {
    from(":core")
    from(":modelCore")
}

val verifyTestFilesCleanup by tasks.getting(EmptyDirectoryCheck::class) {
    isErrorWhenNotEmpty = false
}

