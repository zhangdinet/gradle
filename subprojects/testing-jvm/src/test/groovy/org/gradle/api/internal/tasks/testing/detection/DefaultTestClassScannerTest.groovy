/*
 * Copyright 2010 the original author or authors.
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


package org.gradle.api.internal.tasks.testing.detection

import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.DefaultFileVisitDetails
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import spock.lang.Specification

class DefaultTestClassScannerTest extends Specification {
    private final TestFrameworkDetector detector = Mock()
    private final TestClassProcessor processor = Mock()
    private final FileTree files = Mock()

    void passesEachClassFileToTestClassDetector() {
        DefaultTestClassScanner scanner = new DefaultTestClassScanner(files, detector, processor, [] as Set)

        when:
        scanner.run()

        then:
        1 * files.visit(_) >> { args ->
            FileVisitor visitor = args[0]
            assert visitor
            visitor.visitFile(mockFileVisitDetails('class1'))
            visitor.visitFile(mockFileVisitDetails('class2'))
        }
        then:
        1 * detector.startDetection(processor)
        then:
        1 * detector.processTestClass(new File("class1.class"))
        then:
        1 * detector.processTestClass(new File("class2.class"))

        0 * _._
    }

    void previousFailedClassesPassedToTestClassDetectorFirst() {
        DefaultTestClassScanner scanner = new DefaultTestClassScanner(files, detector, processor, ['Class3'] as Set)

        when:
        scanner.run()

        then:
        1 * files.visit(_) >> { args ->
            FileVisitor visitor = args[0]
            assert visitor
            visitor.visitFile(mockFileVisitDetails('Class1'))
            visitor.visitFile(mockFileVisitDetails('Class2'))
            visitor.visitFile(mockFileVisitDetails('Class3'))
        }
        then:
        1 * detector.startDetection(processor)
        then:
        1 * detector.processTestClass(new File("Class3.class"))
        then:
        1 * detector.processTestClass(new File("Class1.class"))
        then:
        1 * detector.processTestClass(new File("Class2.class"))

        0 * _._
    }

    FileVisitDetails mockFileVisitDetails(String className) {
        return new DefaultFileVisitDetails(new File("${className}.class"), new RelativePath(false, "${className}.class"), null, null, null)
    }
}
