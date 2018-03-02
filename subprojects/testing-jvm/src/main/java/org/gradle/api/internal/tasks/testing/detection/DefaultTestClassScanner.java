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

package org.gradle.api.internal.tasks.testing.detection;

import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.tasks.testing.DefaultTestClassRunInfo;
import org.gradle.api.internal.tasks.testing.TestClassProcessor;
import org.gradle.internal.Pair;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The default test class scanner. Depending on the availability of a test framework detector,
 * a detection or filename scan is performed to find test classes.
 */
public class DefaultTestClassScanner implements Runnable {
    private final FileTree candidateClassFiles;
    private final TestFrameworkDetector testFrameworkDetector;
    private final TestClassProcessor testClassProcessor;
    private final Set<String> previousFailedTestClasses;

    public DefaultTestClassScanner(FileTree candidateClassFiles, TestFrameworkDetector testFrameworkDetector,
                                   TestClassProcessor testClassProcessor, Set<String> previousFailedTestClasses) {
        this.candidateClassFiles = candidateClassFiles;
        this.testFrameworkDetector = testFrameworkDetector;
        this.testClassProcessor = testClassProcessor;
        this.previousFailedTestClasses = previousFailedTestClasses;
    }

    @Override
    public void run() {
        Set<Pair<String, File>> previousFailedTests = new LinkedHashSet<Pair<String, File>>();
        Set<Pair<String, File>> otherTests = new LinkedHashSet<Pair<String, File>>();

        getAllTestClasses(previousFailedTests, otherTests);

        if (testFrameworkDetector == null) {
            filenameScan(previousFailedTests);
            filenameScan(otherTests);
        } else {
            testFrameworkDetector.startDetection(testClassProcessor);
            detectionScan(previousFailedTests);
            detectionScan(otherTests);
        }
    }

    private void getAllTestClasses(final Set<Pair<String, File>> previousFailedTests, final Set<Pair<String, File>> otherTests) {
        candidateClassFiles.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(FileVisitDetails fileDetails) {
                final File file = fileDetails.getFile();

                if (file.getAbsolutePath().endsWith(".class")) {
                    String className = fileDetails.getRelativePath().getPathString().replaceAll("\\.class", "").replace('/', '.');
                    if (previousFailedTestClasses.contains(className)) {
                        previousFailedTests.add(Pair.of(className, fileDetails.getFile()));
                    } else {
                        otherTests.add(Pair.of(className, fileDetails.getFile()));
                    }
                }
            }
        });
    }

    private void filenameScan(Set<Pair<String, File>> tests) {
        for (Pair<String, File> test : tests) {
            testClassProcessor.processTestClass(new DefaultTestClassRunInfo(test.getLeft()));
        }
    }

    private void detectionScan(Set<Pair<String, File>> tests) {
        for (Pair<String, File> test : tests) {
            testFrameworkDetector.processTestClass(test.getRight());
        }
    }
}
