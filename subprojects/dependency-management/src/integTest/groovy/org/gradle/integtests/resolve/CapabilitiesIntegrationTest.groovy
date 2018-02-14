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

package org.gradle.integtests.resolve

import groovy.transform.NotYetImplemented
import org.gradle.integtests.fixtures.GradleMetadataResolveRunner
import org.gradle.integtests.fixtures.RequiredFeature
import org.gradle.integtests.fixtures.RequiredFeatures

class CapabilitiesIntegrationTest extends AbstractModuleDependencyResolveTest {

    def setup() {
        buildFile << """
            configurations { conf }      
        """
        executer.withStackTraceChecksDisabled()
    }

    def "can choose between cglib and cglib-nodep by declaring capabilities"() {
        given:
        repository {
            'cglib:cglib-nodep:3.2.5'()
            'cglib:cglib:3.2.5'()
        }

        buildFile << """

            dependencies {
               conf "cglib:cglib-nodep:3.2.5"
               conf "cglib:cglib:3.2.5"
            
               constraints {
                  capabilities {
                     capability('cglib') {
                        it.providedBy 'cglib:cglib'
                        it.providedBy 'cglib:cglib-nodep'
                        it.prefer 'cglib:cglib'
                     }
                  }
               }
            }
        """

        when:
        repositoryInteractions {
            'cglib:cglib:3.2.5' {
                expectGetMetadata()
            }
        }
        run 'dependencyInsight', '--configuration', 'conf', '--dependency', 'cglib'

        then:
        outputContains """cglib:cglib:3.2.5 (capability cglib is provided by cglib:cglib-nodep and cglib:cglib)
"""
    }

    def "fails resolution if 2 libraries provide the same capability"() {
        given:
        repository {
            'cglib:cglib-nodep:3.2.5'()
            'cglib:cglib:3.2.5'()
        }

        buildFile << """
            dependencies {
               conf "cglib:cglib-nodep:3.2.5"
               conf "cglib:cglib:3.2.5"
            
               constraints {
                  capabilities {
                     capability('cglib') {
                        it.providedBy 'cglib:cglib'
                        it.providedBy 'cglib:cglib-nodep'
                     }
                  }
               }
            }
        """

        when:
        fails 'dependencyInsight', '--configuration', 'conf', '--dependency', 'cglib'

        then:
        failure.assertHasCause("Cannot choose between cglib:cglib-nodep or cglib:cglib because they provide the same capability: cglib")
    }

    @RequiredFeatures(
        @RequiredFeature(feature = GradleMetadataResolveRunner.GRADLE_METADATA, value="true")
    )
    def "published module can declare relocation"() {
        given:
        repository {
            'asm:asm:3.0'()
            'org.ow2.asm:asm:4.0' {
                variant('api') {
                    capability('asm') {
                        providedBy 'asm:asm'
                        providedBy 'org.ow2.asm:asm'
                        prefer 'org.ow2.asm:asm'
                    }
                }
                variant('runtime') {
                    capability('asm') {
                        providedBy 'asm:asm'
                        providedBy 'org.ow2.asm:asm'
                        prefer 'org.ow2.asm:asm'
                    }
                }
            }
        }

        buildFile << """

            dependencies {
               conf "asm:asm:3.0"
               conf "org.ow2.asm:asm:4.0"
            }
        """

        when:
        repositoryInteractions {
            'asm:asm:3.0' {
                expectGetMetadata()
            }
            'org.ow2.asm:asm:4.0' {
                expectGetMetadata()
            }
        }
        run 'dependencyInsight', '--configuration', 'conf', '--dependency', 'asm'

        then:
        outputContains """org.ow2.asm:asm:4.0 (capability asm is provided by asm:asm and org.ow2.asm:asm)
"""

    }

    @RequiredFeatures(
        @RequiredFeature(feature = GradleMetadataResolveRunner.GRADLE_METADATA, value="true")
    )
    def "external module can provide resolution of relocated module"() {
        given:
        repository {
            'asm:asm:3.0'()
            'org.ow2.asm:asm:4.0'()
            'org.test:platform:1.0' {
                variant('api') {
                    capability('asm') {
                        providedBy 'asm:asm'
                        providedBy 'org.ow2.asm:asm'
                        prefer 'org.ow2.asm:asm'
                    }
                }
                variant('runtime') {
                    capability('asm') {
                        providedBy 'asm:asm'
                        providedBy 'org.ow2.asm:asm'
                        prefer 'org.ow2.asm:asm'
                    }
                }
            }
        }

        buildFile << """

            dependencies {
               conf 'org.test:platform:1.0'
               conf "asm:asm:3.0"
               conf "org.ow2.asm:asm:4.0"
            }
        """

        when:
        repositoryInteractions {
            'org.test:platform:1.0' {
                expectGetMetadata()
            }
            'asm:asm:3.0' {
                expectGetMetadata()
            }
            'org.ow2.asm:asm:4.0' {
                expectGetMetadata()
            }
        }
        run 'dependencyInsight', '--configuration', 'conf', '--dependency', 'asm'

        then:
        outputContains """org.ow2.asm:asm:4.0 (capability asm is provided by asm:asm and org.ow2.asm:asm)
"""
    }

    def "can select groovy-all over individual groovy-whatever"() {
        given:
        repository {
            'org.apache:groovy:1.0'()
            'org.apache:groovy-json:1.0'()
            'org.apache:groovy-xml:1.0'()
            'org.apache:groovy-all:1.0'()

            'org:a:1.0' {
                dependsOn 'org.apache:groovy:1.0'
                dependsOn 'org.apache:groovy-json:1.0'
            }
            'org:b:1.0' {
                dependsOn 'org.apache:groovy-all:1.0'
            }
        }

        buildFile << """
            dependencies {
               conf "org:a:1.0"
               conf "org:b:1.0"
            
               constraints {
                  capabilities {
                     capability('groovy') {
                        it.providedBy 'org.apache:groovy'
                        it.providedBy 'org.apache:groovy-xml'
                        it.providedBy 'org.apache:groovy-json'
                        it.providedBy 'org.apache:groovy-all'
                     
                        it.prefer 'org.apache:groovy-all'
                     }
                  }
               }
            }
        """

        when:
        repositoryInteractions {
            'org:a:1.0' {
                expectResolve()
            }
            'org:b:1.0' {
                expectResolve()
            }
            'org.apache:groovy:1.0' {
                expectGetMetadata()
            }
            'org.apache:groovy-json:1.0' {
                expectGetMetadata()
            }
            'org.apache:groovy-all:1.0' {
                expectResolve()
            }
        }

        then:
        run ':checkDeps'
        resolve.expectGraph {
            root(":", ":test:") {
                module('org:a:1.0') {
                    edge('org.apache:groovy:1.0', 'org.apache:groovy-all:1.0')
                    edge('org.apache:groovy-json:1.0', 'org.apache:groovy-all:1.0')
                }
                module('org:b:1.0') {
                    edge('org.apache:groovy-all:1.0', 'org.apache:groovy-all:1.0')
                }
            }
        }

    }

    def "can select individual groovy-whatever over individual groovy-all"() {
        given:
        repository {
            'org.apache:groovy:1.0'()
            'org.apache:groovy-json:1.0'()
            'org.apache:groovy-xml:1.0'()
            'org.apache:groovy-all:1.0'()

            'org:a:1.0' {
                dependsOn 'org.apache:groovy:1.0'
                dependsOn 'org.apache:groovy-json:1.0'
            }
            'org:b:1.0' {
                dependsOn 'org.apache:groovy-all:1.0'
            }
        }

        buildFile << """
            dependencies {
               conf "org:a:1.0"
               conf "org:b:1.0"
            
               constraints {
                  capabilities {
                     capability('groovy-json') {
                        it.providedBy 'org.apache:groovy-json'
                        it.providedBy 'org.apache:groovy-all'
                     
                        it.prefer 'org.apache:groovy-json'
                     }
                     
                     capability('groovy') {
                        it.providedBy 'org.apache:groovy'
                        it.providedBy 'org.apache:groovy-all'
                     
                        it.prefer 'org.apache:groovy'
                     }
                  }
               }
            }
        """

        when:
        repositoryInteractions {
            'org:a:1.0' {
                expectResolve()
            }
            'org:b:1.0' {
                expectResolve()
            }
            'org.apache:groovy:1.0' {
                expectResolve()
            }
            'org.apache:groovy-json:1.0' {
                expectResolve()
            }
        }

        then:
        run ':checkDeps'
        resolve.expectGraph {
            root(":", ":test:") {
                module('org:a:1.0') {
                    edge('org.apache:groovy:1.0', 'org.apache:groovy:1.0')
                    edge('org.apache:groovy-json:1.0', 'org.apache:groovy-json:1.0')
                }
                module('org:b:1.0') {
                    // this is not quite right, as we should replace with 2 edges
                    // one option to do it is to construct "adhoc" modules, and select an adhoc target in "prefer"
                    // where this adhoc target would have dependencies on groovy-json and groovy
                    edge('org.apache:groovy-all:1.0', 'org.apache:groovy-json:1.0')
                }
            }
        }

    }

    @RequiredFeatures(
        @RequiredFeature(feature=GradleMetadataResolveRunner.GRADLE_METADATA, value="true")
    )
    def "forcefully upgrade dependency"() {
        given:
        repository {
            'org:dep:1.0.0'()
            'org:dep:1.0.1'()
            'org:platform-rules:1.0' {
                constraint(group:'org', artifact:'dep', version:'1.0.1', reason:'critical vulnerability in 1.0.0', rejects:['1.0.0'])
            }
        }

        buildFile << """
            dependencies {
                conf 'org:platform-rules:1.0'
                conf 'org:dep:1.0.0'
            }
        """

        when:
        repositoryInteractions {
            'org:dep:1.0.0' {
                expectGetMetadata()
            }
            'org:dep:1.0.1' {
                expectResolve()
            }
            'org:platform-rules:1.0' {
                expectResolve()
            }
        }

        then:
        run ':checkDeps'

        and:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org:platform-rules:1.0') {
                    module('org:dep:1.0.1')
                }
                edge('org:dep:1.0.0', 'org:dep:1.0.1') {
                    byReason('critical vulneradility in 1.0.0')
                }
            }
        }
    }

    @RequiredFeatures(
        @RequiredFeature(feature=GradleMetadataResolveRunner.GRADLE_METADATA, value="true")
    )
    @NotYetImplemented
    def "can express preference for capabilities declared in published modules"() {
        given:
        repository {
            'cglib:cglib-nodep:3.2.5' {
                variant('api') {
                    capability('cglib') {
                        providedBy 'cglib:cglib-nodep'
                    }
                }
                variant('runtime') {
                    capability('cglib') {
                        providedBy 'cglib:cglib-nodep'
                    }
                }
            }
            'cglib:cglib:3.2.5' {
                variant('api') {
                    capability('cglib') {
                        providedBy 'cglib:cglib'
                    }
                }
                variant('runtime') {
                    capability('cglib') {
                        providedBy 'cglib:cglib'
                    }
                }
            }
        }

        buildFile << """

            dependencies {
               conf "cglib:cglib-nodep:3.2.5"
               conf "cglib:cglib:3.2.5"
            
               constraints {
                  capabilities {
                     capability('cglib') {
                        it.prefer 'cglib:cglib'
                     }
                  }
               }
            }
        """

        when:
        repositoryInteractions {
            'cglib:cglib:3.2.5' {
                expectResolve()
            }
            'cglib:cglib-nodep:3.2.5' {
                expectGetMetadata()
            }
        }
        run 'dependencyInsight', '--configuration', 'conf', '--dependency', 'cglib'

        then:
        outputContains """cglib:cglib:3.2.5 (capability cglib is provided by cglib:cglib-nodep and cglib:cglib)
"""
    }

}
