/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Basic implementation of a {@link SortedSetMultimap} with a sorted key set.
 *
 * <p>This superclass allows {@code TreeMultimap} to override methods to return navigable set and
 * map types in non-GWT only, while GWT code will inherit the SortedMap/SortedSet overrides.
 *
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractSortedKeySortedSetMultimap<
        K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends AbstractSortedSetMultimap<K, V> {

  AbstractSortedKeySortedSetMultimap(@ReceiverDependentMutable SortedMap<K, @ReceiverDependentMutable Collection<V>> map) {
    super(map);
  }

  @Override
  public @PolyMutable SortedMap<K, @PolyMutable Collection<V>> asMap(@PolyMutable AbstractSortedKeySortedSetMultimap<K, V> this) {
    return (@PolyMutable SortedMap<K, @PolyMutable Collection<V>>) super.asMap();
  }

  @Override
  @PolyMutable SortedMap<K, @PolyMutable Collection<V>> backingMap(@PolyMutable AbstractSortedKeySortedSetMultimap<K, V> this) {
    return (@PolyMutable SortedMap<K, @PolyMutable Collection<V>>) super.backingMap();
  }

  @Override
  public @PolyMutable SortedSet<K> keySet(@PolyMutable AbstractSortedKeySortedSetMultimap<K, V> this) {
    return (@PolyMutable SortedSet<K>) super.keySet();
  }

  @Override
  @PolyMutable Set<K> createKeySet(@PolyMutable AbstractSortedKeySortedSetMultimap<K, V> this) {
    return createMaybeNavigableKeySet();
  }
}
