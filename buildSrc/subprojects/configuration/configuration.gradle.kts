plugins {
    `java-gradle-plugin`
}

apply { plugin("org.gradle.kotlin.kotlin-dsl") }

dependencies {
    implementation(project(":kotlinDsl"))
}
