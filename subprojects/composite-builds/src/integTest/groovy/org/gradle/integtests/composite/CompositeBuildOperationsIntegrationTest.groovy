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

package org.gradle.integtests.composite

import org.gradle.initialization.ConfigureBuildBuildOperationType
import org.gradle.initialization.LoadBuildBuildOperationType
import org.gradle.integtests.fixtures.build.BuildTestFile
import org.gradle.internal.execution.ExecuteTaskBuildOperationType
import org.gradle.internal.operations.trace.BuildOperationRecord
import org.gradle.internal.taskgraph.CalculateTaskGraphBuildOperationType
import org.gradle.util.CollectionUtils

import java.util.regex.Pattern

class CompositeBuildOperationsIntegrationTest extends AbstractCompositeBuildIntegrationTest {
    BuildTestFile buildB

    def setup() {
        buildB = multiProjectBuild("buildB", ['b1', 'b2']) {
            buildFile << """
                allprojects {
                    apply plugin: 'java'
                }
"""
        }
        includedBuilds << buildB
    }

    def "generates build operations for tasks in included builds"() {
        given:
        dependency 'org.test:buildB:1.0'

        when:
        execute(buildA, ":jar", [])

        then:
        executed ":buildB:jar"

        and:
        List<BuildOperationRecord> allOps = operations.all(ExecuteTaskBuildOperationType)

        allOps.find { it.details.buildPath == ":buildB" && it.details.taskPath == ":jar" }
        allOps.find { it.details.buildPath == ":" && it.details.taskPath == ":jar" }

        for (BuildOperationRecord operationRecord : allOps) {
            assertChildrenNotIn(operationRecord, operationRecord, allOps)
        }
    }

    def "generates configure, task graph and run tasks operations for included builds"() {
        given:
        dependency 'org.test:buildB:1.0'

        when:
        execute(buildA, ":jar", [])

        then:
        executed ":buildB:jar"

        and:
        def root = CollectionUtils.single(operations.roots())

        def loadOps = operations.all(LoadBuildBuildOperationType)
        loadOps.size() == 2
        loadOps[0].displayName == "Load build"
        loadOps[0].details.buildPath == ":"
        loadOps[0].parentId == root.id
        loadOps[1].displayName == "Load build (buildB)"
        loadOps[1].details.buildPath == ":buildB"
        loadOps[1].parentId == root.id

        def configureOps = operations.all(ConfigureBuildBuildOperationType)
        configureOps.size() == 2
        configureOps[0].displayName == "Configure build (buildB)"
        configureOps[0].details.buildPath == ":buildB"
        configureOps[0].parentId == root.id
        configureOps[1].displayName == "Configure build"
        configureOps[1].details.buildPath == ":"
        configureOps[1].parentId == root.id

        def taskGraphOps = operations.all(CalculateTaskGraphBuildOperationType)
        taskGraphOps.size() == 2
        taskGraphOps[0].displayName == "Calculate task graph"
        taskGraphOps[0].details.buildPath == ":"
        taskGraphOps[0].parentId == root.id
        taskGraphOps[1].displayName == "Calculate task graph (:buildB)"
        taskGraphOps[1].details.buildPath == ":buildB"
        taskGraphOps[1].parentId == root.id

        def runTasksOps = operations.all(Pattern.compile("Run tasks.*"))
        runTasksOps.size() == 2
        runTasksOps[0].displayName == "Run tasks"
        runTasksOps[0].parentId == root.id
        runTasksOps[1].displayName == "Run tasks (:buildB)"
        runTasksOps[1].parentId == root.id
    }

    def assertChildrenNotIn(BuildOperationRecord origin, BuildOperationRecord op, List<BuildOperationRecord> allOps) {
        for (BuildOperationRecord child : op.children) {
            assert !allOps.contains(child) : "Task operation $origin has child $child which is also a task operation"
            assertChildrenNotIn(origin, child, allOps)
        }
    }

}
