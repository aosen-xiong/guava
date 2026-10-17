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
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.mutability.qual.Mutable;

/**
 * Implementation of {@link Multimaps#filterKeys(SetMultimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class FilteredKeySetMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
    extends FilteredKeyMultimap<K, V> implements FilteredSetMultimap<K, V> {

  FilteredKeySetMultimap(@ReceiverDependentMutable SetMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
    super(unfiltered, keyPredicate);
  }

  @Override
  public @ReceiverDependentMutable SetMultimap<K, V> unfiltered() {
    return (@ReceiverDependentMutable SetMultimap<K, V>) unfiltered;
  }

  @Override
  public @ReceiverDependentMutable Set<V> get(@ParametricNullness K key) {
    return (@ReceiverDependentMutable Set<V>) super.get(key);
  }

  @Override
  public @Readonly Set<V> removeAll(@Mutable FilteredKeySetMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    return (@Readonly Set<V>) super.removeAll(key);
  }

  @Override
  public Set<V> replaceValues(@Mutable FilteredKeySetMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    return (Set<V>) super.replaceValues(key, values);
  }

  @Override
  public @ReceiverDependentMutable Set<@ReceiverDependentMutable Entry<K, V>> entries() {
    return (@ReceiverDependentMutable Set<@ReceiverDependentMutable Entry<K, V>>) super.entries();
  }

  @Override
  Set<Entry<K, V>> createEntries() {
    return new EntrySet();
  }

  class EntrySet extends Entries implements Set<Entry<K, V>> {
    @Override
    public int hashCode(@UnknownSignedness EntrySet this) {
      return Sets.hashCodeImpl(this);
    }

    @Override
    public boolean equals(@CheckForNull @UnknownSignedness @Readonly Object o) {
      return Sets.equalsImpl(this, o);
    }
  }
}
