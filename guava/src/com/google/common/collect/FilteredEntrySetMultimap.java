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
import com.google.common.base.Predicate;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Implementation of {@link Multimaps#filterEntries(SetMultimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class FilteredEntrySetMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends FilteredEntryMultimap<K, V> implements FilteredSetMultimap<K, V> {

  FilteredEntrySetMultimap(@ReceiverDependentMutable SetMultimap<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
    super(unfiltered, predicate);
  }

  @Override
  public @PolyMutable SetMultimap<K, V> unfiltered(@PolyMutable FilteredEntrySetMultimap<K, V> this) {
    return (@PolyMutable SetMultimap<K, V>) unfiltered;
  }

  @Override
  public @PolyMutable Set<V> get(@PolyMutable FilteredEntrySetMultimap<K, V> this, @ParametricNullness K key) {
    return (@PolyMutable Set<V>) super.get(key);
  }

  @Override
  public @Readonly Set<V> removeAll(@Mutable FilteredEntrySetMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    return (@Readonly Set<V>) super.removeAll(key);
  }

  @Override
  public Set<V> replaceValues(@Mutable FilteredEntrySetMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    return (Set<V>) super.replaceValues(key, values);
  }

  @Override
  @PolyMutable Set<@PolyMutable Entry<K, V>> createEntries(@PolyMutable FilteredEntrySetMultimap<K, V> this) {
    return Sets.filter(unfiltered().entries(), entryPredicate());
  }

  @Override
  public @PolyMutable Set<@PolyMutable Entry<K, V>> entries(@PolyMutable FilteredEntrySetMultimap<K, V> this) {
    return (@PolyMutable Set<@PolyMutable Entry<K, V>>) super.entries();
  }
}
