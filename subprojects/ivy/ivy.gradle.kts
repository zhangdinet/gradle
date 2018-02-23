import org.gradle.cleanup.EmptyDirectoryCheck

plugins {
    `java-library`
    id("classycle")
}

dependencies {
    api(project(":core"))
    api(project(":publish"))
    api(project(":plugins")) // for base plugin to get archives conf
    api(project(":pluginUse"))
    api(project(":dependencyManagement"))

    implementation(Libraries.ivy.coordinates)

    integTestImplementation(project(":ear"))
    integTestRuntimeOnly(project(":resourcesS3"))
    integTestRuntimeOnly(project(":resourcesSftp"))
    testFixturesImplementation(project(":internalIntegTesting"))
}

testFixtures {
    from(":core")
    from(":modelCore")
    from(":platformBase")
}

val verifyTestFilesCleanup by tasks.getting(EmptyDirectoryCheck::class) {
    isErrorWhenNotEmpty = false
}
