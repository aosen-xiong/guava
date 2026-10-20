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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.CFComment;

/**
 * A skeleton {@code Multimap} implementation, not necessarily in terms of a {@code Map}.
 *
 * @author Louis Wasserman
 */
@AnnotatedFor({"nullness", "mutability"})
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    implements Multimap<K, V> {
  @Pure
  @Override
  public boolean isEmpty(@Readonly AbstractMultimap<K, V> this) {
    return size() == 0;
  }

  @Pure
  @Override
  public boolean containsValue(@Readonly AbstractMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
    for (Collection<V> collection : asMap().values()) {
      if (collection.contains(value)) {
        return true;
      }
    }

    return false;
  }

  @Pure
  @Override
  public boolean containsEntry(@Readonly AbstractMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.contains(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@Mutable AbstractMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
    Collection<V> collection = asMap().get(key);
    return collection != null && collection.remove(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean put(@Mutable AbstractMultimap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
    return get(key).add(value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(@Mutable AbstractMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    checkNotNull(values);
    // make sure we only call values.iterator() once
    // and we only call get(key) if values is nonempty
    if (values instanceof Collection) {
      Collection<? extends V> valueCollection = (Collection<? extends V>) values;
      return !valueCollection.isEmpty() && get(key).addAll(valueCollection);
    } else {
      Iterator<? extends V> valueItr = values.iterator();
      return valueItr.hasNext() && Iterators.addAll(get(key), valueItr);
    }
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(@Mutable AbstractMultimap<K, V> this, @Readonly Multimap<? extends K, ? extends V> multimap) {
    boolean changed = false;
    for (Entry<? extends K, ? extends V> entry : multimap.entries()) {
      changed |= put(entry.getKey(), entry.getValue());
    }
    return changed;
  }

  @CanIgnoreReturnValue
  @Override
  public @Readonly Collection<V> replaceValues(@Mutable AbstractMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    checkNotNull(values);
    Collection<V> result = removeAll(key);
    putAll(key, values);
    return result;
  }

  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Collection<@ReceiverDependentMutable Entry<K, V>> entries;

  @SideEffectFree
  @Override
  public @ReceiverDependentMutable Collection<@ReceiverDependentMutable Entry<K, V>> entries(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    Collection<@ReceiverDependentMutable Entry<K, V>> result = entries;
    return (result == null) ? entries = createEntries() : result;
  }

  abstract @ReceiverDependentMutable Collection<@ReceiverDependentMutable Entry<K, V>> createEntries(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  @WeakOuter
  @ReceiverDependentMutable
  class Entries extends Multimaps.Entries<K, V> {
    @Override
    @ReceiverDependentMutable Multimap<K, V> multimap(@ReceiverDependentMutable AbstractMultimap<K,V>.Entries this) {
      return AbstractMultimap.this;
    }

    @Override
    public @ReceiverDependentMutable Iterator<@ReceiverDependentMutable Entry<K, V>> iterator(@ReceiverDependentMutable AbstractMultimap<K,V>.Entries this) {
      return entryIterator();
    }

    @Override
    public Spliterator<@ReceiverDependentMutable Entry<K, V>> spliterator(@ReceiverDependentMutable AbstractMultimap<K,V>.Entries this) {
      return entrySpliterator();
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  class EntrySet extends Entries implements Set<@ReceiverDependentMutable Entry<K, V>> {
    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly EntrySet this) {
      return Sets.hashCodeImpl(this);
    }

    @Pure
    @Override
    public boolean equals(@Readonly EntrySet this, @CheckForNull @UnknownSignedness @Readonly Object obj) {
      return Sets.equalsImpl(this, obj);
    }
  }

  abstract @ReceiverDependentMutable Iterator<@ReceiverDependentMutable Entry<K, V>> entryIterator(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  Spliterator<@ReceiverDependentMutable Entry<K, V>> entrySpliterator(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    return Spliterators.spliterator(
        entryIterator(), size(), (this instanceof SetMultimap) ? Spliterator.DISTINCT : 0);
  }

  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Set<K> keySet;

  @SideEffectFree
  @Override
  public @ReceiverDependentMutable Set<K> keySet(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    Set<K> result = keySet;
    return (result == null) ? keySet = createKeySet() : result;
  }

  @SideEffectFree
  abstract @ReceiverDependentMutable Set<K> createKeySet(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Multiset<K> keys;

  @Override
  public @ReceiverDependentMutable Multiset<K> keys(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    Multiset<K> result = keys;
    return (result == null) ? keys = createKeys() : result;
  }

  abstract @ReceiverDependentMutable Multiset<K> createKeys(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Collection<V> values;

  @SideEffectFree
  @Override
  public @ReceiverDependentMutable Collection<V> values(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    Collection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }

  abstract @ReceiverDependentMutable Collection<V> createValues(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  @WeakOuter
  @ReceiverDependentMutable
  class Values extends AbstractCollection<V> {
    @Override
    public @ReceiverDependentMutable Iterator<V> iterator(@ReceiverDependentMutable AbstractMultimap<K, V>.@ReceiverDependentMutable Values this) {
      return valueIterator();
    }

    @Pure
    @Override
    public Spliterator<V> spliterator(@ReceiverDependentMutable Values this) {
      return valueSpliterator();
    }

    @Override
    public @NonNegative int size(@Readonly Values this) {
      return AbstractMultimap.this.size();
    }

    @Pure
    @Override
    public boolean contains(@Readonly Values this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      return AbstractMultimap.this.containsValue(o);
    }

    @Override
    public void clear(@Mutable Values this) {
      AbstractMultimap.this.clear();
    }
  }

  @SuppressWarnings("mutability:return.type.incompatible") // Maps.valueIterator wraps the entries' iterator and forwards remove() to it, so it has this multimap's mutability
  @ReceiverDependentMutable Iterator<V> valueIterator(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    return Maps.valueIterator(entries().iterator());
  }

  Spliterator<V> valueSpliterator(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    return Spliterators.spliterator(valueIterator(), size(), 0);
  }

  @LazyInit @CheckForNull private transient @Assignable Map<K, @ReceiverDependentMutable Collection<V>> asMap;

  @Override
  public @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> asMap(@ReceiverDependentMutable AbstractMultimap<K, V> this) {
    Map<K, @ReceiverDependentMutable Collection<V>> result = asMap;
    return (result == null) ? asMap = createAsMap() : result;
  }

  abstract @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> createAsMap(@ReceiverDependentMutable AbstractMultimap<K, V> this);

  // Comparison and hashing

  @Pure
  @Override
  public boolean equals(@Readonly AbstractMultimap<K, V> this, @CheckForNull @Readonly Object object) {
    return Multimaps.equalsImpl(this, object);
  }

  /**
   * Returns the hash code for this multimap.
   *
   * <p>The hash code of a multimap is defined as the hash code of the map view, as returned by
   * {@link Multimap#asMap}.
   *
   * @see Map#hashCode
   */
  @Pure
  @Override
  public int hashCode(@UnknownSignedness @Readonly AbstractMultimap<K, V> this) {
    return asMap().hashCode();
  }

  /**
   * Returns a string representation of the multimap, generated by calling {@code toString} on the
   * map returned by {@link Multimap#asMap}.
   *
   * @return a string representation of the multimap
   */
  @Pure
  @Override
  public String toString(@Readonly AbstractMultimap<K, V> this) {
    return asMap().toString();
  }
}
