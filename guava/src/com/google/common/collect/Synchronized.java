/*
 * Copyright (C) 2007 The Guava Authors
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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.j2objc.annotations.RetainedWith;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Synchronized collection views. The returned synchronized collection views are serializable if the
 * backing collection and the mutex are serializable.
 *
 * <p>If {@code null} is passed as the {@code mutex} parameter to any of this class's top-level
 * methods or inner class constructors, the created object uses itself as the synchronization mutex.
 *
 * <p>This class should be used by other collection classes only.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@AnnotatedFor({"nullness"})
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
/*
 * I have decided not to bother adding @ParametricNullness annotations in this class. Adding them is
 * a lot of busy work, and the annotation matters only when the APIs to be annotated are visible to
 * Kotlin code. In this class, nothing is publicly visible (nor exposed indirectly through a
 * publicly visible subclass), and I doubt any of our current or future Kotlin extensions for the
 * package will refer to the class. Plus, @ParametricNullness is only a temporary workaround,
 * anyway, so we just need to get by without the annotations here until Kotlin better understands
 * our other nullness annotations.
 */
final class Synchronized {
  private Synchronized() {}

  @ReceiverDependentMutable
  static class SynchronizedObject implements Serializable {
    final Object delegate;
    final @Readonly Object mutex;

    SynchronizedObject(@ReceiverDependentMutable Object delegate, @CheckForNull @Readonly Object mutex) {
      this.delegate = checkNotNull(delegate);
      this.mutex = (mutex == null) ? this : mutex;
    }

    @ReceiverDependentMutable Object delegate(@ReceiverDependentMutable SynchronizedObject this) {
      return delegate;
    }

    // No equals and hashCode; see ForwardingObject for details.

    @Pure
    @Override
    public String toString(@Readonly SynchronizedObject this) {
      synchronized (mutex) {
        return delegate.toString();
      }
    }

    // Serialization invokes writeObject only when it's private.
    // The SynchronizedObject subclasses don't need a writeObject method since
    // they don't contain any non-transient member variables, while the
    // following writeObject() handles the SynchronizedObject members.

    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      synchronized (mutex) {
        stream.defaultWriteObject();
      }
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <E extends @Nullable @Readonly Object> @PolyMutable Collection<E> collection(
          @PolyMutable Collection<E> collection, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedCollection<E>(collection, mutex);
  }

  @VisibleForTesting
  @ReceiverDependentMutable
  static class SynchronizedCollection<E extends @Nullable @Readonly Object> extends SynchronizedObject
      implements Collection<E> {
    private SynchronizedCollection(@ReceiverDependentMutable Collection<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    @ReceiverDependentMutable Collection<E> delegate(@ReceiverDependentMutable SynchronizedCollection<E> this) {
      return (@ReceiverDependentMutable Collection<E>) super.delegate();
    }

    @Override
    public boolean add(@Mutable SynchronizedCollection<E> this, E e) {
      synchronized (mutex) {
        return delegate().add(e);
      }
    }

    @Override
    public boolean addAll(@Mutable SynchronizedCollection<E> this, @Readonly Collection<? extends E> c) {
      synchronized (mutex) {
        return delegate().addAll(c);
      }
    }

    @Override
    public void clear(@Mutable SynchronizedCollection<E> this) {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Pure
    @Override
    public boolean contains(@Readonly SynchronizedCollection<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return delegate().contains(o);
      }
    }

    @Pure
    @Override
    public boolean containsAll(@Readonly SynchronizedCollection<E> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return delegate().containsAll(c);
      }
    }

    @Pure
    @Override
    public boolean isEmpty(@Readonly SynchronizedCollection<E> this) {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public @Mutable Iterator<E> iterator(@ReceiverDependentMutable SynchronizedCollection<E> this) {
      return delegate().iterator(); // manually synchronized
    }

    @Override
    public Spliterator<E> spliterator(@ReceiverDependentMutable SynchronizedCollection<E> this) {
      synchronized (mutex) {
        return delegate().spliterator();
      }
    }

    @Override
    public Stream<E> stream(@Readonly SynchronizedCollection<E> this) {
      synchronized (mutex) {
        return delegate().stream();
      }
    }

    @Override
    public Stream<E> parallelStream(@Readonly SynchronizedCollection<E> this) {
      synchronized (mutex) {
        return delegate().parallelStream();
      }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    public boolean remove(@Mutable SynchronizedCollection<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return delegate().remove(o);
      }
    }

    @Override
    public boolean removeAll(@Mutable SynchronizedCollection<E> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return delegate().removeAll(c);
      }
    }

    @Override
    public boolean retainAll(@Mutable SynchronizedCollection<E> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return delegate().retainAll(c);
      }
    }

    @Pure
    @Override
    public boolean removeIf(@Mutable SynchronizedCollection<E> this, @Readonly Predicate<? super E> filter) {
      synchronized (mutex) {
        return delegate().removeIf(filter);
      }
    }

    @Override
    public @NonNegative int size(@Readonly SynchronizedCollection<E> this) {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    @SuppressWarnings("nullness:return")
    public @PolyNull @PolySigned @Readonly Object[] toArray() {
      synchronized (mutex) {
        return delegate().toArray();
      }
    }

    @Override
    @SuppressWarnings("nullness:return")
    public <T extends @Nullable @UnknownSignedness @Readonly Object> T[] toArray(@PolyNull T[] a) {
      synchronized (mutex) {
        return delegate().toArray(a);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @VisibleForTesting
  static <E extends @Nullable @Readonly Object> @PolyMutable Set<E> set(@PolyMutable Set<E> set, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedSet<E>(set, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedSet<E extends @Nullable @Readonly Object> extends SynchronizedCollection<E>
      implements Set<E> {

    SynchronizedSet(@ReceiverDependentMutable Set<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable Set<E> delegate(@ReceiverDependentMutable SynchronizedSet<E> this) {
      return (@ReceiverDependentMutable Set<E>) super.delegate();
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedSet<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedSet<E> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <E extends @Nullable @Readonly Object> @PolyMutable SortedSet<E> sortedSet(
      @PolyMutable SortedSet<E> set, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedSortedSet<E>(set, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedSortedSet<E extends @Nullable @Readonly Object> extends SynchronizedSet<E>
      implements SortedSet<E> {
    SynchronizedSortedSet(@ReceiverDependentMutable SortedSet<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable SortedSet<E> delegate(@ReceiverDependentMutable SynchronizedSortedSet<E> this) {
      return (@ReceiverDependentMutable SortedSet<E>) super.delegate();
    }

    @SideEffectFree
    @Override
    @CheckForNull
    public @Readonly Comparator<? super E> comparator() {
      synchronized (mutex) {
        return delegate().comparator();
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable SortedSet<E> subSet(@ReceiverDependentMutable SynchronizedSortedSet<E> this, E fromElement, E toElement) {
      synchronized (mutex) {
        return sortedSet(delegate().subSet(fromElement, toElement), mutex);
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable SortedSet<E> headSet(@ReceiverDependentMutable SynchronizedSortedSet<E> this, E toElement) {
      synchronized (mutex) {
        return sortedSet(delegate().headSet(toElement), mutex);
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable SortedSet<E> tailSet(@ReceiverDependentMutable SynchronizedSortedSet<E> this, E fromElement) {
      synchronized (mutex) {
        return sortedSet(delegate().tailSet(fromElement), mutex);
      }
    }

    @SideEffectFree
    @Override
    public E first(@Readonly SynchronizedSortedSet<E> this) {
      synchronized (mutex) {
        return delegate().first();
      }
    }

    @SideEffectFree
    @Override
    public E last(@Readonly SynchronizedSortedSet<E> this) {
      synchronized (mutex) {
        return delegate().last();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <E extends @Nullable @Readonly Object> @PolyMutable List<E> list(
      @PolyMutable List<E> list, @CheckForNull @Readonly Object mutex) {
    return (list instanceof RandomAccess)
        ? new @PolyMutable SynchronizedRandomAccessList<E>(list, mutex)
        : new @PolyMutable SynchronizedList<E>(list, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedList<E extends @Nullable @Readonly Object> extends SynchronizedCollection<E>
      implements List<E> {
    SynchronizedList(@ReceiverDependentMutable List<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable List<E> delegate(@ReceiverDependentMutable SynchronizedList<E> this) {
      return (@ReceiverDependentMutable List<E>) super.delegate();
    }

    @Override
    public void add(@Mutable SynchronizedList<E> this, int index, E element) {
      synchronized (mutex) {
        delegate().add(index, element);
      }
    }

    @Override
    public boolean addAll(@Mutable SynchronizedList<E> this, int index, @Readonly Collection<? extends E> c) {
      synchronized (mutex) {
        return delegate().addAll(index, c);
      }
    }

    @Override
    public E get(@Readonly SynchronizedList<E> this, int index) {
      synchronized (mutex) {
        return delegate().get(index);
      }
    }

    @Pure
    @Override
    public int indexOf(@Readonly SynchronizedList<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return delegate().indexOf(o);
      }
    }

    @Pure
    @Override
    public int lastIndexOf(@Readonly SynchronizedList<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return delegate().lastIndexOf(o);
      }
    }

    @Override
    public @Mutable ListIterator<E> listIterator(@ReceiverDependentMutable SynchronizedList<E> this) {
      return delegate().listIterator(); // manually synchronized
    }

    @Override
    public @Mutable ListIterator<E> listIterator(@ReceiverDependentMutable SynchronizedList<E> this, int index) {
      return delegate().listIterator(index); // manually synchronized
    }

    @Override
    public E remove(@Mutable SynchronizedList<E> this, int index) {
      synchronized (mutex) {
        return delegate().remove(index);
      }
    }

    @Override
    public E set(@Mutable SynchronizedList<E> this, int index, E element) {
      synchronized (mutex) {
        return delegate().set(index, element);
      }
    }

    @SideEffectFree
    @Override
    public void replaceAll(@Mutable SynchronizedList<E> this, UnaryOperator<E> operator) {
      synchronized (mutex) {
        delegate().replaceAll(operator);
      }
    }

    @Override
    public void sort(@Mutable SynchronizedList<E> this, @Nullable Comparator<? super E> c) {
      synchronized (mutex) {
        delegate().sort(c);
      }
    }

    @Override
    public @ReceiverDependentMutable List<E> subList(@ReceiverDependentMutable SynchronizedList<E> this, int fromIndex, int toIndex) {
      synchronized (mutex) {
        return list(delegate().subList(fromIndex, toIndex), mutex);
      }
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedList<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedList<E> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @ReceiverDependentMutable
  static final class SynchronizedRandomAccessList<E extends @Nullable @Readonly Object>
      extends SynchronizedList<E> implements RandomAccess {
    SynchronizedRandomAccessList(@ReceiverDependentMutable List<E> list, @CheckForNull @Readonly Object mutex) {
      super(list, mutex);
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <E extends @Nullable @Readonly Object> @PolyMutable Multiset<E> multiset(
      @PolyMutable Multiset<E> multiset, @CheckForNull @Readonly Object mutex) {
    if (multiset instanceof SynchronizedMultiset || multiset instanceof ImmutableMultiset) {
      return multiset;
    }
    return new @PolyMutable SynchronizedMultiset<E>(multiset, mutex);
  }

  @ReceiverDependentMutable
  static final class SynchronizedMultiset<E extends @Nullable @Readonly Object>
      extends SynchronizedCollection<E> implements Multiset<E> {
    @CheckForNull transient @Assignable Set<E> elementSet;
    @CheckForNull transient @Assignable Set<Multiset.@Readonly Entry<E>> entrySet;

    SynchronizedMultiset(@ReceiverDependentMutable Multiset<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable Multiset<E> delegate(@ReceiverDependentMutable SynchronizedMultiset<E> this) {
      return (@ReceiverDependentMutable Multiset<E>) super.delegate();
    }

    @Override
    public @NonNegative int count(@Readonly SynchronizedMultiset<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return delegate().count(o);
      }
    }

    @Override
    public int add(@Mutable SynchronizedMultiset<E> this, @ParametricNullness E e, int n) {
      synchronized (mutex) {
        return delegate().add(e, n);
      }
    }

    @Override
    public int remove(@Mutable SynchronizedMultiset<E> this, @CheckForNull @Readonly Object o, int n) {
      synchronized (mutex) {
        return delegate().remove(o, n);
      }
    }

    @Override
    public int setCount(@Mutable SynchronizedMultiset<E> this, @ParametricNullness E element, int count) {
      synchronized (mutex) {
        return delegate().setCount(element, count);
      }
    }

    @Override
    public boolean setCount(@Mutable SynchronizedMultiset<E> this, @ParametricNullness E element, int oldCount, int newCount) {
      synchronized (mutex) {
        return delegate().setCount(element, oldCount, newCount);
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<E> elementSet(@ReceiverDependentMutable SynchronizedMultiset<E> this) {
      synchronized (mutex) {
        if (elementSet == null) {
          elementSet = typePreservingSet(delegate().elementSet(), mutex);
        }
        return elementSet;
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<Multiset.@Readonly Entry<E>> entrySet(@ReceiverDependentMutable SynchronizedMultiset<E> this) {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = typePreservingSet(delegate().entrySet(), mutex);
        }
        return entrySet;
      }
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedMultiset<E> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedMultiset<E> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable Multimap<K, V> multimap(
      @PolyMutable Multimap<K, V> multimap, @CheckForNull @Readonly Object mutex) {
    if (multimap instanceof SynchronizedMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new @PolyMutable SynchronizedMultimap<>(multimap, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedObject implements Multimap<K, V> {
    @CheckForNull transient @Assignable Set<K> keySet;
    @CheckForNull transient @Assignable Collection<V> valuesCollection;
    @CheckForNull transient @Assignable Collection<Map.@ReceiverDependentMutable Entry<K, V>> entries;
    @CheckForNull transient @Assignable Map<K, @ReceiverDependentMutable Collection<V>> asMap;
    @CheckForNull transient @Assignable Multiset<K> keys;

    @SuppressWarnings("unchecked")
    @Override
    @ReceiverDependentMutable Multimap<K, V> delegate(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      return (@ReceiverDependentMutable Multimap<K, V>) super.delegate();
    }

    SynchronizedMultimap(@ReceiverDependentMutable Multimap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Pure
    @Override
    public int size(@Readonly SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Pure
    @Override
    public boolean isEmpty(@Readonly SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Pure
    @Override
    public boolean containsKey(@Readonly SynchronizedMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      synchronized (mutex) {
        return delegate().containsKey(key);
      }
    }

    @Pure
    @Override
    public boolean containsValue(@Readonly SynchronizedMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @Pure
    @Override
    public boolean containsEntry(@Readonly SynchronizedMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      synchronized (mutex) {
        return delegate().containsEntry(key, value);
      }
    }

    @Override
    public @ReceiverDependentMutable Collection<V> get(@ParametricNullness K key) {
      synchronized (mutex) {
        return typePreservingCollection(delegate().get(key), mutex);
      }
    }

    @Override
    public boolean put(@Mutable SynchronizedMultimap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().put(key, value);
      }
    }

    @Override
    public boolean putAll(@Mutable SynchronizedMultimap<K, V> this, @ParametricNullness K key, @Readonly Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().putAll(key, values);
      }
    }

    @Override
    public boolean putAll(@Mutable SynchronizedMultimap<K, V> this, @Readonly Multimap<? extends K, ? extends V> multimap) {
      synchronized (mutex) {
        return delegate().putAll(multimap);
      }
    }

    @Override
    public @Readonly Collection<V> replaceValues(@Mutable SynchronizedMultimap<K, V> this, @ParametricNullness K key, @Readonly Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @Override
    public boolean remove(@Mutable SynchronizedMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      synchronized (mutex) {
        return delegate().remove(key, value);
      }
    }

    @Override
    public @Readonly Collection<V> removeAll(@Mutable SynchronizedMultimap<K, V> this, @CheckForNull @Readonly Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public void clear(@Mutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<K> keySet(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        if (keySet == null) {
          keySet = typePreservingSet(delegate().keySet(), mutex);
        }
        return keySet;
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Collection<V> values(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        if (valuesCollection == null) {
          valuesCollection = collection(delegate().values(), mutex);
        }
        return valuesCollection;
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Collection<Map.@ReceiverDependentMutable Entry<K, V>> entries(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        if (entries == null) {
          entries = typePreservingCollection(delegate().entries(), mutex);
        }
        return entries;
      }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    public @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> asMap(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        if (asMap == null) {
          asMap = new @ReceiverDependentMutable SynchronizedAsMap<>(delegate().asMap(), mutex);
        }
        return asMap;
      }
    }

    @Override
    public @ReceiverDependentMutable Multiset<K> keys(@ReceiverDependentMutable SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        if (keys == null) {
          keys = multiset(delegate().keys(), mutex);
        }
        return keys;
      }
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedMultimap<K, V> this, @CheckForNull @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedMultimap<K, V> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable ListMultimap<K, V> listMultimap(
      @PolyMutable ListMultimap<K, V> multimap, @CheckForNull @Readonly Object mutex) {
    if (multimap instanceof SynchronizedListMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new @PolyMutable SynchronizedListMultimap<>(multimap, mutex);
  }

  @ReceiverDependentMutable
  static final class SynchronizedListMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedMultimap<K, V> implements ListMultimap<K, V> {
    SynchronizedListMultimap(@ReceiverDependentMutable ListMultimap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable ListMultimap<K, V> delegate(@ReceiverDependentMutable SynchronizedListMultimap<K, V> this) {
      return (@ReceiverDependentMutable ListMultimap<K, V>) super.delegate();
    }

    @Override
    public @ReceiverDependentMutable List<V> get(@ReceiverDependentMutable SynchronizedListMultimap<K, V> this, K key) {
      synchronized (mutex) {
        return list(delegate().get(key), mutex);
      }
    }

    @Override
    public @Readonly List<V> removeAll(@Mutable SynchronizedListMultimap<K, V> this, @CheckForNull @Readonly Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public @Readonly List<V> replaceValues(@Mutable SynchronizedListMultimap<K, V> this,  K key, @Readonly Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object> @PolyMutable SetMultimap<K, V> setMultimap(
      @PolyMutable SetMultimap<K, V> multimap, @CheckForNull @Readonly Object mutex) {
    if (multimap instanceof SynchronizedSetMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new @PolyMutable SynchronizedSetMultimap<>(multimap, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedSetMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      extends SynchronizedMultimap<K, V> implements SetMultimap<K, V> {
    @CheckForNull transient @Assignable Set<Map.@ReceiverDependentMutable Entry<K, V>> entrySet;

    SynchronizedSetMultimap(@ReceiverDependentMutable SetMultimap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable SetMultimap<K, V> delegate(@ReceiverDependentMutable SynchronizedSetMultimap<K, V> this) {
      return (@ReceiverDependentMutable SetMultimap<K, V>) super.delegate();
    }

    @Override
    public @ReceiverDependentMutable Set<V> get(@ReceiverDependentMutable SynchronizedSetMultimap<K, V> this, K key) {
      synchronized (mutex) {
        return set(delegate().get(key), mutex);
      }
    }

    @Override
    public @Readonly Set<V> removeAll(@Mutable SynchronizedSetMultimap<K, V> this, @CheckForNull @Readonly Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public @Readonly Set<V> replaceValues(@Mutable SynchronizedSetMultimap<K, V> this, K key, @Readonly Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<Map.@ReceiverDependentMutable Entry<K, V>> entries(@ReceiverDependentMutable SynchronizedSetMultimap<K, V> this) {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = set(delegate().entries(), mutex);
        }
        return entrySet;
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      @PolyMutable SortedSetMultimap<K, V> sortedSetMultimap(
          @PolyMutable SortedSetMultimap<K, V> multimap, @CheckForNull @Readonly Object mutex) {
    if (multimap instanceof SynchronizedSortedSetMultimap) {
      return multimap;
    }
    return new @PolyMutable SynchronizedSortedSetMultimap<>(multimap, mutex);
  }

  @ReceiverDependentMutable
  static final class SynchronizedSortedSetMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      extends SynchronizedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    SynchronizedSortedSetMultimap(@ReceiverDependentMutable SortedSetMultimap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable SortedSetMultimap<K, V> delegate(@ReceiverDependentMutable SynchronizedSortedSetMultimap<K, V> this) {
      return (@ReceiverDependentMutable SortedSetMultimap<K, V>) super.delegate();
    }

    @Override
    public @ReceiverDependentMutable SortedSet<V> get(@ReceiverDependentMutable SynchronizedSortedSetMultimap<K, V> this, K key) {
      synchronized (mutex) {
        return sortedSet(delegate().get(key), mutex);
      }
    }

    @Override
    public @Readonly SortedSet<V> removeAll(@Mutable SynchronizedSortedSetMultimap<K, V> this, @CheckForNull @Readonly Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public @Readonly SortedSet<V> replaceValues(@Mutable SynchronizedSortedSetMultimap<K, V> this, K key, @Readonly Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @Override
    @CheckForNull
    public @Readonly Comparator<? super V> valueComparator(@Readonly SynchronizedSortedSetMultimap<K, V> this) {
      synchronized (mutex) {
        return delegate().valueComparator();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <E extends @Nullable @Readonly Object> @PolyMutable Collection<E> typePreservingCollection(
      @PolyMutable Collection<E> collection, @CheckForNull @Readonly Object mutex) {
    if (collection instanceof SortedSet) {
      return sortedSet((@PolyMutable SortedSet<E>) collection, mutex);
    }
    if (collection instanceof Set) {
      return set((@PolyMutable Set<E>) collection, mutex);
    }
    if (collection instanceof List) {
      return list((@PolyMutable List<E>) collection, mutex);
    }
    return collection(collection, mutex);
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <E extends @Nullable @Readonly Object> @PolyMutable Set<E> typePreservingSet(
      @PolyMutable Set<E> set, @CheckForNull @Readonly Object mutex) {
    if (set instanceof SortedSet) {
      return sortedSet((@PolyMutable SortedSet<E>) set, mutex);
    } else {
      return set(set, mutex);
    }
  }

  @ReceiverDependentMutable
  static final class SynchronizedAsMapEntries<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedSet<Map.Entry<K, Collection<V>>> {
    SynchronizedAsMapEntries(
            @ReceiverDependentMutable Set<Map.Entry<K, Collection<V>>> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    public @Mutable Iterator<Map.Entry<K, Collection<V>>> iterator(@ReceiverDependentMutable SynchronizedAsMapEntries<K, V> this) {
      // Must be manually synchronized.
      return new @Mutable TransformedIterator<Map.Entry<K, Collection<V>>, Map.Entry<K, Collection<V>>>(
          super.iterator()) {
        @Override
        Map.Entry<K, Collection<V>> transform(final Map.Entry<K, Collection<V>> entry) {
          return new @Mutable ForwardingMapEntry<K, Collection<V>>() {
            @Override
            protected Map.Entry<K, Collection<V>> delegate() {
              return entry;
            }

            @Override
            public Collection<V> getValue() {
              return typePreservingCollection(entry.getValue(), mutex);
            }
          };
        }
      };
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override
    public @PolyNull @PolySigned Object[] toArray() {
      synchronized (mutex) {
        /*
         * toArrayImpl returns `@Nullable Object[]` rather than `Object[]` but only because it can
         * be used with collections that may contain null. This collection never contains nulls, so
         * we could return `Object[]`. But this class is private and J2KT cannot change return types
         * in overrides, so we declare `@Nullable Object[]` as the return type.
         */
        return ObjectArrays.toArrayImpl(delegate());
      }
    }

    @Override
    public <T extends @Nullable @UnknownSignedness @Readonly Object> T[] toArray(@PolyNull T[] array) {
      synchronized (mutex) {
        return ObjectArrays.toArrayImpl(delegate(), array);
      }
    }

    @Pure
    @Override
    public boolean contains(@Readonly SynchronizedAsMapEntries<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return Maps.containsEntryImpl(delegate(), o);
      }
    }

    @Pure
    @Override
    public boolean containsAll(@Readonly SynchronizedAsMapEntries<K, V> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return Collections2.containsAllImpl(delegate(), c);
      }
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedAsMapEntries<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return Sets.equalsImpl(delegate(), o);
      }
    }

    @Override
    public boolean remove(@Mutable SynchronizedAsMapEntries<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      synchronized (mutex) {
        return Maps.removeEntryImpl(delegate(), o);
      }
    }

    @Override
    public boolean removeAll(@Mutable SynchronizedAsMapEntries<K, V> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return Iterators.removeAll(delegate().iterator(), c);
      }
    }

    @Override
    public boolean retainAll(@Mutable SynchronizedAsMapEntries<K, V> this, @Readonly Collection<?> c) {
      synchronized (mutex) {
        return Iterators.retainAll(delegate().iterator(), c);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @VisibleForTesting
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable Map<K, V> map(
      @PolyMutable Map<K, V> map, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedMap<>(map, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedObject implements Map<K, V> {
    @CheckForNull transient @Assignable Set<K> keySet;
    @CheckForNull transient @Assignable Collection<V> values;
    @CheckForNull transient @Assignable Set<Map.@ReceiverDependentMutable Entry<K, V>> entrySet;

    SynchronizedMap(@ReceiverDependentMutable Map<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    @ReceiverDependentMutable Map<K, V> delegate(@ReceiverDependentMutable SynchronizedMap<K, V> this) {
      return (@ReceiverDependentMutable Map<K, V>) super.delegate();
    }

    @Override
    public void clear(@Mutable SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Pure
    @Override
    public boolean containsKey(@Readonly SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      synchronized (mutex) {
        return delegate().containsKey(key);
      }
    }

    @Pure
    @Override
    public boolean containsValue(@Readonly SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<Map.@ReceiverDependentMutable Entry<@KeyFor({"this"}) K, V>> entrySet(@ReceiverDependentMutable SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = set(delegate().entrySet(), mutex);
        }
        return entrySet;
      }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    @CheckForNull
    public V get(@Readonly SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      synchronized (mutex) {
        return delegate().get(key);
      }
    }

    @Pure
    @Override
    @CheckForNull
    public V getOrDefault(@Readonly SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key, @CheckForNull V defaultValue) {
      synchronized (mutex) {
        return delegate().getOrDefault(key, defaultValue);
      }
    }

    @Override
    public boolean isEmpty(@Readonly SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<@KeyFor({"this"}) K> keySet(@ReceiverDependentMutable SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        if (keySet == null) {
          keySet = set(delegate().keySet(), mutex);
        }
        return keySet;
      }
    }

    @Override
    @CheckForNull
    public V put(@Mutable SynchronizedMap<K, V> this, K key, V value) {
      synchronized (mutex) {
        return delegate().put(key, value);
      }
    }

    @Override
    @CheckForNull
    public V putIfAbsent(@Mutable SynchronizedMap<K, V> this, K key, V value) {
      synchronized (mutex) {
        return delegate().putIfAbsent(key, value);
      }
    }

    @Override
    public boolean replace(@Mutable SynchronizedMap<K, V> this, K key, V oldValue, V newValue) {
      synchronized (mutex) {
        return delegate().replace(key, oldValue, newValue);
      }
    }

    @Override
    @CheckForNull
    public V replace(@Mutable SynchronizedMap<K, V> this, K key, V value) {
      synchronized (mutex) {
        return delegate().replace(key, value);
      }
    }

    @Override
    public @PolyNull V computeIfAbsent(@Mutable SynchronizedMap<K, V> this, K key, Function<? super K, ? extends @PolyNull V> mappingFunction) {
      synchronized (mutex) {
        return delegate().computeIfAbsent(key, mappingFunction);
      }
    }

    @Override
    @SuppressWarnings("nullness") // TODO(b/262880368): Remove once we see @NonNull in JDK APIs
    public @PolyNull V computeIfPresent(@Mutable SynchronizedMap<K, V> this, 
        K key, BiFunction<? super K, ? super @NonNull V, ? extends @PolyNull V> remappingFunction) {
      synchronized (mutex) {
        return delegate().computeIfPresent(key, remappingFunction);
      }
    }

    @Override
    public @PolyNull V compute(@Mutable SynchronizedMap<K, V> this, 
        K key,
        BiFunction<? super K, ? super @Nullable V, ? extends @PolyNull V> remappingFunction) {
      synchronized (mutex) {
        return delegate().compute(key, remappingFunction);
      }
    }

    @Override
    @SuppressWarnings("nullness") // TODO(b/262880368): Remove once we see @NonNull in JDK APIs
    public @PolyNull V merge(@Mutable SynchronizedMap<K, V> this, 
        K key,
        @NonNull V value,
        BiFunction<? super @NonNull V, ? super @NonNull V, ? extends @PolyNull V>
            remappingFunction) {
      synchronized (mutex) {
        return delegate().merge(key, value, remappingFunction);
      }
    }

    @Override
    public void putAll(@Mutable SynchronizedMap<K, V> this, @Readonly Map<? extends K, ? extends V> map) {
      synchronized (mutex) {
        delegate().putAll(map);
      }
    }

    @Override
    public void replaceAll(@Mutable SynchronizedMap<K, V> this, BiFunction<? super K, ? super V, ? extends V> function) {
      synchronized (mutex) {
        delegate().replaceAll(function);
      }
    }

    @Override
    @CheckForNull
    public V remove(@Mutable SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      synchronized (mutex) {
        return delegate().remove(key);
      }
    }

    @Pure
    @Override
    public boolean remove(@Mutable SynchronizedMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key, @CheckForNull @UnknownSignedness @Readonly Object value) {
      synchronized (mutex) {
        return delegate().remove(key, value);
      }
    }

    @Override
    public @NonNegative int size(@Readonly SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Collection<V> values(@ReceiverDependentMutable SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        if (values == null) {
          values = collection(delegate().values(), mutex);
        }
        return values;
      }
    }

    @Pure
    @Override
    public boolean equals(@Readonly SynchronizedMap<K, V> this, @CheckForNull @Readonly Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedMap<K, V> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable  @Readonly Object> @PolyMutable SortedMap<K, V> sortedMap(
      @PolyMutable SortedMap<K, V> sortedMap, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedSortedMap<>(sortedMap, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedSortedMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedMap<K, V> implements SortedMap<K, V> {

    SynchronizedSortedMap(@ReceiverDependentMutable SortedMap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable SortedMap<K, V> delegate(@ReceiverDependentMutable SynchronizedSortedMap<K, V> this) {
      return (@ReceiverDependentMutable SortedMap<K, V>) super.delegate();
    }

    @Override
    @CheckForNull
    public @Readonly Comparator<? super K> comparator() {
      synchronized (mutex) {
        return delegate().comparator();
      }
    }

    @Override
    public @KeyFor("this") K firstKey(@Readonly SynchronizedSortedMap<K, V> this) {
      synchronized (mutex) {
        return delegate().firstKey();
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> headMap(@ReceiverDependentMutable SynchronizedSortedMap<K, V> this, K toKey) {
      synchronized (mutex) {
        return sortedMap(delegate().headMap(toKey), mutex);
      }
    }

    @Override
    public @KeyFor("this") K lastKey(@Readonly SynchronizedSortedMap<K, V> this) {
      synchronized (mutex) {
        return delegate().lastKey();
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> subMap(@ReceiverDependentMutable SynchronizedSortedMap<K, V> this, K fromKey, K toKey) {
      synchronized (mutex) {
        return sortedMap(delegate().subMap(fromKey, toKey), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> tailMap(@ReceiverDependentMutable SynchronizedSortedMap<K, V> this, K fromKey) {
      synchronized (mutex) {
        return sortedMap(delegate().tailMap(fromKey), mutex);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object> @PolyMutable BiMap<K, V> biMap(
      @PolyMutable BiMap<K, V> bimap, @CheckForNull @Readonly Object mutex) {
    if (bimap instanceof SynchronizedBiMap || bimap instanceof ImmutableBiMap) {
      return bimap;
    }
    return new @PolyMutable SynchronizedBiMap<>(bimap, mutex, null);
  }

  @ReceiverDependentMutable
  static final class SynchronizedBiMap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      extends SynchronizedMap<K, V> implements BiMap<K, V>, Serializable {
    @CheckForNull private transient @Assignable Set<V> valueSet;
    @RetainedWith @CheckForNull private transient @Assignable BiMap<V, K> inverse;

    private SynchronizedBiMap(
            @ReceiverDependentMutable BiMap<K, V> delegate, @CheckForNull @Readonly Object mutex, @CheckForNull @ReceiverDependentMutable BiMap<V, K> inverse) {
      super(delegate, mutex);
      this.inverse = inverse;
    }

    @Override
    @ReceiverDependentMutable BiMap<K, V> delegate(@ReceiverDependentMutable SynchronizedBiMap<K, V> this) {
      return (@ReceiverDependentMutable BiMap<K, V>) super.delegate();
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<V> values(@ReceiverDependentMutable SynchronizedBiMap<K, V> this) {
      synchronized (mutex) {
        if (valueSet == null) {
          valueSet = set(delegate().values(), mutex);
        }
        return valueSet;
      }
    }

    @Override
    @CheckForNull
    public V forcePut(@Mutable SynchronizedBiMap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().forcePut(key, value);
      }
    }

    @Override
    public @ReceiverDependentMutable BiMap<V, K> inverse(@ReceiverDependentMutable SynchronizedBiMap<K, V> this) {
      synchronized (mutex) {
        if (inverse == null) {
          inverse = new @ReceiverDependentMutable SynchronizedBiMap<>(delegate().inverse(), mutex, this);
        }
        return inverse;
      }
    }

    private static final long serialVersionUID = 0;
  }

  @ReceiverDependentMutable
  static final class SynchronizedAsMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedMap<K, Collection<V>> {
    @CheckForNull transient @Assignable Set<Map.Entry<K, Collection<V>>> asMapEntrySet;
    @CheckForNull transient @Assignable Collection<Collection<V>> asMapValues;

    SynchronizedAsMap(@ReceiverDependentMutable Map<K, Collection<V>> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @CheckForNull
    public Collection<V> get(@Readonly SynchronizedAsMap<K, V> this,  @CheckForNull @UnknownSignedness @Readonly Object key) {
      synchronized (mutex) {
        Collection<V> collection = super.get(key);
        return (collection == null) ? null : typePreservingCollection(collection, mutex);
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Set<Map.Entry<@KeyFor({"this"}) K, Collection<V>>> entrySet(@ReceiverDependentMutable SynchronizedAsMap<K, V> this) {
      synchronized (mutex) {
        if (asMapEntrySet == null) {
          asMapEntrySet = new @ReceiverDependentMutable SynchronizedAsMapEntries<>(delegate().entrySet(), mutex);
        }
        return asMapEntrySet;
      }
    }

    @SideEffectFree
    @Override
    public @ReceiverDependentMutable Collection<Collection<V>> values(@ReceiverDependentMutable SynchronizedAsMap<K, V> this) {
      synchronized (mutex) {
        if (asMapValues == null) {
          asMapValues = new @ReceiverDependentMutable SynchronizedAsMapValues<V>(delegate().values(), mutex);
        }
        return asMapValues;
      }
    }

    @Pure
    @Override
    public boolean containsValue(@Readonly SynchronizedAsMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      // values() and its contains() method are both synchronized.
      return values().contains(o);
    }

    private static final long serialVersionUID = 0;
  }

  @ReceiverDependentMutable
  static final class SynchronizedAsMapValues<V extends @Nullable @Readonly Object>
      extends SynchronizedCollection<Collection<V>> {
    SynchronizedAsMapValues(@ReceiverDependentMutable Collection<Collection<V>> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    public @Mutable Iterator<Collection<V>> iterator(@ReceiverDependentMutable SynchronizedAsMapValues<V> this) {
      // Must be manually synchronized.
      return new @Mutable TransformedIterator<Collection<V>, Collection<V>>(super.iterator()) {
        @Override
        Collection<V> transform(Collection<V> from) {
          return typePreservingCollection(from, mutex);
        }
      };
    }

    private static final long serialVersionUID = 0;

  // See Collections.CheckedMap.CheckedEntrySet for details on attacks.
  //Suppressed due to annotations on toArray
  @SuppressWarnings("nullness")
  @Override
  public @PolyNull @PolySigned @PolyMutable Object[] toArray(@Readonly SynchronizedAsMapValues<@PolyNull @PolySigned @PolyMutable V> this) { return super.toArray(); }

  @SuppressWarnings("nullness")
  @Override public <T> T[] toArray(T[] arg0) { return super.toArray(arg0); }

  @Pure
  @Override
  public boolean contains(@Readonly SynchronizedAsMapValues<V> this, @Nullable @UnknownSignedness @Readonly Object arg0) { return super.contains(arg0); }

  @SuppressWarnings("nullness")
  @Pure
  @Override
  public boolean containsAll(@Readonly SynchronizedAsMapValues<V> this, @Readonly Collection<?> arg0) { return super.containsAll(arg0); }

  @Override
  public boolean remove(@Mutable SynchronizedAsMapValues<V> this, @Nullable @UnknownSignedness @Readonly Object arg0) { return super.remove(arg0); }

  @SuppressWarnings("nullness")
  @Override
  public boolean removeAll(@Mutable SynchronizedAsMapValues<V> this, @Readonly Collection<?> arg0) { return super.removeAll(arg0); }

  @SuppressWarnings("nullness")
  @Override
  public boolean retainAll(@Mutable SynchronizedAsMapValues<V> this, @Readonly Collection<?> arg0) { return super.retainAll(arg0); }
  }

  @GwtIncompatible // NavigableSet
  @VisibleForTesting
  @ReceiverDependentMutable
  static final class SynchronizedNavigableSet<E extends @Nullable @Readonly Object>
      extends SynchronizedSortedSet<E> implements NavigableSet<E> {
    SynchronizedNavigableSet(@ReceiverDependentMutable NavigableSet<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable NavigableSet<E> delegate(@ReceiverDependentMutable SynchronizedNavigableSet<E> this) {
      return (@ReceiverDependentMutable NavigableSet<E>) super.delegate();
    }

    @Override
    @CheckForNull
    public E ceiling(@Readonly SynchronizedNavigableSet<E> this, E e) {
      synchronized (mutex) {
        return delegate().ceiling(e);
      }
    }

    @Override
    public @Mutable Iterator<E> descendingIterator(@ReceiverDependentMutable SynchronizedNavigableSet<E> this) {
      return delegate().descendingIterator(); // manually synchronized
    }

    @CheckForNull transient @Assignable NavigableSet<E> descendingSet;

    @Override
    public @ReceiverDependentMutable NavigableSet<E> descendingSet(@ReceiverDependentMutable SynchronizedNavigableSet<E> this) {
      synchronized (mutex) {
        if (descendingSet == null) {
          NavigableSet<E> dS = Synchronized.navigableSet(delegate().descendingSet(), mutex);
          descendingSet = dS;
          return dS;
        }
        return descendingSet;
      }
    }

    @Override
    @CheckForNull
    public E floor(E e) {
      synchronized (mutex) {
        return delegate().floor(e);
      }
    }

    @Override
    public @ReceiverDependentMutable NavigableSet<E> headSet(E toElement, boolean inclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(delegate().headSet(toElement, inclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedSet<E> headSet(E toElement) {
      return headSet(toElement, false);
    }

    @Override
    @CheckForNull
    public E higher(E e) {
      synchronized (mutex) {
        return delegate().higher(e);
      }
    }

    @Override
    @CheckForNull
    public E lower(E e) {
      synchronized (mutex) {
        return delegate().lower(e);
      }
    }

    @Override
    @CheckForNull
    public E pollFirst(@Mutable SynchronizedNavigableSet<E> this) {
      synchronized (mutex) {
        return delegate().pollFirst();
      }
    }

    @Override
    @CheckForNull
    public E pollLast(@Mutable SynchronizedNavigableSet<E> this) {
      synchronized (mutex) {
        return delegate().pollLast();
      }
    }

    @Override
    public @ReceiverDependentMutable NavigableSet<E> subSet(
        E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(
            delegate().subSet(fromElement, fromInclusive, toElement, toInclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public @ReceiverDependentMutable NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(delegate().tailSet(fromElement, inclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @GwtIncompatible // NavigableSet
  static <E extends @Nullable @Readonly Object> @PolyMutable NavigableSet<E> navigableSet(
      @PolyMutable NavigableSet<E> navigableSet, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedNavigableSet<E>(navigableSet, mutex);
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @GwtIncompatible // NavigableSet
  static <E extends @Nullable @Readonly Object> @PolyMutable NavigableSet<E> navigableSet(@PolyMutable NavigableSet<E> navigableSet) {
    return navigableSet(navigableSet, null);
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @GwtIncompatible // NavigableMap
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable NavigableMap<K, V> navigableMap(
      @PolyMutable NavigableMap<K, V> navigableMap) {
    return navigableMap(navigableMap, null);
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  @GwtIncompatible // NavigableMap
  static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable NavigableMap<K, V> navigableMap(
      @PolyMutable NavigableMap<K, V> navigableMap, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedNavigableMap<>(navigableMap, mutex);
  }

  @GwtIncompatible // NavigableMap
  @VisibleForTesting
  @ReceiverDependentMutable
  static final class SynchronizedNavigableMap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {

    SynchronizedNavigableMap(@ReceiverDependentMutable NavigableMap<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable NavigableMap<K, V> delegate(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this) {
      return (@ReceiverDependentMutable NavigableMap<K, V>) super.delegate();
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> ceilingEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().ceilingEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K ceilingKey(K key) {
      synchronized (mutex) {
        return delegate().ceilingKey(key);
      }
    }

    @CheckForNull transient @Assignable NavigableSet<K> descendingKeySet;

    @Override
    public @ReceiverDependentMutable NavigableSet<@KeyFor({"this"}) K> descendingKeySet(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this) {
      synchronized (mutex) {
        if (descendingKeySet == null) {
          return descendingKeySet = Synchronized.navigableSet(delegate().descendingKeySet(), mutex);
        }
        return descendingKeySet;
      }
    }

    @CheckForNull transient @Assignable NavigableMap<K, V> descendingMap;

    @Override
    public @ReceiverDependentMutable NavigableMap<K, V> descendingMap() {
      synchronized (mutex) {
        if (descendingMap == null) {
          return descendingMap = navigableMap(delegate().descendingMap(), mutex);
        }
        return descendingMap;
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> firstEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().firstEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> floorEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().floorEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K floorKey(K key) {
      synchronized (mutex) {
        return delegate().floorKey(key);
      }
    }

    @Override
    public @ReceiverDependentMutable NavigableMap<K, V> headMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this, K toKey, boolean inclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().headMap(toKey, inclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> headMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this, K toKey) {
      return headMap(toKey, false);
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> higherEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().higherEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K higherKey(K key) {
      synchronized (mutex) {
        return delegate().higherKey(key);
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> lastEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lastEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> lowerEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lowerEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K lowerKey(K key) {
      synchronized (mutex) {
        return delegate().lowerKey(key);
      }
    }

    @Override
    public @ReceiverDependentMutable Set<@KeyFor({"this"}) K> keySet(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this) {
      return navigableKeySet();
    }

    @CheckForNull transient @Assignable NavigableSet<K> navigableKeySet;

    @Override
    public @ReceiverDependentMutable NavigableSet<@KeyFor({"this"}) K> navigableKeySet(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this) {
      synchronized (mutex) {
        if (navigableKeySet == null) {
          return navigableKeySet = Synchronized.navigableSet(delegate().navigableKeySet(), mutex);
        }
        return navigableKeySet;
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> pollFirstEntry(@Mutable SynchronizedNavigableMap<K, V> this) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollFirstEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.@Readonly Entry<K, V> pollLastEntry(@Mutable SynchronizedNavigableMap<K, V> this) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollLastEntry(), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable NavigableMap<K, V> subMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this,
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().subMap(fromKey, fromInclusive, toKey, toInclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> subMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this, K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public @ReceiverDependentMutable NavigableMap<K, V> tailMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this, K fromKey, boolean inclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().tailMap(fromKey, inclusive), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable SortedMap<K, V> tailMap(@ReceiverDependentMutable SynchronizedNavigableMap<K, V> this, K fromKey) {
      return tailMap(fromKey, true);
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // works but is needed only for NavigableMap
  @CheckForNull
  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the entry and its synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  private static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      Map.@PolyMutable Entry<K, V> nullableSynchronizedEntry(
          @CheckForNull Map.@PolyMutable Entry<K, V> entry, @CheckForNull @Readonly Object mutex) {
    if (entry == null) {
      return null;
    }
    return new @PolyMutable SynchronizedEntry<>(entry, mutex);
  }

  @GwtIncompatible // works but is needed only for NavigableMap
  @ReceiverDependentMutable
  static final class SynchronizedEntry<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedObject implements Map.Entry<K, V> {

    SynchronizedEntry(Map.@ReceiverDependentMutable Entry<K, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked") // guaranteed by the constructor
    @Override
    Map.@ReceiverDependentMutable Entry<K, V> delegate(@ReceiverDependentMutable SynchronizedEntry<K, V> this) {
      return (Map.@ReceiverDependentMutable Entry<K, V>) super.delegate();
    }

    @Override
    public boolean equals(@CheckForNull @Readonly Object obj) {
      synchronized (mutex) {
        return delegate().equals(obj);
      }
    }

    @Override
    public int hashCode(@UnknownSignedness SynchronizedEntry<K, V> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    @Override
    public K getKey() {
      synchronized (mutex) {
        return delegate().getKey();
      }
    }

    @Override
    public V getValue() {
      synchronized (mutex) {
        return delegate().getValue();
      }
    }

    @Override
    public V setValue(@Mutable SynchronizedEntry<K, V> this, V value) {
      synchronized (mutex) {
        return delegate().setValue(value);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <E extends @Nullable @Readonly Object> @PolyMutable Queue<E> queue(@PolyMutable Queue<E> queue, @CheckForNull @Readonly Object mutex) {
    return (queue instanceof SynchronizedQueue) ? queue : new @PolyMutable SynchronizedQueue<E>(queue, mutex);
  }

  @ReceiverDependentMutable
  static class SynchronizedQueue<E extends @Nullable @Readonly Object> extends SynchronizedCollection<E>
      implements Queue<E> {

    SynchronizedQueue(@ReceiverDependentMutable Queue<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable Queue<E> delegate() {
      return (@ReceiverDependentMutable Queue<E>) super.delegate();
    }

    @Override
    public E element() {
      synchronized (mutex) {
        return delegate().element();
      }
    }

    @Override
    public boolean offer(@Mutable SynchronizedQueue<E> this, E e) {
      synchronized (mutex) {
        return delegate().offer(e);
      }
    }

    @Override
    @CheckForNull
    public E peek() {
      synchronized (mutex) {
        return delegate().peek();
      }
    }

    @Override
    @CheckForNull
    public E poll(@Mutable SynchronizedQueue<E> this) {
      synchronized (mutex) {
        return delegate().poll();
      }
    }

    @Override
    public E remove(@Mutable SynchronizedQueue<E> this) {
      synchronized (mutex) {
        return delegate().remove();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <E extends @Nullable @Readonly Object> @PolyMutable Deque<E> deque(@PolyMutable Deque<E> deque, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedDeque<E>(deque, mutex);
  }

  @ReceiverDependentMutable
  static final class SynchronizedDeque<E extends @Nullable @Readonly Object> extends SynchronizedQueue<E>
      implements Deque<E> {

    SynchronizedDeque(@ReceiverDependentMutable Deque<E> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @ReceiverDependentMutable Deque<E> delegate() {
      return (@ReceiverDependentMutable Deque<E>) super.delegate();
    }

    @Override
    public void addFirst(@Mutable SynchronizedDeque<E> this, E e) {
      synchronized (mutex) {
        delegate().addFirst(e);
      }
    }

    @Override
    public void addLast(@Mutable SynchronizedDeque<E> this, E e) {
      synchronized (mutex) {
        delegate().addLast(e);
      }
    }

    @Override
    public boolean offerFirst(@Mutable SynchronizedDeque<E> this, E e) {
      synchronized (mutex) {
        return delegate().offerFirst(e);
      }
    }

    @Override
    public boolean offerLast(@Mutable SynchronizedDeque<E> this, E e) {
      synchronized (mutex) {
        return delegate().offerLast(e);
      }
    }

    @Override
    public E removeFirst(@Mutable SynchronizedDeque<E> this) {
      synchronized (mutex) {
        return delegate().removeFirst();
      }
    }

    @Override
    public E removeLast(@Mutable SynchronizedDeque<E> this) {
      synchronized (mutex) {
        return delegate().removeLast();
      }
    }

    @Override
    @CheckForNull
    public E pollFirst(@Mutable SynchronizedDeque<E> this) {
      synchronized (mutex) {
        return delegate().pollFirst();
      }
    }

    @Override
    @CheckForNull
    public E pollLast(@Mutable SynchronizedDeque<E> this) {
      synchronized (mutex) {
        return delegate().pollLast();
      }
    }

    @Override
    public E getFirst() {
      synchronized (mutex) {
        return delegate().getFirst();
      }
    }

    @Override
    public E getLast() {
      synchronized (mutex) {
        return delegate().getLast();
      }
    }

    @Override
    @CheckForNull
    public E peekFirst() {
      synchronized (mutex) {
        return delegate().peekFirst();
      }
    }

    @Override
    @CheckForNull
    public E peekLast() {
      synchronized (mutex) {
        return delegate().peekLast();
      }
    }

    @Override
    public boolean removeFirstOccurrence(@Mutable SynchronizedDeque<E> this, @CheckForNull @Readonly Object o) {
      synchronized (mutex) {
        return delegate().removeFirstOccurrence(o);
      }
    }

    @Override
    public boolean removeLastOccurrence(@Mutable SynchronizedDeque<E> this, @CheckForNull @Readonly Object o) {
      synchronized (mutex) {
        return delegate().removeLastOccurrence(o);
      }
    }

    @Override
    public void push(@Mutable SynchronizedDeque<E> this, E e) {
      synchronized (mutex) {
        delegate().push(e);
      }
    }

    @Override
    public E pop(@Mutable SynchronizedDeque<E> this) {
      synchronized (mutex) {
        return delegate().pop();
      }
    }

    @Override
    public @Mutable Iterator<E> descendingIterator() {
      synchronized (mutex) {
        return delegate().descendingIterator();
      }
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings({"mutability:argument.type.incompatible", "mutability:mutability.lost.parameter"}) // one poly: the delegate and the synchronized view are the same instantiation, as in Collections.unmodifiableCollection
  static <R extends @Nullable @Immutable Object, C extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      @PolyMutable Table<R, C, V> table(@PolyMutable Table<R, C, V> table, @CheckForNull @Readonly Object mutex) {
    return new @PolyMutable SynchronizedTable<>(table, mutex);
  }

  @ReceiverDependentMutable
  static final class SynchronizedTable<
          R extends @Nullable @Immutable Object, C extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends SynchronizedObject implements Table<R, C, V> {

    SynchronizedTable(@ReceiverDependentMutable Table<R, C, V> delegate, @CheckForNull @Readonly Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    @ReceiverDependentMutable Table<R, C, V> delegate(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      return (@ReceiverDependentMutable Table<R, C, V>) super.delegate();
    }

    @Override
    public boolean contains(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
      synchronized (mutex) {
        return delegate().contains(rowKey, columnKey);
      }
    }

    @Override
    public boolean containsRow(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object rowKey) {
      synchronized (mutex) {
        return delegate().containsRow(rowKey);
      }
    }

    @Override
    public boolean containsColumn(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object columnKey) {
      synchronized (mutex) {
        return delegate().containsColumn(columnKey);
      }
    }

    @Override
    public boolean containsValue(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @Override
    @CheckForNull
    public V get(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
      synchronized (mutex) {
        return delegate().get(rowKey, columnKey);
      }
    }

    @Override
    public boolean isEmpty(@Readonly SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public int size(@Readonly SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    public void clear(@Mutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Override
    @CheckForNull
    public V put(@Mutable SynchronizedTable<R, C, V> this, 
        @ParametricNullness R rowKey,
        @ParametricNullness C columnKey,
        @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().put(rowKey, columnKey, value);
      }
    }

    @Override
    public void putAll(@Mutable SynchronizedTable<R, C, V> this, @Readonly Table<? extends R, ? extends C, ? extends V> table) {
      synchronized (mutex) {
        delegate().putAll(table);
      }
    }

    @Override
    @CheckForNull
    public V remove(@Mutable SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
      synchronized (mutex) {
        return delegate().remove(rowKey, columnKey);
      }
    }

    @Override
    public @ReceiverDependentMutable Map<C, V> row(@ParametricNullness R rowKey) {
      synchronized (mutex) {
        return map(delegate().row(rowKey), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Map<R, V> column(@ParametricNullness C columnKey) {
      synchronized (mutex) {
        return map(delegate().column(columnKey), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Set<Cell<R, C, V>> cellSet(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return set(delegate().cellSet(), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Set<R> rowKeySet(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return set(delegate().rowKeySet(), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Set<C> columnKeySet(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return set(delegate().columnKeySet(), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Collection<V> values(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return collection(delegate().values(), mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Map<R, @ReceiverDependentMutable Map<C, V>> rowMap(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return map(
            Maps.transformValues(
                delegate().rowMap(),
                new com.google.common.base.Function<Map<C, V>, Map<C, V>>() {
                  @Override
                  public Map<C, V> apply(Map<C, V> t) {
                    return map(t, mutex);
                  }
                }),
            mutex);
      }
    }

    @Override
    public @ReceiverDependentMutable Map<C, @ReceiverDependentMutable Map<R, V>> columnMap(@ReceiverDependentMutable SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return map(
            Maps.transformValues(
                delegate().columnMap(),
                new com.google.common.base.Function<Map<R, V>, Map<R, V>>() {
                  @Override
                  public Map<R, V> apply(Map<R, V> t) {
                    return map(t, mutex);
                  }
                }),
            mutex);
      }
    }

    @Override
    public int hashCode(@UnknownSignedness @Readonly SynchronizedTable<R, C, V> this) {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    @Override
    public boolean equals(@Readonly SynchronizedTable<R, C, V> this, @CheckForNull @Readonly Object obj) {
      if (this == obj) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(obj);
      }
    }
  }
}
