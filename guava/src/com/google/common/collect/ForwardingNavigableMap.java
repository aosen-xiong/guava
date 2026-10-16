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

import static com.google.common.collect.Maps.keyOrNull;

import com.google.common.annotations.GwtIncompatible;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A navigable map which forwards all its method calls to another navigable map. Subclasses should
 * override one or more methods to modify the behavior of the backing map as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingNavigableMap} forward <i>indiscriminately</i>
 * to the methods of the delegate. For example, overriding {@link #put} alone <i>will not</i> change
 * the behavior of {@link #putAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code putAll} as well, either providing your own implementation, or delegating to the
 * provided {@code standardPutAll} method.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingNavigableMap}.
 *
 * <p>Each of the {@code standard} methods uses the map's comparator (or the natural ordering of the
 * elements, if there is no comparator) to test element equality. As a result, if the comparator is
 * not consistent with equals, some of the standard implementations may violate the {@code Map}
 * contract.
 *
 * <p>The {@code standard} methods and the collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Louis Wasserman
 * @since 12.0
 */
@AnnotatedFor("mutability")
@GwtIncompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
public abstract class ForwardingNavigableMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends ForwardingSortedMap<K, V> implements NavigableMap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingNavigableMap() {}

  @Override
  protected abstract @PolyMutable NavigableMap<K, V> delegate(@PolyMutable ForwardingNavigableMap<K, V> this);

  @Override
  @CheckForNull
  public Entry<K, V> lowerEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().lowerEntry(key);
  }

  /**
   * A sensible definition of {@link #lowerEntry} in terms of the {@code lastEntry()} of {@link
   * #headMap(Object, boolean)}. If you override {@code headMap}, you may wish to override {@code
   * lowerEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardLowerEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  @CheckForNull
  public K lowerKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().lowerKey(key);
  }

  /**
   * A sensible definition of {@link #lowerKey} in terms of {@code lowerEntry}. If you override
   * {@link #lowerEntry}, you may wish to override {@code lowerKey} to forward to this
   * implementation.
   */
  @CheckForNull
  protected K standardLowerKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return keyOrNull(lowerEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> floorEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().floorEntry(key);
  }

  /**
   * A sensible definition of {@link #floorEntry} in terms of the {@code lastEntry()} of {@link
   * #headMap(Object, boolean)}. If you override {@code headMap}, you may wish to override {@code
   * floorEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardFloorEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  @CheckForNull
  public K floorKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().floorKey(key);
  }

  /**
   * A sensible definition of {@link #floorKey} in terms of {@code floorEntry}. If you override
   * {@code floorEntry}, you may wish to override {@code floorKey} to forward to this
   * implementation.
   */
  @CheckForNull
  protected K standardFloorKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return keyOrNull(floorEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> ceilingEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().ceilingEntry(key);
  }

  /**
   * A sensible definition of {@link #ceilingEntry} in terms of the {@code firstEntry()} of {@link
   * #tailMap(Object, boolean)}. If you override {@code tailMap}, you may wish to override {@code
   * ceilingEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardCeilingEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  @CheckForNull
  public K ceilingKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().ceilingKey(key);
  }

  /**
   * A sensible definition of {@link #ceilingKey} in terms of {@code ceilingEntry}. If you override
   * {@code ceilingEntry}, you may wish to override {@code ceilingKey} to forward to this
   * implementation.
   */
  @CheckForNull
  protected K standardCeilingKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return keyOrNull(ceilingEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> higherEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().higherEntry(key);
  }

  /**
   * A sensible definition of {@link #higherEntry} in terms of the {@code firstEntry()} of {@link
   * #tailMap(Object, boolean)}. If you override {@code tailMap}, you may wish to override {@code
   * higherEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardHigherEntry(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  @CheckForNull
  public K higherKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return delegate().higherKey(key);
  }

  /**
   * A sensible definition of {@link #higherKey} in terms of {@code higherEntry}. If you override
   * {@code higherEntry}, you may wish to override {@code higherKey} to forward to this
   * implementation.
   */
  @CheckForNull
  protected K standardHigherKey(@Readonly ForwardingNavigableMap<K, V> this, @ParametricNullness K key) {
    return keyOrNull(higherEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> firstEntry(@Readonly ForwardingNavigableMap<K, V> this) {
    return delegate().firstEntry();
  }

  /**
   * A sensible definition of {@link #firstEntry} in terms of the {@code iterator()} of {@link
   * #entrySet}. If you override {@code entrySet}, you may wish to override {@code firstEntry} to
   * forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardFirstEntry() {
    return Iterables.<@Nullable Entry<K, V>>getFirst(entrySet(), null);
  }

  /**
   * A sensible definition of {@link #firstKey} in terms of {@code firstEntry}. If you override
   * {@code firstEntry}, you may wish to override {@code firstKey} to forward to this
   * implementation.
   */
  protected K standardFirstKey(@Readonly ForwardingNavigableMap<K, V> this) {
    Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @CheckForNull
  public Entry<K, V> lastEntry(@Readonly ForwardingNavigableMap<K, V> this) {
    return delegate().lastEntry();
  }

  /**
   * A sensible definition of {@link #lastEntry} in terms of the {@code iterator()} of the {@link
   * #entrySet} of {@link #descendingMap}. If you override {@code descendingMap}, you may wish to
   * override {@code lastEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardLastEntry() {
    return Iterables.<@Nullable Entry<K, V>>getFirst(descendingMap().entrySet(), null);
  }

  /**
   * A sensible definition of {@link #lastKey} in terms of {@code lastEntry}. If you override {@code
   * lastEntry}, you may wish to override {@code lastKey} to forward to this implementation.
   */
  protected K standardLastKey(@Readonly ForwardingNavigableMap<K, V> this) {
    Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    } else {
      return entry.getKey();
    }
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollFirstEntry(@Mutable ForwardingNavigableMap<K, V> this) {
    return delegate().pollFirstEntry();
  }

  /**
   * A sensible definition of {@link #pollFirstEntry} in terms of the {@code iterator} of {@code
   * entrySet}. If you override {@code entrySet}, you may wish to override {@code pollFirstEntry} to
   * forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardPollFirstEntry(@Mutable ForwardingNavigableMap<K, V> this) {
    return Iterators.pollNext(entrySet().iterator());
  }

  @Override
  @CheckForNull
  public Entry<K, V> pollLastEntry(@Mutable ForwardingNavigableMap<K, V> this) {
    return delegate().pollLastEntry();
  }

  /**
   * A sensible definition of {@link #pollFirstEntry} in terms of the {@code iterator} of the {@code
   * entrySet} of {@code descendingMap}. If you override {@code descendingMap}, you may wish to
   * override {@code pollFirstEntry} to forward to this implementation.
   */
  @CheckForNull
  protected Entry<K, V> standardPollLastEntry(@Mutable ForwardingNavigableMap<K, V> this) {
    return Iterators.pollNext(descendingMap().entrySet().iterator());
  }

  @Override
  public @PolyMutable NavigableMap<K, V> descendingMap(@PolyMutable ForwardingNavigableMap<K, V> this) {
    return delegate().descendingMap();
  }

  /**
   * A sensible implementation of {@link NavigableMap#descendingMap} in terms of the methods of this
   * {@code NavigableMap}. In many cases, you may wish to override {@link
   * ForwardingNavigableMap#descendingMap} to forward to this implementation or a subclass thereof.
   *
   * <p>In particular, this map iterates over entries with repeated calls to {@link
   * NavigableMap#lowerEntry}. If a more efficient means of iteration is available, you may wish to
   * override the {@code entryIterator()} method of this class.
   *
   * @since 12.0
   */
  protected class StandardDescendingMap extends Maps.DescendingMap<K, V> {
    /** Constructor for use by subclasses. */
    public StandardDescendingMap() {}

    @Override
    @PolyMutable NavigableMap<K, V> forward(@PolyMutable StandardDescendingMap this) {
      return ForwardingNavigableMap.this;
    }

    @Override
    public void replaceAll(@Mutable StandardDescendingMap this, BiFunction<? super K, ? super V, ? extends V> function) {
      forward().replaceAll(function);
    }

    @Override
    protected Iterator<@PolyMutable Entry<K, V>> entryIterator(@PolyMutable StandardDescendingMap this) {
      return new Iterator<@PolyMutable Entry<K, V>>() {
        @CheckForNull private Entry<K, V> toRemove = null;
        @CheckForNull private Entry<K, V> nextOrNull = forward().lastEntry();

        @Override
        public boolean hasNext() {
          return nextOrNull != null;
        }

        @Override
        public java.util.Map.Entry<K, V> next() {
          if (nextOrNull == null) {
            throw new NoSuchElementException();
          }
          try {
            return nextOrNull;
          } finally {
            toRemove = nextOrNull;
            nextOrNull = forward().lowerEntry(nextOrNull.getKey());
          }
        }

        @Override
        public void remove() {
          if (toRemove == null) {
            throw new IllegalStateException("no calls to next() since the last call to remove()");
          }
          forward().remove(toRemove.getKey());
          toRemove = null;
        }
      };
    }
  }

  @Override
  public @PolyMutable NavigableSet<@KeyFor({"this"}) K> navigableKeySet(@PolyMutable ForwardingNavigableMap<K, V> this) {
    return delegate().navigableKeySet();
  }

  /**
   * A sensible implementation of {@link NavigableMap#navigableKeySet} in terms of the methods of
   * this {@code NavigableMap}. In many cases, you may wish to override {@link
   * ForwardingNavigableMap#navigableKeySet} to forward to this implementation or a subclass
   * thereof.
   *
   * @since 12.0
   */
  protected class StandardNavigableKeySet extends Maps.NavigableKeySet<K, V> {
    /** Constructor for use by subclasses. */
    public StandardNavigableKeySet() {
      super(ForwardingNavigableMap.this);
    }
  }

  @Override
  public @PolyMutable NavigableSet<@KeyFor({"this"}) K> descendingKeySet(@PolyMutable ForwardingNavigableMap<K, V> this) {
    return delegate().descendingKeySet();
  }

  /**
   * A sensible definition of {@link #descendingKeySet} as the {@code navigableKeySet} of {@link
   * #descendingMap}. (The {@link StandardDescendingMap} implementation implements {@code
   * navigableKeySet} on its own, so as not to cause an infinite loop.) If you override {@code
   * descendingMap}, you may wish to override {@code descendingKeySet} to forward to this
   * implementation.
   */
  protected NavigableSet<K> standardDescendingKeySet() {
    return descendingMap().navigableKeySet();
  }

  /**
   * A sensible definition of {@link #subMap(Object, Object)} in terms of {@link #subMap(Object,
   * boolean, Object, boolean)}. If you override {@code subMap(K, boolean, K, boolean)}, you may
   * wish to override {@code subMap} to forward to this implementation.
   */
  @Override
  protected @PolyMutable SortedMap<K, V> standardSubMap(
          @PolyMutable ForwardingNavigableMap<K, V> this,
      @ParametricNullness K fromKey, @ParametricNullness K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public @PolyMutable NavigableMap<K, V> subMap(
          @PolyMutable ForwardingNavigableMap<K, V> this,
      @ParametricNullness K fromKey,
      boolean fromInclusive,
      @ParametricNullness K toKey,
      boolean toInclusive) {
    return delegate().subMap(fromKey, fromInclusive, toKey, toInclusive);
  }

  @Override
  public @PolyMutable NavigableMap<K, V> headMap(@PolyMutable ForwardingNavigableMap<K, V> this, @ParametricNullness K toKey, boolean inclusive) {
    return delegate().headMap(toKey, inclusive);
  }

  @Override
  public @PolyMutable NavigableMap<K, V> tailMap(@PolyMutable ForwardingNavigableMap<K, V> this, @ParametricNullness K fromKey, boolean inclusive) {
    return delegate().tailMap(fromKey, inclusive);
  }

  /**
   * A sensible definition of {@link #headMap(Object)} in terms of {@link #headMap(Object,
   * boolean)}. If you override {@code headMap(K, boolean)}, you may wish to override {@code
   * headMap} to forward to this implementation.
   */
  protected @PolyMutable SortedMap<K, V> standardHeadMap(@PolyMutable ForwardingNavigableMap<K, V> this, @ParametricNullness K toKey) {
    return headMap(toKey, false);
  }

  /**
   * A sensible definition of {@link #tailMap(Object)} in terms of {@link #tailMap(Object,
   * boolean)}. If you override {@code tailMap(K, boolean)}, you may wish to override {@code
   * tailMap} to forward to this implementation.
   */
  protected @PolyMutable SortedMap<K, V> standardTailMap(@PolyMutable ForwardingNavigableMap<K, V> this, @ParametricNullness K fromKey) {
    return tailMap(fromKey, true);
  }
}
