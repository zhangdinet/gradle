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
package org.gradle.api.internal.file.collections

import spock.lang.Specification

class ImmutableFileCollectionTest extends Specification {
    def collectionContainsFixedSetOfFiles() {
        File file1 = new File('file1')
        File file2 = new File('file2')

        expect:
        ImmutableFileCollection collection = new ImmutableFileCollection(file1, file2)
        collection.files as List == [file1, file2]
    }

    def "cannot add file to collection"() {
        given:
        File file = new File('file1')
        ImmutableFileCollection collection = new ImmutableFileCollection()

        when:
        collection.files.add(file)

        then:
        thrown(UnsupportedOperationException)
        collection.files.size() == 0
    }
}
