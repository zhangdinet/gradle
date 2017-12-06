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

package org.gradle.plugins.ide.tooling.r45

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.model.idea.IdeaContentRoot
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaProject

class ToolingApiIdeaModelCrossVersionSpec extends ToolingApiSpecification {

    @ToolingApiVersion(">=4.5")
    @TargetGradleVersion(">=4.5")
    def "provides source dir information"() {

        file('build.gradle').text = "apply plugin: 'java'"

        projectDir.create {
            src {
                main {
                    java {}
                    resources {}
                }
                test {
                    java {}
                    resources {}
                }
            }
        }

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }
        IdeaModule module = project.children[0]
        IdeaContentRoot root = module.contentRoots[0]

        then:
        root.sourceDirectories.size() == 1
        root.sourceDirectories.first().directory == file('src/main/java')
        root.resourceDirectories.size() == 1
        root.resourceDirectories.first().directory == file('src/main/resources')

        root.testDirectories.size() == 1
        root.testDirectories.first().directory == file('src/test/java')
        root.testResourceDirectories.size() == 1
        root.testResourceDirectories.first().directory == file('src/test/resources')
    }
}
