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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Maps.ViewCachingAbstractMap;
import com.google.j2objc.annotations.WeakOuter;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.CFComment;

/**
 * Basic implementation of the {@link Multimap} interface. This class represents a multimap as a map
 * that associates each key with a collection of values. All methods of {@link Multimap} are
 * supported, including those specified as optional in the interface.
 *
 * <p>To implement a multimap, a subclass must define the method {@link #createCollection()}, which
 * creates an empty collection of values for a key.
 *
 * <p>The multimap constructor takes a map that has a single entry for each distinct key. When you
 * insert a key-value pair with a key that isn't already in the multimap, {@code
 * AbstractMapBasedMultimap} calls {@link #createCollection()} to create the collection of values
 * for that key. The subclass should not call {@link #createCollection()} directly, and a new
 * instance should be created every time the method is called.
 *
 * <p>For example, the subclass could pass a {@link java.util.TreeMap} during construction, and
 * {@link #createCollection()} could return a {@link java.util.TreeSet}, in which case the
 * multimap's iterators would propagate through the keys and values in sorted order.
 *
 * <p>Keys and values may be null, as long as the underlying collection classes support null
 * elements.
 *
 * <p>The collections created by {@link #createCollection()} may or may not allow duplicates. If the
 * collection, such as a {@link Set}, does not support duplicates, an added key-value pair will
 * replace an existing pair with the same key and value, if such a pair is present. With collections
 * like {@link List} that allow duplicates, the collection will keep the existing key-value pairs
 * while adding a new pair.
 *
 * <p>This class is not threadsafe when any concurrent operations update the multimap, even if the
 * underlying map and {@link #createCollection()} method return threadsafe classes. Concurrent read
 * operations will work correctly. To allow concurrent update operations, wrap your multimap with a
 * call to {@link Multimaps#synchronizedMultimap}.
 *
 * <p>For serialization to work, the subclass must specify explicit {@code readObject} and {@code
 * writeObject} methods.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractMapBasedMultimap<K extends @Nullable @Immutable Object, V extends @Readonly @Nullable Object>
    extends AbstractMultimap<K, V> implements Serializable {
  /*
   * Here's an outline of the overall design.
   *
   * The map variable contains the collection of values associated with each
   * key. When a key-value pair is added to a multimap that didn't previously
   * contain any values for that key, a new collection generated by
   * createCollection is added to the map. That same collection instance
   * remains in the map as long as the multimap has any values for the key. If
   * all values for the key are removed, the key and collection are removed
   * from the map.
   *
   * The get method returns a WrappedCollection, which decorates the collection
   * in the map (if the key is present) or an empty collection (if the key is
   * not present). When the collection delegate in the WrappedCollection is
   * empty, the multimap may contain subsequently added values for that key. To
   * handle that situation, the WrappedCollection checks whether map contains
   * an entry for the provided key, and if so replaces the delegate.
   */

  private transient Map<K, @ReceiverDependentMutable Collection<V>> map;
  private transient int totalSize;

  /**
   * Creates a new multimap that uses the provided map.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  protected AbstractMapBasedMultimap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map) {
    checkArgument(map.isEmpty());
    this.map = map;
  }

  /** Used during deserialization only. */
  final void setMap(@Mutable AbstractMapBasedMultimap<K, V> this, Map<K, Collection<V>> map) {
    this.map = map;
    totalSize = 0;
    for (Collection<V> values : map.values()) {
      checkArgument(!values.isEmpty());
      totalSize += values.size();
    }
  }

  /**
   * Creates an unmodifiable, empty collection of values.
   *
   * <p>This is used in {@link #removeAll} on an empty key.
   */
  @Readonly Collection<V> createUnmodifiableEmptyCollection(@Readonly AbstractMapBasedMultimap<K, V> this) {
    return unmodifiableCollectionSubclass(createCollection());
  }

  /**
   * Creates the collection of values for a single key.
   *
   * <p>Collections with weak, soft, or phantom references are not supported. Each call to {@code
   * createCollection} should create a new instance.
   *
   * <p>The returned collection class determines whether duplicate key-value pairs are allowed.
   *
   * @return an empty collection of values
   */
  abstract @PolyMutable Collection<V> createCollection(@PolyMutable AbstractMapBasedMultimap<K, V> this);

  /**
   * Creates the collection of values for an explicitly provided key. By default, it simply calls
   * {@link #createCollection()}, which is the correct behavior for most implementations. The {@link
   * LinkedHashMultimap} class overrides it.
   *
   * @param key key to associate with values in the collection
   * @return an empty collection of values
   */
  @PolyMutable Collection<V> createCollection(@PolyMutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key) {
    return createCollection();
  }

  @PolyMutable Map<K, @PolyMutable Collection<V>> backingMap(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return map;
  }

  // Query Operations

  @Override
  public int size(@Readonly AbstractMapBasedMultimap<K, V> this) {
    return totalSize;
  }

  @Override
  public boolean containsKey(@Readonly AbstractMapBasedMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
    return map.containsKey(key);
  }

  // Modification Operations

  @Override
  public boolean put(@Mutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
    Collection<V> collection = map.get(key);
    if (collection == null) {
      collection = createCollection(key);
      if (collection.add(value)) {
        totalSize++;
        map.put(key, collection);
        return true;
      } else {
        throw new AssertionError("New Collection violated the Collection spec");
      }
    } else if (collection.add(value)) {
      totalSize++;
      return true;
    } else {
      return false;
    }
  }

  private Collection<V> getOrCreateCollection(@Mutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key) {
    Collection<V> collection = map.get(key);
    if (collection == null) {
      collection = createCollection(key);
      map.put(key, collection);
    }
    return collection;
  }

  // Bulk Operations

  /**
   * {@inheritDoc}
   *
   * <p>The returned collection is immutable.
   */
  @Override
  public @Readonly Collection<V> replaceValues(@Mutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    Iterator<? extends V> iterator = values.iterator();
    if (!iterator.hasNext()) {
      return removeAll(key);
    }

    // TODO(lowasser): investigate atomic failure?
    Collection<V> collection = getOrCreateCollection(key);
    Collection<V> oldValues = createCollection();
    oldValues.addAll(collection);

    totalSize -= collection.size();
    collection.clear();

    while (iterator.hasNext()) {
      if (collection.add(iterator.next())) {
        totalSize++;
      }
    }

    return unmodifiableCollectionSubclass(oldValues);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned collection is immutable.
   */
  @Override
  public @Readonly Collection<V> removeAll(@Mutable AbstractMapBasedMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    Collection<V> collection = map.remove(key);

    if (collection == null) {
      return createUnmodifiableEmptyCollection();
    }

    Collection<V> output = createCollection();
    output.addAll(collection);
    totalSize -= collection.size();
    collection.clear();

    return unmodifiableCollectionSubclass(output);
  }

  <E extends @Nullable @Readonly Object> @Readonly Collection<E> unmodifiableCollectionSubclass(
        @Readonly AbstractMapBasedMultimap<K, V> this,
        @Readonly Collection<E> collection) {
    return Collections.unmodifiableCollection(collection);
  }

  @Override
  public void clear(@Mutable AbstractMapBasedMultimap<K, V> this) {
    // Clear each collection, to make previously returned collections empty.
    for (Collection<V> collection : map.values()) {
      collection.clear();
    }
    map.clear();
    totalSize = 0;
  }

  // Views

  /**
   * {@inheritDoc}
   *
   * <p>The returned collection is not serializable.
   */
  @Override
  public @PolyMutable Collection<V> get(@PolyMutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key) {
    Collection<V> collection = map.get(key);
    if (collection == null) {
      collection = createCollection(key);
    }
    return wrapCollection(key, collection);
  }

  /**
   * Generates a decorated collection that remains consistent with the values in the multimap for
   * the provided key. Changes to the multimap may alter the returned collection, and vice versa.
   */
  @PolyMutable Collection<V> wrapCollection(@PolyMutable AbstractMapBasedMultimap<K, V> this, @ParametricNullness K key, @PolyMutable Collection<V> collection) {
    return new @PolyMutable WrappedCollection(key, collection, null);
  }

  final @PolyMutable List<V> wrapList(
      @ParametricNullness K key, @PolyMutable List<V> list, @CheckForNull WrappedCollection ancestor) {
    return (list instanceof RandomAccess)
        ? new @PolyMutable RandomAccessWrappedList(key, list, ancestor)
        : new @PolyMutable WrappedList(key, list, ancestor);
  }

  /**
   * Collection decorator that stays in sync with the multimap values for a key. There are two kinds
   * of wrapped collections: full and subcollections. Both have a delegate pointing to the
   * underlying collection class.
   *
   * <p>Full collections, identified by a null ancestor field, contain all multimap values for a
   * given key. Its delegate is a value in {@link AbstractMapBasedMultimap#map} whenever the
   * delegate is non-empty. The {@code refreshIfEmpty}, {@code removeIfEmpty}, and {@code addToMap}
   * methods ensure that the {@code WrappedCollection} and map remain consistent.
   *
   * <p>A subcollection, such as a sublist, contains some of the values for a given key. Its
   * ancestor field points to the full wrapped collection with all values for the key. The
   * subcollection {@code refreshIfEmpty}, {@code removeIfEmpty}, and {@code addToMap} methods call
   * the corresponding methods of the full wrapped collection.
   */
  @WeakOuter
  @ReceiverDependentMutable
  class WrappedCollection extends AbstractCollection<V> {
    @ParametricNullness final K key;
    Collection<V> delegate;
    @CheckForNull final WrappedCollection ancestor;
    @CheckForNull final Collection<V> ancestorDelegate;

    WrappedCollection(
        @ParametricNullness K key,
        @ReceiverDependentMutable Collection<V> delegate,
        @CheckForNull @ReceiverDependentMutable WrappedCollection ancestor) {
      this.key = key;
      this.delegate = delegate;
      this.ancestor = ancestor;
      this.ancestorDelegate = (ancestor == null) ? null : ancestor.getDelegate();
    }

    /**
     * If the delegate collection is empty, but the multimap has values for the key, replace the
     * delegate with the new collection for the key.
     *
     * <p>For a subcollection, refresh its ancestor and validate that the ancestor delegate hasn't
     * changed.
     */
    void refreshIfEmpty(@Mutable WrappedCollection this) {
      if (ancestor != null) {
        ancestor.refreshIfEmpty();
        if (ancestor.getDelegate() != ancestorDelegate) {
          throw new ConcurrentModificationException();
        }
      } else if (delegate.isEmpty()) {
        Collection<V> newDelegate = map.get(key);
        if (newDelegate != null) {
          delegate = newDelegate;
        }
      }
    }

    /**
     * If collection is empty, remove it from {@code AbstractMapBasedMultimap.this.map}. For
     * subcollections, check whether the ancestor collection is empty.
     */
    void removeIfEmpty(@Mutable WrappedCollection this) {
      if (ancestor != null) {
        ancestor.removeIfEmpty();
      } else if (delegate.isEmpty()) {
        map.remove(key);
      }
    }

    @ParametricNullness
    K getKey(@Readonly WrappedCollection this) {
      return key;
    }

    /**
     * Add the delegate to the map. Other {@code WrappedCollection} methods should call this method
     * after adding elements to a previously empty collection.
     *
     * <p>Subcollection add the ancestor's delegate instead.
     */
    void addToMap(@Mutable WrappedCollection this) {
      if (ancestor != null) {
        ancestor.addToMap();
      } else {
        map.put(key, delegate);
      }
    }

    @Override
    public @NonNegative int size(@Readonly WrappedCollection this) {
      refreshIfEmpty();
      return delegate.size();
    }

    @Override
    public boolean equals(@Readonly WrappedCollection this, @CheckForNull @UnknownSignedness @Readonly Object object) {
      if (object == this) {
        return true;
      }
      refreshIfEmpty();
      return delegate.equals(object);
    }

    @Override
    public int hashCode(@UnknownSignedness @Readonly WrappedCollection this) {
      refreshIfEmpty();
      return delegate.hashCode();
    }

    @Override
    public String toString(@Readonly WrappedCollection this) {
      refreshIfEmpty();
      return delegate.toString();
    }

    @PolyMutable Collection<V> getDelegate(@PolyMutable WrappedCollection this) {
      return delegate;
    }

    @Override
    @SuppressWarnings("mutability:method.invocation.invalid") // refreshIfEmpty mutates 'this'
    public Iterator<V> iterator(@Readonly WrappedCollection this) {
      refreshIfEmpty();
      return new WrappedIterator();
    }

    @Override
    @SuppressWarnings("mutability:method.invocation.invalid") // refreshIfEmpty mutates 'this'
    public Spliterator<V> spliterator(@Readonly WrappedCollection this) {
      refreshIfEmpty();
      return delegate.spliterator();
    }

    /** Collection iterator for {@code WrappedCollection}. */
    @ReceiverDependentMutable
    class WrappedIterator implements Iterator<V> {
      final Iterator<V> delegateIterator;
      final Collection<V> originalDelegate = delegate;

      WrappedIterator() {
        delegateIterator = iteratorOrListIterator(delegate);
      }

      WrappedIterator(@ReceiverDependentMutable Iterator<V> delegateIterator) {
        this.delegateIterator = delegateIterator;
      }

      /**
       * If the delegate changed since the iterator was created, the iterator is no longer valid.
       */
      void validateIterator() {
        refreshIfEmpty();
        if (delegate != originalDelegate) {
          throw new ConcurrentModificationException();
        }
      }

      @Override
      public boolean hasNext() {
        validateIterator();
        return delegateIterator.hasNext();
      }

      @Override
      @ParametricNullness
      public V next() {
        validateIterator();
        return delegateIterator.next();
      }

      @Override
      public void remove() {
        delegateIterator.remove();
        totalSize--;
        removeIfEmpty();
      }

      Iterator<V> getDelegateIterator() {
        validateIterator();
        return delegateIterator;
      }
    }

    @Override
    public boolean add(@ParametricNullness V value) {
      refreshIfEmpty();
      boolean wasEmpty = delegate.isEmpty();
      boolean changed = delegate.add(value);
      if (changed) {
        totalSize++;
        if (wasEmpty) {
          addToMap();
        }
      }
      return changed;
    }

    @CheckForNull
    WrappedCollection getAncestor() {
      return ancestor;
    }

    // The following methods are provided for better performance.

    @Override
    public boolean addAll(Collection<? extends V> collection) {
      if (collection.isEmpty()) {
        return false;
      }
      int oldSize = size(); // calls refreshIfEmpty
      boolean changed = delegate.addAll(collection);
      if (changed) {
        int newSize = delegate.size();
        totalSize += (newSize - oldSize);
        if (oldSize == 0) {
          addToMap();
        }
      }
      return changed;
    }

    @Override
    public boolean contains(@CheckForNull @UnknownSignedness Object o) {
      refreshIfEmpty();
      return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      refreshIfEmpty();
      return delegate.containsAll(c);
    }

    @Override
    public void clear() {
      int oldSize = size(); // calls refreshIfEmpty
      if (oldSize == 0) {
        return;
      }
      delegate.clear();
      totalSize -= oldSize;
      removeIfEmpty(); // maybe shouldn't be removed if this is a sublist
    }

    @Override
    public boolean remove(@Mutable  @CheckForNull @UnknownSignedness @Readonly Object o) {
      refreshIfEmpty();
      boolean changed = delegate.remove(o);
      if (changed) {
        totalSize--;
        removeIfEmpty();
      }
      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      if (c.isEmpty()) {
        return false;
      }
      int oldSize = size(); // calls refreshIfEmpty
      boolean changed = delegate.removeAll(c);
      if (changed) {
        int newSize = delegate.size();
        totalSize += (newSize - oldSize);
        removeIfEmpty();
      }
      return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      checkNotNull(c);
      int oldSize = size(); // calls refreshIfEmpty
      boolean changed = delegate.retainAll(c);
      if (changed) {
        int newSize = delegate.size();
        totalSize += (newSize - oldSize);
        removeIfEmpty();
      }
      return changed;
    }
  }

  private static <E extends @Nullable @Readonly Object> Iterator<E> iteratorOrListIterator(
      Collection<E> collection) {
    return (collection instanceof List)
        ? ((List<E>) collection).listIterator()
        : collection.iterator();
  }

  /** Set decorator that stays in sync with the multimap values for a key. */
  @WeakOuter
  @ReceiverDependentMutable
  class WrappedSet extends WrappedCollection implements Set<V> {
    WrappedSet(@ParametricNullness K key, @ReceiverDependentMutable Set<V> delegate) {
      super(key, delegate, null);
    }

    @Override
    public boolean removeAll(@Mutable WrappedSet this, @Readonly Collection<?> c) {
      if (c.isEmpty()) {
        return false;
      }
      int oldSize = size(); // calls refreshIfEmpty

      // Guava issue 1013: AbstractSet and most JDK set implementations are
      // susceptible to quadratic removeAll performance on lists;
      // use a slightly smarter implementation here
      boolean changed = Sets.removeAllImpl((Set<V>) delegate, c);
      if (changed) {
        int newSize = delegate.size();
        totalSize += (newSize - oldSize);
        removeIfEmpty();
      }
      return changed;
    }
  }

  /** SortedSet decorator that stays in sync with the multimap values for a key. */
  @WeakOuter
  @ReceiverDependentMutable
  class WrappedSortedSet extends WrappedCollection implements SortedSet<V> {
    WrappedSortedSet(
        @ParametricNullness K key,
        @ReceiverDependentMutable SortedSet<V> delegate,
        @CheckForNull @ReceiverDependentMutable WrappedCollection ancestor) {
      super(key, delegate, ancestor);
    }

    SortedSet<V> getSortedSetDelegate() {
      return (SortedSet<V>) getDelegate();
    }

    @Override
    @CheckForNull
    public Comparator<? super V> comparator() {
      return getSortedSetDelegate().comparator();
    }

    @Override
    @ParametricNullness
    public V first() {
      refreshIfEmpty();
      return getSortedSetDelegate().first();
    }

    @Override
    @ParametricNullness
    public V last() {
      refreshIfEmpty();
      return getSortedSetDelegate().last();
    }

    @Override
    public SortedSet<V> headSet(@ParametricNullness V toElement) {
      refreshIfEmpty();
      return new WrappedSortedSet(
          getKey(),
          getSortedSetDelegate().headSet(toElement),
          (getAncestor() == null) ? this : getAncestor());
    }

    @Override
    public SortedSet<V> subSet(@ParametricNullness V fromElement, @ParametricNullness V toElement) {
      refreshIfEmpty();
      return new WrappedSortedSet(
          getKey(),
          getSortedSetDelegate().subSet(fromElement, toElement),
          (getAncestor() == null) ? this : getAncestor());
    }

    @Override
    public SortedSet<V> tailSet(@ParametricNullness V fromElement) {
      refreshIfEmpty();
      return new WrappedSortedSet(
          getKey(),
          getSortedSetDelegate().tailSet(fromElement),
          (getAncestor() == null) ? this : getAncestor());
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  class WrappedNavigableSet extends WrappedSortedSet implements NavigableSet<V> {
    WrappedNavigableSet(
        @ParametricNullness K key,
        @ReceiverDependentMutable NavigableSet<V> delegate,
        @CheckForNull @ReceiverDependentMutable WrappedCollection ancestor) {
      super(key, delegate, ancestor);
    }

    @Override
    @PolyMutable NavigableSet<V> getSortedSetDelegate(@PolyMutable WrappedNavigableSet this) {
      return (@PolyMutable NavigableSet<V>) super.getSortedSetDelegate();
    }

    @Override
    @CheckForNull
    public V lower(@ParametricNullness V v) {
      return getSortedSetDelegate().lower(v);
    }

    @Override
    @CheckForNull
    public V floor(@ParametricNullness V v) {
      return getSortedSetDelegate().floor(v);
    }

    @Override
    @CheckForNull
    public V ceiling(@ParametricNullness V v) {
      return getSortedSetDelegate().ceiling(v);
    }

    @Override
    @CheckForNull
    public V higher(@ParametricNullness V v) {
      return getSortedSetDelegate().higher(v);
    }

    @Override
    @CheckForNull
    public V pollFirst() {
      return Iterators.pollNext(iterator());
    }

    @Override
    @CheckForNull
    public V pollLast() {
      return Iterators.pollNext(descendingIterator());
    }

    private NavigableSet<V> wrap(NavigableSet<V> wrapped) {
      return new WrappedNavigableSet(key, wrapped, (getAncestor() == null) ? this : getAncestor());
    }

    @Override
    public NavigableSet<V> descendingSet() {
      return wrap(getSortedSetDelegate().descendingSet());
    }

    @Override
    public Iterator<V> descendingIterator() {
      return new WrappedIterator(getSortedSetDelegate().descendingIterator());
    }

    @Override
    public NavigableSet<V> subSet(
        @ParametricNullness V fromElement,
        boolean fromInclusive,
        @ParametricNullness V toElement,
        boolean toInclusive) {
      return wrap(
          getSortedSetDelegate().subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public NavigableSet<V> headSet(@ParametricNullness V toElement, boolean inclusive) {
      return wrap(getSortedSetDelegate().headSet(toElement, inclusive));
    }

    @Override
    public NavigableSet<V> tailSet(@ParametricNullness V fromElement, boolean inclusive) {
      return wrap(getSortedSetDelegate().tailSet(fromElement, inclusive));
    }
  }

  /** List decorator that stays in sync with the multimap values for a key. */
  @WeakOuter
  @ReceiverDependentMutable
  class WrappedList extends WrappedCollection implements List<V> {
    WrappedList(
        @ParametricNullness K key, @ReceiverDependentMutable List<V> delegate, @CheckForNull WrappedCollection ancestor) {
      super(key, delegate, ancestor);
    }

    List<V> getListDelegate() {
      return (List<V>) getDelegate();
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
      if (c.isEmpty()) {
        return false;
      }
      int oldSize = size(); // calls refreshIfEmpty
      boolean changed = getListDelegate().addAll(index, c);
      if (changed) {
        int newSize = getDelegate().size();
        totalSize += (newSize - oldSize);
        if (oldSize == 0) {
          addToMap();
        }
      }
      return changed;
    }

    @Override
    @ParametricNullness
    public V get(int index) {
      refreshIfEmpty();
      return getListDelegate().get(index);
    }

    @Override
    @ParametricNullness
    public V set(int index, @ParametricNullness V element) {
      refreshIfEmpty();
      return getListDelegate().set(index, element);
    }

    @Override
    public void add(int index, @ParametricNullness V element) {
      refreshIfEmpty();
      boolean wasEmpty = getDelegate().isEmpty();
      getListDelegate().add(index, element);
      totalSize++;
      if (wasEmpty) {
        addToMap();
      }
    }

    @Override
    @ParametricNullness
    public V remove(int index) {
      refreshIfEmpty();
      V value = getListDelegate().remove(index);
      totalSize--;
      removeIfEmpty();
      return value;
    }

    @Override
    public int indexOf(@CheckForNull @UnknownSignedness Object o) {
      refreshIfEmpty();
      return getListDelegate().indexOf(o);
    }

    @Override
    public int lastIndexOf(@CheckForNull @UnknownSignedness Object o) {
      refreshIfEmpty();
      return getListDelegate().lastIndexOf(o);
    }

    @Override
    public ListIterator<V> listIterator() {
      refreshIfEmpty();
      return new WrappedListIterator();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
      refreshIfEmpty();
      return new WrappedListIterator(index);
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
      refreshIfEmpty();
      return wrapList(
          getKey(),
          getListDelegate().subList(fromIndex, toIndex),
          (getAncestor() == null) ? this : getAncestor());
    }

    /** ListIterator decorator. */
    @ReceiverDependentMutable
    private class WrappedListIterator extends WrappedIterator implements ListIterator<V> {
      WrappedListIterator() {}

      public WrappedListIterator(int index) {
        super(getListDelegate().listIterator(index));
      }

      private ListIterator<V> getDelegateListIterator() {
        return (ListIterator<V>) getDelegateIterator();
      }

      @Override
      public boolean hasPrevious() {
        return getDelegateListIterator().hasPrevious();
      }

      @Override
      @ParametricNullness
      public V previous() {
        return getDelegateListIterator().previous();
      }

      @Override
      public @NonNegative int nextIndex() {
        return getDelegateListIterator().nextIndex();
      }

      @Override
      public @NonNegative int previousIndex() {
        return getDelegateListIterator().previousIndex();
      }

      @Override
      public void set(@ParametricNullness V value) {
        getDelegateListIterator().set(value);
      }

      @Override
      public void add(@ParametricNullness V value) {
        boolean wasEmpty = isEmpty();
        getDelegateListIterator().add(value);
        totalSize++;
        if (wasEmpty) {
          addToMap();
        }
      }
    }
  }

  /**
   * List decorator that stays in sync with the multimap values for a key and supports rapid random
   * access.
   */
  @ReceiverDependentMutable
  private class RandomAccessWrappedList extends WrappedList implements RandomAccess {
    RandomAccessWrappedList(
        @ParametricNullness K key, @ReceiverDependentMutable List<V> delegate, @CheckForNull WrappedCollection ancestor) {
      super(key, delegate, ancestor);
    }
  }

  @Override
  @PolyMutable Set<K> createKeySet(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new @PolyMutable KeySet(map);
  }

  final @PolyMutable Set<K> createMaybeNavigableKeySet(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    if (map instanceof NavigableMap) {
      return new @PolyMutable NavigableKeySet((@PolyMutable NavigableMap<K, @PolyMutable Collection<V>>) map);
    } else if (map instanceof SortedMap) {
      return new @PolyMutable SortedKeySet((@PolyMutable SortedMap<K, @PolyMutable Collection<V>>) map);
    } else {
      return new @PolyMutable KeySet(map);
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class KeySet extends Maps.KeySet<K, @ReceiverDependentMutable Collection<V>> {
    KeySet(final @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> subMap) {
      super(subMap);
    }

    @Override
    public Iterator<K> iterator() {
      final Iterator<Entry<K, Collection<V>>> entryIterator = map().entrySet().iterator();
      return new Iterator<K>() {
        @CheckForNull Entry<K, Collection<V>> entry;

        @Override
        public boolean hasNext() {
          return entryIterator.hasNext();
        }

        @Override
        @ParametricNullness
        public K next() {
          entry = entryIterator.next();
          return entry.getKey();
        }

        @Override
        public void remove() {
          checkState(entry != null, "no calls to next() since the last call to remove()");
          Collection<V> collection = entry.getValue();
          entryIterator.remove();
          totalSize -= collection.size();
          collection.clear();
          entry = null;
        }
      };
    }

    // The following methods are included for better performance.

    @Override
    public Spliterator<K> spliterator() {
      return map().keySet().spliterator();
    }

    @Override
    public boolean remove(@CheckForNull @UnknownSignedness @Readonly Object key) {
      int count = 0;
      Collection<V> collection = map().remove(key);
      if (collection != null) {
        count = collection.size();
        collection.clear();
        totalSize -= count;
      }
      return count > 0;
    }

    @Override
    public void clear() {
      Iterators.clear(iterator());
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return map().keySet().containsAll(c);
    }

    @Override
    public boolean equals(@CheckForNull @UnknownSignedness @Readonly Object object) {
      return this == object || this.map().keySet().equals(object);
    }

    @Override
    public int hashCode(@UnknownSignedness @Readonly KeySet this) {
      return map().keySet().hashCode();
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class SortedKeySet extends KeySet implements SortedSet<K> {

    SortedKeySet(SortedMap<K, Collection<V>> subMap) {
      super(subMap);
    }

    SortedMap<K, Collection<V>> sortedMap() {
      return (SortedMap<K, Collection<V>>) super.map();
    }

    @Override
    @CheckForNull
    public Comparator<? super K> comparator() {
      return sortedMap().comparator();
    }

    @Override
    @ParametricNullness
    public K first() {
      return sortedMap().firstKey();
    }

    @Override
    public SortedSet<K> headSet(@ParametricNullness K toElement) {
      return new SortedKeySet(sortedMap().headMap(toElement));
    }

    @Override
    @ParametricNullness
    public K last() {
      return sortedMap().lastKey();
    }

    @Override
    public SortedSet<K> subSet(@ParametricNullness K fromElement, @ParametricNullness K toElement) {
      return new SortedKeySet(sortedMap().subMap(fromElement, toElement));
    }

    @Override
    public SortedSet<K> tailSet(@ParametricNullness K fromElement) {
      return new SortedKeySet(sortedMap().tailMap(fromElement));
    }
  }

  @WeakOuter
  private final class NavigableKeySet extends SortedKeySet implements NavigableSet<K> {
    NavigableKeySet(NavigableMap<K, Collection<V>> subMap) {
      super(subMap);
    }

    @Override
    NavigableMap<K, Collection<V>> sortedMap() {
      return (NavigableMap<K, Collection<V>>) super.sortedMap();
    }

    @Override
    @CheckForNull
    public K lower(@ParametricNullness K k) {
      return sortedMap().lowerKey(k);
    }

    @Override
    @CheckForNull
    public K floor(@ParametricNullness K k) {
      return sortedMap().floorKey(k);
    }

    @Override
    @CheckForNull
    public K ceiling(@ParametricNullness K k) {
      return sortedMap().ceilingKey(k);
    }

    @Override
    @CheckForNull
    public K higher(@ParametricNullness K k) {
      return sortedMap().higherKey(k);
    }

    @Override
    @CheckForNull
    public K pollFirst() {
      return Iterators.pollNext(iterator());
    }

    @Override
    @CheckForNull
    public K pollLast() {
      return Iterators.pollNext(descendingIterator());
    }

    @Override
    public NavigableSet<K> descendingSet() {
      return new NavigableKeySet(sortedMap().descendingMap());
    }

    @Override
    public Iterator<K> descendingIterator() {
      return descendingSet().iterator();
    }

    @Override
    public NavigableSet<K> headSet(@ParametricNullness K toElement) {
      return headSet(toElement, false);
    }

    @Override
    public NavigableSet<K> headSet(@ParametricNullness K toElement, boolean inclusive) {
      return new NavigableKeySet(sortedMap().headMap(toElement, inclusive));
    }

    @Override
    public NavigableSet<K> subSet(
        @ParametricNullness K fromElement, @ParametricNullness K toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<K> subSet(
        @ParametricNullness K fromElement,
        boolean fromInclusive,
        @ParametricNullness K toElement,
        boolean toInclusive) {
      return new NavigableKeySet(
          sortedMap().subMap(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public NavigableSet<K> tailSet(@ParametricNullness K fromElement) {
      return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<K> tailSet(@ParametricNullness K fromElement, boolean inclusive) {
      return new NavigableKeySet(sortedMap().tailMap(fromElement, inclusive));
    }
  }

  /** Removes all values for the provided key. */
  private void removeValuesForKey(@CheckForNull @Readonly Object key) {
    Collection<V> collection = Maps.safeRemove(map, key);

    if (collection != null) {
      int count = collection.size();
      collection.clear();
      totalSize -= count;
    }
  }

  @ReceiverDependentMutable
  private abstract class Itr<T extends @Nullable @Readonly Object> implements Iterator<T> {
    final Iterator<Entry<K, Collection<V>>> keyIterator;
    @CheckForNull K key;
    @CheckForNull Collection<V> collection;
    Iterator<V> valueIterator;

    Itr() {
      keyIterator = map.entrySet().iterator();
      key = null;
      collection = null;
      valueIterator = Iterators.emptyModifiableIterator();
    }

    abstract T output(@ParametricNullness K key, @ParametricNullness V value);

    @Override
    public boolean hasNext() {
      return keyIterator.hasNext() || valueIterator.hasNext();
    }

    @Override
    @ParametricNullness
    public T next() {
      if (!valueIterator.hasNext()) {
        Entry<K, Collection<V>> mapEntry = keyIterator.next();
        key = mapEntry.getKey();
        collection = mapEntry.getValue();
        valueIterator = collection.iterator();
      }
      /*
       * uncheckedCastNullableTToT is safe: The first call to this method always enters the !hasNext() case and
       * populates key, after which it's never cleared.
       */
      return output(uncheckedCastNullableTToT(key), valueIterator.next());
    }

    @Override
    public void remove() {
      valueIterator.remove();
      /*
       * requireNonNull is safe because we've already initialized `collection`. If we hadn't, then
       * valueIterator.remove() would have failed.
       */
      if (requireNonNull(collection).isEmpty()) {
        keyIterator.remove();
      }
      totalSize--;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>The iterator generated by the returned collection traverses the values for one key, followed
   * by the values of a second key, and so on.
   */
  @Override
  public @PolyMutable Collection<V> values(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return super.values();
  }

  @Override
  @PolyMutable Collection<V> createValues(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new Values();
  }

  @Override
  @CFComment("PICO: follow super's polymutable")
  Iterator<V> valueIterator(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new Itr<V>() {
      @Override
      @ParametricNullness
      V output(@ParametricNullness K key, @ParametricNullness V value) {
        return value;
      }
    };
  }

  @Override
  @CFComment("PICO: follow super's polymutable")
  Spliterator<V> valueSpliterator(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return CollectSpliterators.flatMap(
        map.values().spliterator(), Collection::spliterator, Spliterator.SIZED, size());
  }

  /*
   * TODO(kevinb): should we copy this javadoc to each concrete class, so that
   * classes like LinkedHashMultimap that need to say something different are
   * still able to {@inheritDoc} all the way from Multimap?
   */

  @Override
  @PolyMutable Multiset<K> createKeys(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new Multimaps.Keys<K, V>(this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The iterator generated by the returned collection traverses the values for one key, followed
   * by the values of a second key, and so on.
   *
   * <p>Each entry is an immutable snapshot of a key-value mapping in the multimap, taken at the
   * time the entry is returned by a method call to the collection or its iterator.
   */
  @Override
  public @PolyMutable Collection<@PolyMutable Entry<K, V>> entries(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return super.entries();
  }

  @Override
  @PolyMutable Collection<@PolyMutable Entry<K, V>> createEntries(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    if (this instanceof SetMultimap) {
      return new EntrySet();
    } else {
      return new Entries();
    }
  }

  /**
   * Returns an iterator across all key-value map entries, used by {@code entries().iterator()} and
   * {@code values().iterator()}. The default behavior, which traverses the values for one key, the
   * values for a second key, and so on, suffices for most {@code AbstractMapBasedMultimap}
   * implementations.
   *
   * @return an iterator across map entries
   */
  @Override
  Iterator<@PolyMutable Entry<K, V>> entryIterator(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new Itr<@PolyMutable Entry<K, V>>() {
      @Override
      @Immutable Entry<K, V> output(@ParametricNullness K key, @ParametricNullness V value) {
        return Maps.immutableEntry(key, value);
      }
    };
  }

  @Override
  Spliterator<@PolyMutable Entry<K, V>> entrySpliterator(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return CollectSpliterators.flatMap(
        map.entrySet().spliterator(),
        keyToValueCollectionEntry -> {
          K key = keyToValueCollectionEntry.getKey();
          Collection<V> valueCollection = keyToValueCollectionEntry.getValue();
          return CollectSpliterators.map(
              valueCollection.spliterator(), (V value) -> Maps.immutableEntry(key, value));
        },
        Spliterator.SIZED,
        size());
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    map.forEach(
        (key, valueCollection) -> valueCollection.forEach(value -> action.accept(key, value)));
  }

  @Override
  @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    return new AsMap(map);
  }

  final Map<K, @PolyMutable Collection<V>> createMaybeNavigableAsMap(@PolyMutable AbstractMapBasedMultimap<K, V> this) {
    if (map instanceof NavigableMap) {
      return new NavigableAsMap((NavigableMap<K, Collection<V>>) map);
    } else if (map instanceof SortedMap) {
      return new SortedAsMap((SortedMap<K, Collection<V>>) map);
    } else {
      return new AsMap(map);
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class AsMap extends ViewCachingAbstractMap<K, Collection<V>> {
    /**
     * Usually the same as map, but smaller for the headMap(), tailMap(), or subMap() of a
     * SortedAsMap.
     */
    final transient Map<K, @ReceiverDependentMutable  Collection<V>> submap;

    AsMap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable  Collection<V>> submap) {
      this.submap = submap;
    }

    @Override
    protected @PolyMutable Set<@PolyMutable Entry<K, @PolyMutable Collection<V>>> createEntrySet(@PolyMutable AsMap this) {
      return new AsMapEntries();
    }

    // The following methods are included for performance.

    @Override
    public boolean containsKey(@Readonly AsMap this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return Maps.safeContainsKey(submap, key);
    }

    @Override
    @CheckForNull
    public @PolyMutable Collection<V> get(@PolyMutable AsMap this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      Collection<V> collection = Maps.safeGet(submap, key);
      if (collection == null) {
        return null;
      }
      @SuppressWarnings("unchecked")
      K k = (K) key;
      return wrapCollection(k, collection);
    }

    @Override
    public @PolyMutable Set<@KeyFor({"this"}) K> keySet(@PolyMutable AsMap this) {
      return AbstractMapBasedMultimap.this.keySet();
    }

    @Override
    public @NonNegative int size(@Readonly AsMap this) {
      return submap.size();
    }

    @Override
    @CheckForNull
    public Collection<V> remove(@Mutable AsMap this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      Collection<V> collection = submap.remove(key);
      if (collection == null) {
        return null;
      }

      Collection<V> output = createCollection();
      output.addAll(collection);
      totalSize -= collection.size();
      collection.clear();
      return output;
    }

    @Override
    public boolean equals(@Readonly AsMap this, @CheckForNull @Readonly Object object) {
      return this == object || submap.equals(object);
    }

    @Override
    public int hashCode(@UnknownSignedness @Readonly AsMap this) {
      return submap.hashCode();
    }

    @Override
    public String toString(@Readonly AsMap this) {
      return submap.toString();
    }

    @Override
    public void clear(@Mutable AsMap this) {
      if (submap == map) {
        AbstractMapBasedMultimap.this.clear();
      } else {
        Iterators.clear(new AsMapIterator());
      }
    }

    @Immutable Entry<K, Collection<V>> wrapEntry(Entry<K, Collection<V>> entry) {
      K key = entry.getKey();
      return Maps.immutableEntry(key, wrapCollection(key, entry.getValue()));
    }

    @WeakOuter
    @ReceiverDependentMutable
    class AsMapEntries extends Maps.EntrySet<K, @ReceiverDependentMutable Collection<V>> {
      @Override
      @PolyMutable Map<K, @PolyMutable Collection<V>> map(@PolyMutable AsMapEntries this) {
        return AsMap.this;
      }

      @Override
      public Iterator<Entry<K, Collection<V>>> iterator(@PolyMutable AsMapEntries this) {
        return new AsMapIterator();
      }

      @Override
      public Spliterator<Entry<K, Collection<V>>> spliterator() {
        return CollectSpliterators.map(submap.entrySet().spliterator(), AsMap.this::wrapEntry);
      }

      // The following methods are included for performance.

      @Override
      public boolean contains(@CheckForNull @UnknownSignedness @Readonly Object o) {
        return Collections2.safeContains(submap.entrySet(), o);
      }

      @Override
      public boolean remove(@CheckForNull @UnknownSignedness @Readonly Object o) {
        if (!contains(o)) {
          return false;
        }
        // requireNonNull is safe because of the contains check.
        Entry<?, ?> entry = requireNonNull((Entry<?, ?>) o);
        removeValuesForKey(entry.getKey());
        return true;
      }
    }

    /** Iterator across all keys and value collections. */
    class AsMapIterator implements Iterator<Entry<K, Collection<V>>> {
      final Iterator<Entry<K, Collection<V>>> delegateIterator = submap.entrySet().iterator();
      @CheckForNull Collection<V> collection;

      @Override
      public boolean hasNext() {
        return delegateIterator.hasNext();
      }

      @Override
      public Entry<K, Collection<V>> next() {
        Entry<K, Collection<V>> entry = delegateIterator.next();
        collection = entry.getValue();
        return wrapEntry(entry);
      }

      @Override
      public void remove() {
        checkState(collection != null, "no calls to next() since the last call to remove()");
        delegateIterator.remove();
        totalSize -= collection.size();
        collection.clear();
        collection = null;
      }
    }
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class SortedAsMap extends AsMap implements SortedMap<K, Collection<V>> {
    SortedAsMap(@ReceiverDependentMutable SortedMap<K, Collection<V>> submap) {
      super(submap);
    }

    @PolyMutable SortedMap<K, @PolyMutable Collection<V>> sortedMap(@PolyMutable SortedAsMap this) {
      return (@PolyMutable SortedMap<K, @PolyMutable Collection<V>>) submap;
    }

    @Override
    @CheckForNull
    public Comparator<? super K> comparator(@Readonly SortedAsMap this) {
      return sortedMap().comparator();
    }

    @Override
    @ParametricNullness
    public @KeyFor("this") K firstKey(@Readonly SortedAsMap this) {
      return sortedMap().firstKey();
    }

    @Override
    @ParametricNullness
    public @KeyFor("this") K lastKey(@Readonly SortedAsMap this) {
      return sortedMap().lastKey();
    }

    @Override
    public @PolyMutable SortedMap<K, @PolyMutable Collection<V>> headMap(@PolyMutable SortedAsMap this, @ParametricNullness K toKey) {
      return new SortedAsMap(sortedMap().headMap(toKey));
    }

    @Override
    public @PolyMutable SortedMap<K, @PolyMutable Collection<V>> subMap(
        @ParametricNullness K fromKey, @ParametricNullness K toKey) {
      return new @PolyMutable SortedAsMap(sortedMap().subMap(fromKey, toKey));
    }

    @Override
    public @PolyMutable SortedMap<K, @PolyMutable Collection<V>> tailMap(@ParametricNullness K fromKey) {
      return new @PolyMutable SortedAsMap(sortedMap().tailMap(fromKey));
    }

    @CheckForNull SortedSet<K> sortedKeySet;

    // returns a SortedSet, even though returning a Set would be sufficient to
    // satisfy the SortedMap.keySet() interface
    @Override
    public @PolyMutable SortedSet<@KeyFor({"this"}) K> keySet(@PolyMutable SortedAsMap this) {
      SortedSet<K> result = sortedKeySet;
      return (result == null) ? sortedKeySet = createKeySet() : result;
    }

    @Override
    @PolyMutable SortedSet<K> createKeySet(@PolyMutable SortedAsMap this) {
      return new @PolyMutable SortedKeySet(sortedMap());
    }
  }

  private final class NavigableAsMap extends SortedAsMap implements NavigableMap<K, Collection<V>> {

    NavigableAsMap(NavigableMap<K, @ReceiverDependentMutable Collection<V>> submap) {
      super(submap);
    }

    @Override
    NavigableMap<K, @PolyMutable Collection<V>> sortedMap(@PolyMutable NavigableAsMap this) {
      return (NavigableMap<K, @PolyMutable Collection<V>>) super.sortedMap();
    }

    @Override
    @CheckForNull
    public @PolyMutable Entry<K, @PolyMutable Collection<V>> lowerEntry(@PolyMutable NavigableAsMap this, @ParametricNullness K key) {
      Entry<K, @PolyMutable Collection<V>> entry = sortedMap().lowerEntry(key);
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public K lowerKey(@Readonly NavigableAsMap this, @ParametricNullness K key) {
      return sortedMap().lowerKey(key);
    }

    @Override
    @CheckForNull
    public @PolyMutable Entry<K, @PolyMutable Collection<V>> floorEntry(@PolyMutable NavigableAsMap this, @ParametricNullness K key) {
        @PolyMutable Entry<K, @PolyMutable Collection<V>> entry = sortedMap().floorEntry(key);
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public K floorKey(@Readonly NavigableAsMap this, @ParametricNullness K key) {
      return sortedMap().floorKey(key);
    }

    @Override
    @CheckForNull
    public Entry<K, @PolyMutable Collection<V>> ceilingEntry(@PolyMutable NavigableAsMap this, @ParametricNullness K key) {
      Entry<K, @PolyMutable Collection<V>> entry = sortedMap().ceilingEntry(key);
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public K ceilingKey(@Readonly NavigableAsMap this, @ParametricNullness K key) {
      return sortedMap().ceilingKey(key);
    }

    @Override
    @CheckForNull
    public Entry<K, @PolyMutable Collection<V>> higherEntry(@PolyMutable NavigableAsMap this, @ParametricNullness K key) {
      Entry<K, @PolyMutable Collection<V>> entry = sortedMap().higherEntry(key);
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public K higherKey(@Readonly NavigableAsMap this, @ParametricNullness K key) {
      return sortedMap().higherKey(key);
    }

    @Override
    @CheckForNull
    public Entry<K, @PolyMutable Collection<V>> firstEntry(@PolyMutable NavigableAsMap this) {
      Entry<K, @PolyMutable Collection<V>> entry = sortedMap().firstEntry();
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public Entry<K, @PolyMutable Collection<V>> lastEntry(@PolyMutable NavigableAsMap this) {
      Entry<K, @PolyMutable Collection<V>> entry = sortedMap().lastEntry();
      return (entry == null) ? null : wrapEntry(entry);
    }

    @Override
    @CheckForNull
    public @Immutable Entry<K, Collection<V>> pollFirstEntry(@Mutable NavigableAsMap this) {
      return pollAsMapEntry(entrySet().iterator());
    }

    @Override
    @CheckForNull
    public @Immutable Entry<K, Collection<V>> pollLastEntry(@Mutable NavigableAsMap this) {
      return pollAsMapEntry(descendingMap().entrySet().iterator());
    }

    @CheckForNull
    @Immutable Entry<K, Collection<V>> pollAsMapEntry(@Mutable NavigableAsMap this, Iterator<Entry<K, Collection<V>>> entryIterator) {
      if (!entryIterator.hasNext()) {
        return null;
      }
      Entry<K, Collection<V>> entry = entryIterator.next();
      Collection<V> output = createCollection();
      output.addAll(entry.getValue());
      entryIterator.remove();
      return Maps.immutableEntry(entry.getKey(), unmodifiableCollectionSubclass(output));
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> descendingMap(@PolyMutable NavigableAsMap this) {
      return new @PolyMutable NavigableAsMap(sortedMap().descendingMap());
    }

    @Override
    public @PolyMutable NavigableSet<@KeyFor({"this"}) K> keySet(@PolyMutable NavigableAsMap this) {
      return (@PolyMutable NavigableSet<K>) super.keySet();
    }

    @Override
    @PolyMutable NavigableSet<K> createKeySet(@PolyMutable NavigableAsMap this) {
      return new @PolyMutable NavigableKeySet(sortedMap());
    }

    @Override
    public @PolyMutable NavigableSet<@KeyFor({"this"}) K> navigableKeySet(@PolyMutable NavigableAsMap this) {
      return keySet();
    }

    @Override
    public @PolyMutable NavigableSet<@KeyFor({"this"}) K> descendingKeySet(@PolyMutable NavigableAsMap this) {
      return descendingMap().navigableKeySet();
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> subMap(
            @PolyMutable NavigableAsMap this,
        @ParametricNullness K fromKey, @ParametricNullness K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> subMap(
            @PolyMutable NavigableAsMap this,
        @ParametricNullness K fromKey,
        boolean fromInclusive,
        @ParametricNullness K toKey,
        boolean toInclusive) {
      return new @PolyMutable NavigableAsMap(sortedMap().subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> headMap(@PolyMutable NavigableAsMap this, @ParametricNullness K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> headMap(@PolyMutable NavigableAsMap this, @ParametricNullness K toKey, boolean inclusive) {
      return new @PolyMutable NavigableAsMap(sortedMap().headMap(toKey, inclusive));
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> tailMap(@PolyMutable NavigableAsMap this, @ParametricNullness K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public @PolyMutable NavigableMap<K, @PolyMutable Collection<V>> tailMap(
            @PolyMutable NavigableAsMap this,
        @ParametricNullness K fromKey, boolean inclusive) {
      return new @PolyMutable NavigableAsMap(sortedMap().tailMap(fromKey, inclusive));
    }
  }

  private static final long serialVersionUID = 2447537837011683357L;
}
