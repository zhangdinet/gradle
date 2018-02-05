/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.internal.component.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.HasAttributes;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributeValue;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A stateless attribute matcher, which optimizes for the case of only comparing 0 or 1 candidates and delegates to {@link MultipleCandidateMatcher} for all other cases.
 */
public class ComponentAttributeMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentAttributeMatcher.class);

    /**
     * Determines whether the given candidate is compatible with the requested criteria, according to the given schema.
     */
    public boolean isMatching(AttributeSelectionSchema schema, AttributeContainerInternal candidate, AttributeContainerInternal requested) {
        if (requested.isEmpty() || candidate.isEmpty()) {
            return true;
        }

        ImmutableAttributes requestedAttributes = requested.asImmutable();
        ImmutableAttributes candidateAttributes = candidate.asImmutable();

        for (Attribute<?> attribute : requestedAttributes.keySet()) {
            AttributeValue<?> requestedValue = requestedAttributes.findEntry(attribute);
            AttributeValue<?> candidateValue = candidateAttributes.findEntry(attribute.getName());
            if (candidateValue.isPresent()) {
                Object coercedValue = candidateValue.coerce(attribute);
                boolean match = schema.matchValue(attribute, requestedValue.get(), coercedValue);
                if (!match) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Selects the candidates from the given set that are compatible with the requested criteria, according to the given schema.
     */
    public <T extends HasAttributes> List<T> match(AttributeSelectionSchema schema, Collection<? extends T> candidates, AttributeContainerInternal requested, @Nullable T fallback) {
        if (candidates.size() == 0) {
            if (fallback != null && isMatching(schema, (AttributeContainerInternal) fallback.getAttributes(), requested)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No candidates for {}, selected matching fallback {}", requested, fallback);
                }
                return ImmutableList.of(fallback);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No candidates for {} and fallback {} does not match. Select nothing.", requested, fallback);
            }
            return ImmutableList.of();
        }

        if (candidates.size() == 1) {
            T candidate = candidates.iterator().next();
            if (isMatching(schema, (AttributeContainerInternal) candidate.getAttributes(), requested)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Selected match {} from candidates {} for {}", candidate, candidates, requested);
                }
                return Collections.singletonList(candidate);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Selected match [] from candidates {} for {}", candidates, requested);
            }
            return ImmutableList.of();
        }

        ImmutableAttributes requestedAttributes = requested.asImmutable();
        List<CacheEntry> cand = Lists.newArrayListWithCapacity(candidates.size());
        int i = 0;
        for (T candidate : candidates) {
            cand.add(new CacheEntry(i++, candidate.getAttributes()));
        }
        CacheKey key = new CacheKey(requestedAttributes, cand);

        List<CacheEntry> cached = cache.get(key);
        if (cached == null) {
            cached = new MultipleCandidateMatcher<CacheEntry>(schema, cand, requestedAttributes).getMatches();
            cache.put(key, cached);
        }
        List<T> matches = Lists.newArrayListWithCapacity(cached.size());
        Iterator<? extends T> it = candidates.iterator();
        i = 0;
        for (CacheEntry entry : cached) {
            while (i++ < entry.index) {
                it.next();
            }
            matches.add(it.next());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Selected matches {} from candidates {} for {}", matches, candidates, requested);
        }
        return matches;
    }

    private final Map<CacheKey, List<CacheEntry>> cache = Maps.newHashMap();

    private static class CacheKey {
        private final ImmutableAttributes requested;
        private final List<CacheEntry> entries;

        private CacheKey(ImmutableAttributes requested, List<CacheEntry> entries) {
            this.requested = requested;
            this.entries = entries;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            if (!requested.equals(cacheKey.requested)) {
                return false;
            }
            return entries.equals(cacheKey.entries);
        }

        @Override
        public int hashCode() {
            int result = requested.hashCode();
            result = 31 * result + entries.hashCode();
            return result;
        }
    }

    private static class CacheEntry implements HasAttributes {
        private final int index;
        private final AttributeContainer attributes;

        private CacheEntry(int index, AttributeContainer attributes) {
            this.index = index;
            this.attributes = attributes;
        }

        @Override
        public AttributeContainer getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheEntry that = (CacheEntry) o;

            if (index != that.index) {
                return false;
            }
            return attributes.equals(that.attributes);
        }

        @Override
        public int hashCode() {
            int result = index;
            result = 31 * result + attributes.hashCode();
            return result;
        }
    }

}
