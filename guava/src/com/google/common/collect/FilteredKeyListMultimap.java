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
import java.util.List;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.mutability.qual.Mutable;

/**
 * Implementation of {@link Multimaps#filterKeys(ListMultimap, Predicate)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class FilteredKeyListMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends FilteredKeyMultimap<K, V> implements ListMultimap<K, V> {
  FilteredKeyListMultimap(@ReceiverDependentMutable ListMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
    super(unfiltered, keyPredicate);
  }

  @Override
  public @ReceiverDependentMutable ListMultimap<K, V> unfiltered() {
    return (@ReceiverDependentMutable ListMultimap<K, V>) super.unfiltered();
  }

  @Override
  public @ReceiverDependentMutable List<V> get(@ParametricNullness K key) {
    return (@ReceiverDependentMutable List<V>) super.get(key);
  }

  @Override
  public @Readonly List<V> removeAll(@Mutable FilteredKeyListMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    return (@Readonly List<V>) super.removeAll(key);
  }

  @Override
  public @Readonly List<V> replaceValues(@Mutable FilteredKeyListMultimap<K, V> this, @ParametricNullness K key, @Readonly Iterable<? extends V> values) {
    return (@Readonly List<V>) super.replaceValues(key, values);
  }
}
