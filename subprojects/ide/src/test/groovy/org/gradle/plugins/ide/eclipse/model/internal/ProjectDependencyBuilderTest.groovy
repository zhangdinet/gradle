/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.plugins.ide.eclipse.model.internal

import org.gradle.internal.component.local.model.LocalComponentArtifactMetadata
import org.gradle.internal.component.model.DefaultIvyArtifactName
import org.gradle.plugins.ide.internal.IdeArtifactRegistry
import org.gradle.test.fixtures.AbstractProjectBuilderSpec

import static org.gradle.internal.component.local.model.TestComponentIdentifiers.newProjectId

class ProjectDependencyBuilderTest extends AbstractProjectBuilderSpec {
    def projectId = newProjectId(":nested:project-name")
    def artifactRegistry = Mock(IdeArtifactRegistry)
    def builder = new ProjectDependencyBuilder(artifactRegistry)

    def "should create dependency using project name for project without eclipse plugin applied"() {
        when:
        def dependency = builder.build(projectId)

        then:
        dependency.path == "/project-name"

        and:
        artifactRegistry.getIdeArtifactMetadata(_, "eclipse.project") >> null
    }

    def "should create dependency using eclipse projectName"() {
        given:
        def projectArtifact = Stub(LocalComponentArtifactMetadata) {
            getName() >> new DefaultIvyArtifactName("foo", "eclipse.project", "project", null)
        }
        artifactRegistry.getIdeArtifactMetadata(projectId, "eclipse.project") >> projectArtifact

        when:
        def dependency = builder.build(projectId)

        then:
        dependency.path == '/foo'
    }
}
