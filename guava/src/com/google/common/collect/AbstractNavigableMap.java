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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Skeletal implementation of {@link NavigableMap}.
 *
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtIncompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractNavigableMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends IteratorBasedAbstractMap<K, V> implements NavigableMap<K, V> {

  @Override
  @CheckForNull
  public abstract V get(@Readonly AbstractNavigableMap<K,V> this, @CheckForNull @UnknownSignedness @Readonly Object key);

  @Override
  @CheckForNull
  public Entry<K, V> firstEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(entryIterator(), null);
  }

  @Override
  @CheckForNull
  public Entry<K, V> lastEntry() {
    return Iterators.<@Nullable Entry<K, V>>getNext(descendingEntryIterator(), null);
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollFirstEntry(@Mutable AbstractNavigableMap<K,V> this) {
    return Iterators.pollNext(entryIterator());
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollLastEntry(@Mutable AbstractNavigableMap<K,V> this) {
    return Iterators.pollNext(descendingEntryIterator());
  }

  @Override
  @ParametricNullness
  public @KeyFor("this") K firstKey(@Readonly AbstractNavigableMap<K,V> this) {
    Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @ParametricNullness
  public @KeyFor("this") K lastKey(@Readonly AbstractNavigableMap<K,V> this) {
    Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @CheckForNull
  public @PolyMutable Entry<K, V> lowerEntry(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  @CheckForNull
  public @PolyMutable Entry<K, V> floorEntry(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  @CheckForNull
  public @PolyMutable Entry<K, V> ceilingEntry(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  @CheckForNull
  public @PolyMutable Entry<K, V> higherEntry(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  @CheckForNull
  public K lowerKey(@Readonly AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return Maps.keyOrNull(lowerEntry(key));
  }

  @Override
  @CheckForNull
  public K floorKey(@Readonly AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return Maps.keyOrNull(floorEntry(key));
  }

  @Override
  @CheckForNull
  public K ceilingKey(@Readonly AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return Maps.keyOrNull(ceilingEntry(key));
  }

  @Override
  @CheckForNull
  public K higherKey(@Readonly AbstractNavigableMap<K,V> this, @ParametricNullness K key) {
    return Maps.keyOrNull(higherEntry(key));
  }

  abstract Iterator<@PolyMutable Entry<K, V>> descendingEntryIterator(@PolyMutable AbstractNavigableMap<K,V> this);

  @Override
  public @PolyMutable SortedMap<K, V> subMap(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K fromKey, @ParametricNullness K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public @PolyMutable SortedMap<K, V> headMap(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public @PolyMutable SortedMap<K, V> tailMap(@PolyMutable AbstractNavigableMap<K,V> this, @ParametricNullness K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public @PolyMutable NavigableSet<@KeyFor({"this"}) K> navigableKeySet(@PolyMutable AbstractNavigableMap<K,V> this) {
    return new Maps.NavigableKeySet<>(this);
  }

  @Override
  public @PolyMutable Set<@KeyFor({"this"}) K> keySet(@PolyMutable AbstractNavigableMap<K,V> this) {
    return navigableKeySet();
  }

  @Override
  public @PolyMutable NavigableSet<@KeyFor({"this"}) K> descendingKeySet(@PolyMutable AbstractNavigableMap<K,V> this) {
    return descendingMap().navigableKeySet();
  }

  @Override
  public @PolyMutable NavigableMap<K, V> descendingMap(@PolyMutable AbstractNavigableMap<K,V> this) {
    return new @PolyMutable DescendingMap();
  }

  @ReceiverDependentMutable
  private final class DescendingMap extends Maps.DescendingMap<K, V> {
    @Override
    @PolyMutable NavigableMap<K, V> forward(@PolyMutable AbstractNavigableMap<K, V>.DescendingMap this) {
      return AbstractNavigableMap.this;
    }

    @Override
    Iterator<@PolyMutable Entry<K, V>> entryIterator(@PolyMutable AbstractNavigableMap<K, V>.DescendingMap this) {
      return descendingEntryIterator();
    }
  }
}
