/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A map-like data structure that wraps a backing map and caches values while iterating through
 * {@link #unmodifiableKeySet()}. By design, the cache is cleared when this structure is mutated. If
 * this structure is never mutated, it provides a thread-safe view of the backing map.
 *
 * <p>The {@link MapIteratorCache} assumes ownership of the backing map, and cannot guarantee
 * correctness in the face of external mutations to the backing map. As such, it is <b>strongly</b>
 * recommended that the caller does not persist a reference to the backing map (unless the backing
 * map is immutable).
 *
 * <p>This class is tailored toward use cases in common.graph. It is *NOT* a general purpose map.
 *
 * @author James Sexton
 */
@AnnotatedFor("mutability")
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
class MapIteratorCache<K extends @Immutable Object, V> {
  private final Map<K, V> backingMap;

  /*
   * Per JDK: "the behavior of a map entry is undefined if the backing map has been modified after
   * the entry was returned by the iterator, except through the setValue operation on the map entry"
   * As such, this field must be cleared before every map mutation.
   *
   * Note about volatile: volatile doesn't make it safe to read from a mutable graph in one thread
   * while writing to it in another. All it does is help with _reading_ from multiple threads
   * concurrently. For more information, see AbstractNetworkTest.concurrentIteration.
   */
  @CheckForNull private transient volatile Entry<K, V> cacheEntry;

  MapIteratorCache(@ReceiverDependentMutable Map<K, V> backingMap) {
    this.backingMap = checkNotNull(backingMap);
  }

  @CanIgnoreReturnValue
  @CheckForNull
  final V put(@Mutable MapIteratorCache<K, V> this, K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    clearCache();
    return backingMap.put(key, value);
  }

  @CanIgnoreReturnValue
  @CheckForNull
  final V remove(@Mutable MapIteratorCache<K, V> this, @Readonly Object key) {
    checkNotNull(key);
    clearCache();
    return backingMap.remove(key);
  }

  final void clear(@Mutable MapIteratorCache<K, V> this) {
    clearCache();
    backingMap.clear();
  }

  @CheckForNull
  V get(@Readonly MapIteratorCache<K, V> this, @Readonly Object key) {
    checkNotNull(key);
    V value = getIfCached(key);
    // TODO(b/192579700): Use a ternary once it no longer confuses our nullness checker.
    if (value == null) {
      return getWithoutCaching(key);
    } else {
      return value;
    }
  }

  @CheckForNull
  final V getWithoutCaching(@Readonly MapIteratorCache<K, V> this, @Readonly Object key) {
    checkNotNull(key);
    return backingMap.get(key);
  }

  final boolean containsKey(@Readonly MapIteratorCache<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
    return getIfCached(key) != null || backingMap.containsKey(key);
  }

  final @Readonly Set<K> unmodifiableKeySet() {
    return new AbstractSet<K>() {
      @Override
      public UnmodifiableIterator<K> iterator() {
        Iterator<Entry<K, V>> entryIterator = backingMap.entrySet().iterator();

        return new UnmodifiableIterator<K>() {
          @Override
          public boolean hasNext() {
            return entryIterator.hasNext();
          }

          @Override
          public K next() {
            Entry<K, V> entry = entryIterator.next(); // store local reference for thread-safety
            cacheEntry = entry;
            return entry.getKey();
          }
        };
      }

      @Override
      public @NonNegative int size() {
        return backingMap.size();
      }

      @Override
      public boolean contains(@CheckForNull @UnknownSignedness @Readonly Object key) {
        return containsKey(key);
      }
    };
  }

  // Internal methods (package-visible, but treat as only subclass-visible)

  @CheckForNull
  V getIfCached(@Readonly MapIteratorCache<K, V> this, @CheckForNull @Readonly Object key) {
    Entry<K, V> entry = cacheEntry; // store local reference for thread-safety

    // Check cache. We use == on purpose because it's cheaper and a cache miss is ok.
    if (entry != null && entry.getKey() == key) {
      return entry.getValue();
    }
    return null;
  }

  void clearCache(@Mutable MapIteratorCache<K, V> this) {
    cacheEntry = null;
  }
}
