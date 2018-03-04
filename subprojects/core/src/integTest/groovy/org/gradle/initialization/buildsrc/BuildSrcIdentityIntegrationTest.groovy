/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization.buildsrc

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class BuildSrcIdentityIntegrationTest extends AbstractIntegrationSpec {
    def "includes build identifier in error message on failure to resolve dependencies of build"() {
        def m = mavenRepo.module("org.test", "test", "1.2")

        given:
        def buildSrc = file("buildSrc/build.gradle")
        buildSrc << """
            repositories {
                maven { url '$mavenRepo.uri' }
            }

            dependencies {
                implementation "org.test:test:1.2"
            }
        """
        file("buildSrc/src/main/java/Thing.java") << "class Thing { }"

        when:
        fails()

        then:
        failure.assertHasDescription("Could not resolve all files for configuration ':buildSrc:runtimeClasspath'.")
        failure.assertHasCause("""Could not find org.test:test:1.2.
Searched in the following locations:
    ${m.pom.file.toURL()}
    ${m.artifact.file.toURL()}
Required by:
    project :buildSrc""")

        when:
        m.publish()
        m.artifact.file.delete()

        fails()

        then:
        failure.assertHasDescription("Could not resolve all files for configuration ':buildSrc:runtimeClasspath'.")
        failure.assertHasCause("Could not find test.jar (org.test:test:1.2).")
    }

    def "includes build identifier in task failure error message"() {
        def buildSrc = file("buildSrc/build.gradle")
        buildSrc << """
            classes.doLast {
                throw new RuntimeException("broken")
            }
        """

        when:
        fails()

        then:
        failure.assertHasDescription("Execution failed for task ':buildSrc:classes'.")
        failure.assertHasCause("broken")
    }

    def "includes build identifier in dependency resolution results"() {
        given:
        file("buildSrc/settings.gradle") << """
            include 'a'
        """
        file("buildSrc/build.gradle") << """
            dependencies {
                implementation project(':a')
            }
            project(':a') {
                apply plugin: 'java'
            }
            classes.doLast {
                def components = configurations.compileClasspath.incoming.resolutionResult.allComponents.id
                assert components.size() == 2
                assert components[0].build.name == 'buildSrc'
                assert components[0].projectPath == ':'
                assert components[0].projectName == 'buildSrc'
                assert components[1].build.name == 'buildSrc'
                assert components[1].projectPath == ':a'
                assert components[1].projectName == 'a'
            }
        """

        expect:
        succeeds()
    }
}
