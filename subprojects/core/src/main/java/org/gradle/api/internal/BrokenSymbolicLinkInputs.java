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

package org.gradle.api.internal;

import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot;
import org.gradle.api.internal.changedetection.state.NormalizedFileSnapshot;
import org.gradle.internal.file.FileType;

import javax.annotation.Nullable;
import java.util.Map;

public class BrokenSymbolicLinkInputs {
    private final String propertyName;
    private final String brokenSymbolicLinkPath;

    public BrokenSymbolicLinkInputs(String propertyName, String brokenSymbolicLinkPath) {
        this.propertyName = propertyName;
        this.brokenSymbolicLinkPath = brokenSymbolicLinkPath;
    }

    @Nullable
    public static BrokenSymbolicLinkInputs detect(Map<String, FileCollectionSnapshot> taskOutputs) {
        for (Map.Entry<String, FileCollectionSnapshot> entry : taskOutputs.entrySet()) {
            String propertyName = entry.getKey();
            FileCollectionSnapshot beforeExecution = entry.getValue();
            BrokenSymbolicLinkInputs brokenSymbolicLinkInputs = detect(propertyName, beforeExecution);
            if (brokenSymbolicLinkInputs != null) {
                return brokenSymbolicLinkInputs;
            }
        }
        return null;
    }

    @Nullable
    private static BrokenSymbolicLinkInputs detect(String propertyName, FileCollectionSnapshot beforeExecution) {
        Map<String, NormalizedFileSnapshot> beforeSnapshots = beforeExecution.getSnapshots();

        for (Map.Entry<String, NormalizedFileSnapshot> beforeSnapshot : beforeSnapshots.entrySet()) {
            NormalizedFileSnapshot fileSnapshot = beforeSnapshot.getValue();

            if (fileSnapshot.getSnapshot().getType() == FileType.BrokenSymbolicLink) {
                return new BrokenSymbolicLinkInputs(propertyName, fileSnapshot.getNormalizedPath());
            }
        }
        return null;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getBrokenSymbolicLinkPath() {
        return brokenSymbolicLinkPath;
    }

    public String toString() {
        return String.format("output property '%s' with path '%s'", propertyName, brokenSymbolicLinkPath);
    }
}
