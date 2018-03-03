/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.ide.xcode

import org.gradle.ide.xcode.fixtures.AbstractXcodeIntegrationSpec
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition


@Requires(TestPrecondition.XCODE)
class XcodeCompositeBuildIntegrationTest extends AbstractXcodeIntegrationSpec {

    def "creates workspace with Xcode project for each project in build"() {
        given:
        settingsFile << """
            include 'app', 'greeter', 'empty'
            includeBuild 'util'
            includeBuild 'other'
        """

        buildFile << """
            allprojects {
                apply plugin: 'xcode'
            }
            apply plugin: 'swift-application' 
            project(':app') {
                apply plugin: 'swift-application'
                dependencies {
                    implementation project(':greeter')
                }
            }
            project(':greeter') {
                apply plugin: 'swift-library'
                dependencies {
                    implementation 'test:util:1.3'
                }
            }
"""

        buildTestFixture.withBuildInSubDir()
        singleProjectBuild("util") {
            settingsFile << "rootProject.name = 'util'"
            buildFile << """
                apply plugin: 'swift-library'
                apply plugin: 'xcode'
                group = 'test'
                version = '1.3'
            """
        }
        singleProjectBuild("other") {
            settingsFile << "rootProject.name = 'other'"
            buildFile << """
                apply plugin: 'swift-application'
                apply plugin: 'xcode'
            """
        }

        when:
        succeeds("xcode")

        then:
        result.assertTasksExecuted(":xcodeProject", ":xcodeProjectWorkspaceSettings", ":xcodeScheme",
            ":app:xcodeProjectWorkspaceSettings", ":app:xcodeProject", ":app:xcodeScheme", ":app:xcode",
            ":greeter:xcodeProjectWorkspaceSettings", ":greeter:xcodeProject", ":greeter:xcodeScheme", ":greeter:xcode",
            ":empty:xcodeProjectWorkspaceSettings", ":empty:xcodeProject", ":empty:xcode",
            // TODO - shouldn't be present (unless all of the other compile tasks are present as well)
            ":util:compileDebugSwift",
            ":util:xcodeProjectWorkspaceSettings", ":util:xcodeProject", ":util:xcodeScheme",
            ":other:xcodeProjectWorkspaceSettings", ":other:xcodeProject", ":other:xcodeScheme",
            ":xcodeWorkspaceWorkspaceSettings", ":xcodeWorkspace", ":xcode")

        rootXcodeWorkspace.contentFile.assertHasProjects("${rootProjectName}.xcodeproj", 'app/app.xcodeproj', 'greeter/greeter.xcodeproj', 'empty/empty.xcodeproj', 'util/util.xcodeproj', 'other/other.xcodeproj')

        xcodeProject("${rootProjectName}.xcodeproj")
        xcodeProject("app/app.xcodeproj")
        xcodeProject("greeter/greeter.xcodeproj")
        xcodeProject("empty/empty.xcodeproj")
        xcodeProject("util/util.xcodeproj")
        xcodeProject("other/other.xcodeproj")
    }
}
