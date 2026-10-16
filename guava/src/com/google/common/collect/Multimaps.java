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
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.CFComment;

/**
 * Provides static methods acting on or generating a {@code Multimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#multimaps">{@code
 * Multimaps}</a>.
 *
 * @author Jared Levy
 * @author Robert Konigsberg
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@AnnotatedFor({"nullness"})
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Multimaps {
  private Multimaps() {}

  /**
   * Returns a {@code Collector} accumulating entries into a {@code Multimap} generated from the
   * specified supplier. The keys and values of the entries are the result of applying the provided
   * mapping functions to the input elements, accumulated in the encounter order of the stream.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final ListMultimap<Character, String> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(
   *             toMultimap(
   *                  str -> str.charAt(0),
   *                  str -> str.substring(1),
   *                  MultimapBuilder.treeKeys().arrayListValues()::build));
   *
   * // is equivalent to
   *
   * static final ListMultimap<Character, String> FIRST_LETTER_MULTIMAP;
   *
   * static {
   *     FIRST_LETTER_MULTIMAP = MultimapBuilder.treeKeys().arrayListValues().build();
   *     FIRST_LETTER_MULTIMAP.put('b', "anana");
   *     FIRST_LETTER_MULTIMAP.put('a', "pple");
   *     FIRST_LETTER_MULTIMAP.put('a', "sparagus");
   *     FIRST_LETTER_MULTIMAP.put('c', "arrot");
   *     FIRST_LETTER_MULTIMAP.put('c', "herry");
   * }
   * }</pre>
   *
   * <p>To collect to an {@link ImmutableMultimap}, use either {@link
   * ImmutableSetMultimap#toImmutableSetMultimap} or {@link
   * ImmutableListMultimap#toImmutableListMultimap}.
   *
   * @since 21.0
   */
  public static <
          T extends @Nullable @Readonly Object,
          K extends @Nullable @Immutable Object,
          V extends @Nullable @Readonly Object,
          M extends Multimap<K, V>>
      Collector<T, ?, M> toMultimap(
          java.util.function.Function<? super T, ? extends K> keyFunction,
          java.util.function.Function<? super T, ? extends V> valueFunction,
          java.util.function.Supplier<M> multimapSupplier) {
    return CollectCollectors.<T, K, V, M>toMultimap(keyFunction, valueFunction, multimapSupplier);
  }

  /**
   * Returns a {@code Collector} accumulating entries into a {@code Multimap} generated from the
   * specified supplier. Each input element is mapped to a key and a stream of values, each of which
   * are put into the resulting {@code Multimap}, in the encounter order of the stream and the
   * encounter order of the streams of values.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final ListMultimap<Character, Character> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(
   *             flatteningToMultimap(
   *                  str -> str.charAt(0),
   *                  str -> str.substring(1).chars().mapToObj(c -> (char) c),
   *                  MultimapBuilder.linkedHashKeys().arrayListValues()::build));
   *
   * // is equivalent to
   *
   * static final ListMultimap<Character, Character> FIRST_LETTER_MULTIMAP;
   *
   * static {
   *     FIRST_LETTER_MULTIMAP = MultimapBuilder.linkedHashKeys().arrayListValues().build();
   *     FIRST_LETTER_MULTIMAP.putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'));
   *     FIRST_LETTER_MULTIMAP.putAll('a', Arrays.asList('p', 'p', 'l', 'e'));
   *     FIRST_LETTER_MULTIMAP.putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'));
   *     FIRST_LETTER_MULTIMAP.putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'));
   *     FIRST_LETTER_MULTIMAP.putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'));
   * }
   * }</pre>
   *
   * @since 21.0
   */
  public static <
          T extends @Nullable @Readonly Object,
          K extends @Nullable @Immutable Object,
          V extends @Nullable @Readonly Object,
          M extends Multimap<K, V>>
      Collector<T, ?, M> flatteningToMultimap(
          java.util.function.Function<? super T, ? extends K> keyFunction,
          java.util.function.Function<? super T, ? extends Stream<? extends V>> valueFunction,
          java.util.function.Supplier<M> multimapSupplier) {
    return CollectCollectors.<T, K, V, M>flatteningToMultimap(
        keyFunction, valueFunction, multimapSupplier);
  }

  /**
   * Creates a new {@code Multimap} backed by {@code map}, whose internal value collections are
   * generated by {@code factory}.
   *
   * <p><b>Warning: do not use</b> this method when the collections returned by {@code factory}
   * implement either {@link List} or {@code Set}! Use the more specific method {@link
   * #newListMultimap}, {@link #newSetMultimap} or {@link #newSortedSetMultimap} instead, to avoid
   * very surprising behavior from {@link Multimap#equals}.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the collections generated by
   * {@code factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link ArrayListMultimap#create()}, {@link
   * HashMultimap#create()}, {@link LinkedHashMultimap#create()}, {@link
   * LinkedListMultimap#create()}, {@link TreeMultimap#create()}, and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the collections
   * returned by {@code factory}. Those objects should not be manually updated and they should not
   * use soft, weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty collections that will each hold all values for a given
   *     key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> Multimap<K, V> newMultimap(
      Map<K, Collection<V>> map, final Supplier<? extends Collection<V>> factory) {
    return new CustomMultimap<>(map, factory);
  }

  @ReceiverDependentMutable
  private static class CustomMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractMapBasedMultimap<K, V> {
    transient Supplier<? extends @ReceiverDependentMutable Collection<V>> factory;

    CustomMultimap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map, Supplier<? extends @ReceiverDependentMutable Collection<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable CustomMultimap<K, V> this) {
      return createMaybeNavigableKeySet();
    }

    @Override
    @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable CustomMultimap<K, V> this) {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected @PolyMutable Collection<V> createCollection(@PolyMutable CustomMultimap<K, V> this) {
      return factory.get();
    }

    @Override
    <E extends @Nullable @Readonly Object> @Readonly Collection<E> unmodifiableCollectionSubclass(
            @Readonly Collection<E> collection) {
      if (collection instanceof NavigableSet) {
        return Sets.unmodifiableNavigableSet((NavigableSet<E>) collection);
      } else if (collection instanceof SortedSet) {
        return Collections.unmodifiableSortedSet((SortedSet<E>) collection);
      } else if (collection instanceof Set) {
        return Collections.unmodifiableSet((Set<E>) collection);
      } else if (collection instanceof List) {
        return Collections.unmodifiableList((List<E>) collection);
      } else {
        return Collections.unmodifiableCollection(collection);
      }
    }

    @Override
    Collection<V> wrapCollection(@ParametricNullness K key, Collection<V> collection) {
      if (collection instanceof List) {
        return wrapList(key, (List<V>) collection, null);
      } else if (collection instanceof NavigableSet) {
        return new WrappedNavigableSet(key, (NavigableSet<V>) collection, null);
      } else if (collection instanceof SortedSet) {
        return new WrappedSortedSet(key, (SortedSet<V>) collection, null);
      } else if (collection instanceof Set) {
        return new WrappedSet(key, (Set<V>) collection);
      } else {
        return new WrappedCollection(key, collection, null);
      }
    }

    // can't use Serialization writeMultimap and populateMultimap methods since
    // there's no way to generate the empty backing map.

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends Collection<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // java serialization not supported
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code ListMultimap} that uses the provided map and factory. It can generate a
   * multimap based on arbitrary {@link Map} and {@link List} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. The multimap's {@code get}, {@code
   * removeAll}, and {@code replaceValues} methods return {@code RandomAccess} lists if the factory
   * does. However, the multimap's {@code get} method returns instances of a different class than
   * does {@code factory.get()}.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the lists generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedListMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link ArrayListMultimap#create()} and {@link
   * LinkedListMultimap#create()} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the lists returned by
   * {@code factory}. Those objects should not be manually updated, they should be empty when
   * provided, and they should not use soft, weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty lists that will each hold all values for a given key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      ListMultimap<K, V> newListMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends List<V>> factory) {
    return new CustomListMultimap<>(map, factory);
  }

  @ReceiverDependentMutable
  private static class CustomListMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractListMultimap<K, V> {
    transient Supplier<? extends @ReceiverDependentMutable List<V>> factory;

    CustomListMultimap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map, Supplier<? extends @ReceiverDependentMutable List<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable CustomListMultimap<K, V> this) {
      return createMaybeNavigableKeySet();
    }

    @Override
    @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable CustomListMultimap<K, V> this) {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected @ReceiverDependentMutable List<V> createCollection(@PolyMutable CustomListMultimap<K, V> this) {
      return factory.get();
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends List<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // java serialization not supported
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code SetMultimap} that uses the provided map and factory. It can generate a
   * multimap based on arbitrary {@link Map} and {@link Set} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the sets generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedSetMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link HashMultimap#create()}, {@link
   * LinkedHashMultimap#create()}, {@link TreeMultimap#create()}, and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the sets returned by
   * {@code factory}. Those objects should not be manually updated and they should not use soft,
   * weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty sets that will each hold all values for a given key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> newSetMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends Set<V>> factory) {
    return new CustomSetMultimap<>(map, factory);
  }

  @ReceiverDependentMutable
  private static class CustomSetMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractSetMultimap<K, V> {
    transient Supplier<? extends @ReceiverDependentMutable Set<V>> factory;

    CustomSetMultimap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map, Supplier<? extends @ReceiverDependentMutable Set<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable CustomSetMultimap<K, V> this) {
      return createMaybeNavigableKeySet();
    }

    @Override
    @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable CustomSetMultimap<K, V> this) {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected @PolyMutable Set<V> createCollection(@PolyMutable CustomSetMultimap<K, V> this) {
      return factory.get();
    }

    @Override
    <E extends @Nullable @Readonly Object> @Readonly Collection<E> unmodifiableCollectionSubclass(
        Collection<E> collection) {
      if (collection instanceof NavigableSet) {
        return Sets.unmodifiableNavigableSet((NavigableSet<E>) collection);
      } else if (collection instanceof SortedSet) {
        return Collections.unmodifiableSortedSet((SortedSet<E>) collection);
      } else {
        return Collections.unmodifiableSet((Set<E>) collection);
      }
    }

    @Override
    @PolyMutable Collection<V> wrapCollection(@ParametricNullness K key, @PolyMutable Collection<V> collection) {
      if (collection instanceof NavigableSet) {
        return new @PolyMutable WrappedNavigableSet(key, (NavigableSet<V>) collection, null);
      } else if (collection instanceof SortedSet) {
        return new @PolyMutable WrappedSortedSet(key, (SortedSet<V>) collection, null);
      } else {
        return new @PolyMutable WrappedSet(key, (Set<V>) collection);
      }
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends Set<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code SortedSetMultimap} that uses the provided map and factory. It can generate
   * a multimap based on arbitrary {@link Map} and {@link SortedSet} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the sets generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedSortedSetMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link TreeMultimap#create()} and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the sets returned by
   * {@code factory}. Those objects should not be manually updated and they should not use soft,
   * weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty sorted sets that will each hold all values for a given
   *     key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SortedSetMultimap<K, V> newSortedSetMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends SortedSet<V>> factory) {
    return new CustomSortedSetMultimap<>(map, factory);
  }

  @ReceiverDependentMutable
  private static class CustomSortedSetMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractSortedSetMultimap<K, V> {
    transient Supplier<? extends @ReceiverDependentMutable SortedSet<V>> factory;
    @CheckForNull transient @Mutable Comparator<? super V> valueComparator;

    CustomSortedSetMultimap(@ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map, Supplier<? extends @ReceiverDependentMutable SortedSet<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
      valueComparator = factory.get().comparator();
    }

    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable CustomSortedSetMultimap<K, V> this) {
      return createMaybeNavigableKeySet();
    }

    @Override
    @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable CustomSortedSetMultimap<K, V> this) {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected @PolyMutable SortedSet<V> createCollection(@PolyMutable CustomSortedSetMultimap<K, V> this) {
      return factory.get();
    }

    @Override
    @CheckForNull
    public Comparator<? super V> valueComparator() {
      return valueComparator;
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends SortedSet<V>>) requireNonNull(stream.readObject());
      valueComparator = factory.get().comparator();
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Copies each key-value mapping in {@code source} into {@code dest}, with its key and value
   * reversed.
   *
   * <p>If {@code source} is an {@link ImmutableMultimap}, consider using {@link
   * ImmutableMultimap#inverse} instead.
   *
   * @param source any multimap
   * @param dest the multimap to copy into; usually empty
   * @return {@code dest}
   */
  @CanIgnoreReturnValue
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object, M extends Multimap<K, V>>
      M invertFrom(Multimap<? extends V, ? extends K> source, M dest) {
    checkNotNull(dest);
    for (Map.Entry<? extends V, ? extends K> entry : source.entries()) {
      dest.put(entry.getValue(), entry.getKey());
    }
    return dest;
  }

  /**
   * Returns a synchronized (thread-safe) multimap backed by the specified multimap. In order to
   * guarantee serial access, it is critical that <b>all</b> access to the backing multimap is
   * accomplished through the returned multimap.
   *
   * <p>It is imperative that the user manually synchronize on the returned multimap when accessing
   * any of its collection views:
   *
   * <pre>{@code
   * Multimap<K, V> multimap = Multimaps.synchronizedMultimap(
   *     HashMultimap.<K, V>create());
   * ...
   * Collection<V> values = multimap.get(key);  // Needn't be in synchronized block
   * ...
   * synchronized (multimap) {  // Synchronizing on multimap, not values!
   *   Iterator<V> i = values.iterator(); // Must be in synchronized block
   *   while (i.hasNext()) {
   *     foo(i.next());
   *   }
   * }
   * }</pre>
   *
   * <p>Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and {@link
   * Multimap#replaceValues} methods return collections that aren't synchronized.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped in a synchronized view
   * @return a synchronized view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      Multimap<K, V> synchronizedMultimap(Multimap<K, V> multimap) {
    return Synchronized.multimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified multimap. Query operations on the returned
   * multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @Readonly Multimap<K, V> unmodifiableMultimap(@Readonly Multimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableMultimap || delegate instanceof ImmutableMultimap) {
      return delegate;
    }
    return new UnmodifiableMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K extends @Immutable Object, V> @Readonly Multimap<K, V> unmodifiableMultimap(ImmutableMultimap<K, @Immutable V> delegate) {
    return checkNotNull(delegate);
  }

  @Immutable
  private static class UnmodifiableMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends ForwardingMultimap<K, @Immutable V> implements Serializable {
    final @Readonly Multimap<K, V> delegate;
    @LazyInit @CheckForNull transient @Assignable @Readonly Collection<@Readonly Entry<K, V>> entries;
    @LazyInit @CheckForNull transient @Assignable @Readonly Multiset<K> keys;
    @LazyInit @CheckForNull transient @Assignable @Readonly Set<K> keySet;
    @LazyInit @CheckForNull transient @Assignable @Readonly Collection<V> values;
    @LazyInit @CheckForNull transient @Assignable @Readonly Map<K, @Readonly Collection<V>> map;

    UnmodifiableMultimap(final @Readonly Multimap<K, V> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    protected @Readonly Multimap<K, V> delegate() {
      return delegate;
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly Map<K, @Readonly Collection<V>> asMap() {
      Map<K, @Readonly Collection<V>> result = map;
      if (result == null) {
        result =
            map =
                Collections.unmodifiableMap(
                    Maps.transformValues(
                        delegate.asMap(), collection -> unmodifiableValueCollection(collection)));
      }
      return result;
    }

    @SideEffectFree
    @Override
    public @Readonly Collection<@Readonly Entry<K, V>> entries() {
      Collection<@Readonly Entry<K, V>> result = entries;
      if (result == null) {
        entries = result = unmodifiableEntries(delegate.entries());
      }
      return result;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
      delegate.forEach(checkNotNull(consumer));
    }

    @Override
    public @Readonly Collection<V> get(@ParametricNullness K key) {
      return unmodifiableValueCollection(delegate.get(key));
    }

    @Override
    public @Readonly Multiset<K> keys() {
      Multiset<K> result = keys;
      if (result == null) {
        keys = result = Multisets.unmodifiableMultiset(delegate.keys());
      }
      return result;
    }

    @SideEffectFree
    @Override
    public @Readonly Set<K> keySet() {
      Set<K> result = keySet;
      if (result == null) {
        keySet = result = Collections.unmodifiableSet(delegate.keySet());
      }
      return result;
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly Collection<V> removeAll(@CheckForNull @Readonly Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly Collection<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @SideEffectFree
    @Override
    public @Readonly Collection<V> values() {
      Collection<V> result = values;
      if (result == null) {
        values = result = Collections.unmodifiableCollection(delegate.values());
      }
      return result;
    }

    private static final long serialVersionUID = 0;
  }

  @Immutable
  private static class UnmodifiableListMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends UnmodifiableMultimap<K, V> implements ListMultimap<K, V> {
    UnmodifiableListMultimap(@Readonly ListMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public @Readonly ListMultimap<K, V> delegate() {
      return (@Readonly ListMultimap<K, V>) super.delegate();
    }

    @Override
    public @Readonly List<V> get(@ParametricNullness K key) {
      return Collections.unmodifiableList(delegate().get(key));
    }

    @Override
    public @Readonly List<V> removeAll(@CheckForNull @Readonly Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly List<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 0;
  }

  @Immutable
  private static class UnmodifiableSetMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends UnmodifiableMultimap<K, V> implements SetMultimap<K, V> {
    UnmodifiableSetMultimap(@Readonly SetMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public @Readonly SetMultimap<K, V> delegate() {
      return (@Readonly SetMultimap<K, V>) super.delegate();
    }

    @Override
    public @Readonly Set<V> get(@ParametricNullness K key) {
      /*
       * Note that this doesn't return a SortedSet when delegate is a
       * SortedSetMultiset, unlike (SortedSet<V>) super.get().
       */
      return Collections.unmodifiableSet(delegate().get(key));
    }

    @SideEffectFree
    @Override
    public @Readonly Set<Map.@Readonly Entry<K, V>> entries() {
      return Maps.unmodifiableEntrySet(delegate().entries());
    }

    @Override
    public @Readonly Set<V> removeAll(@CheckForNull @Readonly Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly Set<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 0;
  }

  @Immutable
  private static class UnmodifiableSortedSetMultimap<
          K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends UnmodifiableSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    UnmodifiableSortedSetMultimap(@Readonly SortedSetMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public @Readonly SortedSetMultimap<K, V> delegate() {
      return (@Readonly SortedSetMultimap<K, V>) super.delegate();
    }

    @Override
    public @Readonly SortedSet<V> get(@ParametricNullness K key) {
      return Collections.unmodifiableSortedSet(delegate().get(key));
    }

    @Override
    public @Readonly SortedSet<V> removeAll(@CheckForNull @Readonly Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly SortedSet<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public Comparator<? super V> valueComparator() {
      return delegate().valueComparator();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a synchronized (thread-safe) {@code SetMultimap} backed by the specified multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> synchronizedSetMultimap(SetMultimap<K, V> multimap) {
    return Synchronized.setMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code SetMultimap}. Query operations on the
   * returned multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @Readonly SetMultimap<K, V> unmodifiableSetMultimap(@Readonly SetMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableSetMultimap || delegate instanceof ImmutableSetMultimap) {
      return delegate;
    }
    return new UnmodifiableSetMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K extends @Immutable Object, V> @Readonly SetMultimap<K, V> unmodifiableSetMultimap(
      ImmutableSetMultimap<K, @Immutable V> delegate) {
    return checkNotNull(delegate);
  }

  /**
   * Returns a synchronized (thread-safe) {@code SortedSetMultimap} backed by the specified
   * multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SortedSetMultimap<K, V> synchronizedSortedSetMultimap(SortedSetMultimap<K, V> multimap) {
    return Synchronized.sortedSetMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code SortedSetMultimap}. Query operations on
   * the returned multimap "read through" to the specified multimap, and attempts to modify the
   * returned multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @Readonly SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(@Readonly SortedSetMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableSortedSetMultimap) {
      return delegate;
    }
    return new UnmodifiableSortedSetMultimap<>(delegate);
  }

  /**
   * Returns a synchronized (thread-safe) {@code ListMultimap} backed by the specified multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      ListMultimap<K, V> synchronizedListMultimap(ListMultimap<K, V> multimap) {
    return Synchronized.listMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code ListMultimap}. Query operations on the
   * returned multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @Readonly ListMultimap<K, V> unmodifiableListMultimap(@Readonly ListMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableListMultimap || delegate instanceof ImmutableListMultimap) {
      return delegate;
    }
    return new UnmodifiableListMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K extends @Immutable Object, V> ListMultimap<K, V> unmodifiableListMultimap(
      ImmutableListMultimap<K, @Immutable V> delegate) {
    return checkNotNull(delegate);
  }

  /**
   * Returns an unmodifiable view of the specified collection, preserving the interface for
   * instances of {@code SortedSet}, {@code Set}, {@code List} and {@code Collection}, in that order
   * of preference.
   *
   * @param collection the collection for which to return an unmodifiable view
   * @return an unmodifiable view of the collection
   */
  private static <V extends @Nullable @Readonly Object> @Readonly Collection<V> unmodifiableValueCollection(
          @Readonly Collection<V> collection) {
    if (collection instanceof SortedSet) {
      return Collections.unmodifiableSortedSet((SortedSet<V>) collection);
    } else if (collection instanceof Set) {
      return Collections.unmodifiableSet((Set<V>) collection);
    } else if (collection instanceof List) {
      return Collections.unmodifiableList((List<V>) collection);
    }
    return Collections.unmodifiableCollection(collection);
  }

  /**
   * Returns an unmodifiable view of the specified collection of entries. The {@link Entry#setValue}
   * operation throws an {@link UnsupportedOperationException}. If the specified collection is a
   * {@code Set}, the returned collection is also a {@code Set}.
   *
   * @param entries the entries for which to return an unmodifiable view
   * @return an unmodifiable view of the entries
   */
  private static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @Readonly Collection<@Readonly Entry<K, V>> unmodifiableEntries(@Readonly Collection<Entry<K, V>> entries) {
    if (entries instanceof Set) {
      return Maps.unmodifiableEntrySet((Set<@Readonly Entry<K, V>>) entries);
    }
    return new Maps.UnmodifiableEntries<>(Collections.unmodifiableCollection(entries));
  }

  /**
   * Returns {@link ListMultimap#asMap multimap.asMap()}, with its type corrected from {@code Map<K,
   * Collection<V>>} to {@code Map<K, List<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of ListMultimap.asMap()
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable Map<K, @PolyMutable List<V>> asMap(
          @PolyMutable ListMultimap<K, V> multimap) {
    return (@PolyMutable Map<K, @PolyMutable List<V>>) (@PolyMutable Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link SetMultimap#asMap multimap.asMap()}, with its type corrected from {@code Map<K,
   * Collection<V>>} to {@code Map<K, Set<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of SetMultimap.asMap()
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable Map<K, @PolyMutable Set<V>> asMap(
          @PolyMutable SetMultimap<K, V> multimap) {
    return (@PolyMutable Map<K, @PolyMutable Set<V>>) (@PolyMutable Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link SortedSetMultimap#asMap multimap.asMap()}, with its type corrected from {@code
   * Map<K, Collection<V>>} to {@code Map<K, SortedSet<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of SortedSetMultimap.asMap()
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> @PolyMutable Map<K, @PolyMutable SortedSet<V>> asMap(
          @PolyMutable SortedSetMultimap<K, V> multimap) {
    return (@PolyMutable Map<K, @PolyMutable SortedSet<V>>) (@PolyMutable Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link Multimap#asMap multimap.asMap()}. This is provided for parity with the other
   * more strongly-typed {@code asMap()} implementations.
   *
   * @since 15.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Map<K, Collection<V>> asMap(Multimap<K, V> multimap) {
    return multimap.asMap();
  }

  /**
   * Returns a multimap view of the specified map. The multimap is backed by the map, so changes to
   * the map are reflected in the multimap, and vice versa. If the map is modified while an
   * iteration over one of the multimap's collection views is in progress (except through the
   * iterator's own {@code remove} operation, or through the {@code setValue} operation on a map
   * entry returned by the iterator), the results of the iteration are undefined.
   *
   * <p>The multimap supports mapping removal, which removes the corresponding mapping from the map.
   * It does not support any operations which might add mappings, such as {@code put}, {@code
   * putAll} or {@code replaceValues}.
   *
   * <p>The returned multimap will be serializable if the specified map is serializable.
   *
   * @param map the backing map for the returned multimap view
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> SetMultimap<K, V> forMap(
      Map<K, V> map) {
    return new MapMultimap<>(map);
  }

  /** @see Multimaps#forMap */
  @ReceiverDependentMutable
  @CFComment("PICO: V is immutable because it is used as hashset's element")
  private static class MapMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      extends AbstractMultimap<K, V> implements SetMultimap<K, V>, Serializable {
    final Map<K, V> map;

    MapMultimap(@ReceiverDependentMutable Map<K, V> map) {
      this.map = checkNotNull(map);
    }

    @Pure
    @Override
    public int size(@Readonly MapMultimap<K, V> this) {
      return map.size();
    }

    @Pure
    @Override
    public boolean containsKey(@Readonly MapMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return map.containsKey(key);
    }

    @Pure
    @Override
    public boolean containsValue(@Readonly MapMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
      return map.containsValue(value);
    }

    @Pure
    @Override
    public boolean containsEntry(@Readonly MapMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      return map.entrySet().contains(Maps.immutableEntry(key, value));
    }

    @Override
    public @PolyMutable Set<V> get(@PolyMutable MapMultimap<K, V> this, @ParametricNullness final K key) {
      return new Sets.@PolyMutable ImprovedAbstractSet<V>() {
        @Override
        public Iterator<V> iterator() {
          return new Iterator<V>() {
            int i;

            @Override
            public boolean hasNext() {
              return (i == 0) && map.containsKey(key);
            }

            @Override
            @ParametricNullness
            public V next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              i++;
              /*
               * The cast is safe because of the containsKey check in hasNext(). (That means it's
               * unsafe under concurrent modification, but all bets are off then, anyway.)
               */
              return uncheckedCastNullableTToT(map.get(key));
            }

            @Override
            public void remove() {
              checkRemove(i == 1);
              i = -1;
              map.remove(key);
            }
          };
        }

        @Pure
        @Override
        public @NonNegative int size() {
          return map.containsKey(key) ? 1 : 0;
        }
      };
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@Readonly Multimap<? extends K, ? extends V> multimap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Readonly Set<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@Mutable MapMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      return map.entrySet().remove(Maps.immutableEntry(key, value));
    }

    @Override
    public @Readonly Set<V> removeAll(@Mutable MapMultimap<K, V> this, @CheckForNull @Readonly Object key) {
      Set<V> values = new HashSet<V>(2);
      if (!map.containsKey(key)) {
        return values;
      }
      values.add(map.remove(key));
      return values;
    }

    @Override
    public void clear(@Mutable MapMultimap<K, V> this) {
      map.clear();
    }

    @SideEffectFree
    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable MapMultimap<K, V> this) {
      return map.keySet();
    }

    @SideEffectFree
    @Override
    @PolyMutable Collection<V> createValues(@PolyMutable MapMultimap<K, V> this) {
      return map.values();
    }

    @SideEffectFree
    @Override
    public @PolyMutable Set<@PolyMutable Entry<K, V>> entries(@PolyMutable MapMultimap<K, V> this) {
      return map.entrySet();
    }

    @Override
    @PolyMutable Collection<@PolyMutable Entry<K, V>> createEntries(@PolyMutable MapMultimap<K, V> this) {
      throw new AssertionError("unreachable");
    }

    @Override
    @PolyMutable Multiset<K> createKeys(@PolyMutable MapMultimap<K, V> this) {
      return new Multimaps.Keys<K, V>(this);
    }

    @Override
    Iterator<@PolyMutable Entry<K, V>> entryIterator(@PolyMutable MapMultimap<K, V> this) {
      return map.entrySet().iterator();
    }

    @Override
    @PolyMutable Map<K, @PolyMutable Collection<V>> createAsMap(@PolyMutable MapMultimap<K, V> this) {
      return new AsMap<>(this);
    }

    @Pure
    @Override
    public int hashCode(@UnknownSignedness @Readonly MapMultimap<K, V> this) {
      return map.hashCode();
    }

    private static final long serialVersionUID = 7845222491160860175L;
  }

  /**
   * Returns a view of a multimap where each value is transformed by a function. All other
   * properties of the multimap, such as iteration order, are left intact. For example, the code:
   *
   * <pre>{@code
   * Multimap<String, Integer> multimap =
   *     ImmutableSetMultimap.of("a", 2, "b", -3, "b", -3, "a", 4, "c", 6);
   * Function<Integer, String> square = new Function<Integer, String>() {
   *     public String apply(Integer in) {
   *       return Integer.toString(in * in);
   *     }
   * };
   * Multimap<String, String> transformed =
   *     Multimaps.transformValues(multimap, square);
   *   System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[4, 16], b=[9, 9], c=[36]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys, and even null values
   * provided that the function is capable of accepting null input. The transformed multimap might
   * contain null values, if the function sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is. The {@code equals} and {@code hashCode} methods of the returned multimap are meaningless,
   * since there is not a definition of {@code equals} or {@code hashCode} for general collections,
   * and {@code get()} will return a general {@code Collection} as opposed to a {@code List} or a
   * {@code Set}.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the function will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@code Multimap.toString()}. For this to
   * perform well, {@code function} should be fast. To avoid lazy evaluation when the returned
   * multimap doesn't need to be a view, copy the returned multimap into a new multimap of your
   * choosing.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      Multimap<K, V2> transformValues(
          Multimap<K, V1> fromMultimap, final Function<? super V1, V2> function) {
    checkNotNull(function);
    EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
    return transformEntries(fromMultimap, transformer);
  }

  /**
   * Returns a view of a {@code ListMultimap} where each value is transformed by a function. All
   * other properties of the multimap, such as iteration order, are left intact. For example, the
   * code:
   *
   * <pre>{@code
   * ListMultimap<String, Integer> multimap
   *      = ImmutableListMultimap.of("a", 4, "a", 16, "b", 9);
   * Function<Integer, Double> sqrt =
   *     new Function<Integer, Double>() {
   *       public Double apply(Integer in) {
   *         return Math.sqrt((int) in);
   *       }
   *     };
   * ListMultimap<String, Double> transformed = Multimaps.transformValues(map,
   *     sqrt);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[2.0, 4.0], b=[3.0]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys, and even null values
   * provided that the function is capable of accepting null input. The transformed multimap might
   * contain null values, if the function sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the function will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@code Multimap.toString()}. For this to
   * perform well, {@code function} should be fast. To avoid lazy evaluation when the returned
   * multimap doesn't need to be a view, copy the returned multimap into a new multimap of your
   * choosing.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      ListMultimap<K, V2> transformValues(
          ListMultimap<K, V1> fromMultimap, final Function<? super V1, V2> function) {
    checkNotNull(function);
    EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
    return transformEntries(fromMultimap, transformer);
  }

  /**
   * Returns a view of a multimap whose values are derived from the original multimap's entries. In
   * contrast to {@link #transformValues}, this method's entry-transformation logic may depend on
   * the key as well as the value.
   *
   * <p>All other properties of the transformed multimap, such as iteration order, are left intact.
   * For example, the code:
   *
   * <pre>{@code
   * SetMultimap<String, Integer> multimap =
   *     ImmutableSetMultimap.of("a", 1, "a", 4, "b", -6);
   * EntryTransformer<String, Integer, String> transformer =
   *     new EntryTransformer<String, Integer, String>() {
   *       public String transformEntry(String key, Integer value) {
   *          return (value >= 0) ? key : "no" + key;
   *       }
   *     };
   * Multimap<String, String> transformed =
   *     Multimaps.transformEntries(multimap, transformer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[a, a], b=[nob]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys and null values provided
   * that the transformer is capable of accepting null inputs. The transformed multimap might
   * contain null values if the transformer sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is. The {@code equals} and {@code hashCode} methods of the returned multimap are meaningless,
   * since there is not a definition of {@code equals} or {@code hashCode} for general collections,
   * and {@code get()} will return a general {@code Collection} as opposed to a {@code List} or a
   * {@code Set}.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned multimap
   * doesn't need to be a view, copy the returned multimap into a new multimap of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed multimap.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      Multimap<K, V2> transformEntries(
          Multimap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesMultimap<>(fromMap, transformer);
  }

  /**
   * Returns a view of a {@code ListMultimap} whose values are derived from the original multimap's
   * entries. In contrast to {@link #transformValues(ListMultimap, Function)}, this method's
   * entry-transformation logic may depend on the key as well as the value.
   *
   * <p>All other properties of the transformed multimap, such as iteration order, are left intact.
   * For example, the code:
   *
   * <pre>{@code
   * Multimap<String, Integer> multimap =
   *     ImmutableMultimap.of("a", 1, "a", 4, "b", 6);
   * EntryTransformer<String, Integer, String> transformer =
   *     new EntryTransformer<String, Integer, String>() {
   *       public String transformEntry(String key, Integer value) {
   *         return key + value;
   *       }
   *     };
   * Multimap<String, String> transformed =
   *     Multimaps.transformEntries(multimap, transformer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {"a"=["a1", "a4"], "b"=["b6"]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys and null values provided
   * that the transformer is capable of accepting null inputs. The transformed multimap might
   * contain null values if the transformer sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned multimap
   * doesn't need to be a view, copy the returned multimap into a new multimap of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed multimap.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      ListMultimap<K, V2> transformEntries(
          ListMultimap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesListMultimap<>(fromMap, transformer);
  }

  @ReceiverDependentMutable
  private static class TransformedEntriesMultimap<
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      extends AbstractMultimap<K, V2> {
    final Multimap<K, V1> fromMultimap;
    final EntryTransformer<? super K, ? super V1, V2> transformer;

    TransformedEntriesMultimap(
            @ReceiverDependentMutable Multimap<K, V1> fromMultimap,
        final @ReceiverDependentMutable EntryTransformer<? super K, ? super V1, V2> transformer) {
      this.fromMultimap = checkNotNull(fromMultimap);
      this.transformer = checkNotNull(transformer);
    }

    Collection<V2> transform(@ParametricNullness K key, Collection<V1> values) {
      Function<? super V1, V2> function = Maps.asValueToValueFunction(transformer, key);
      if (values instanceof List) {
        return Lists.transform((List<V1>) values, function);
      } else {
        return Collections2.transform(values, function);
      }
    }

    @Override
    Map<K, Collection<V2>> createAsMap() {
      return Maps.transformEntries(fromMultimap.asMap(), (key, value) -> transform(key, value));
    }

    @Override
    public void clear(@Mutable TransformedEntriesMultimap<K, V1, V2> this) {
      fromMultimap.clear();
    }

    @Override
    public boolean containsKey(@Readonly TransformedEntriesMultimap<K, V1, V2> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return fromMultimap.containsKey(key);
    }

    @Override
    @PolyMutable Collection<@PolyMutable Entry<K, V2>> createEntries(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this) {
      return new Entries();
    }

    @Override
    Iterator<@PolyMutable Entry<K, V2>> entryIterator(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this) {
      return Iterators.transform(
          fromMultimap.entries().iterator(), Maps.<K, V1, V2>asEntryToEntryFunction(transformer));
    }

    @Override
    public @PolyMutable Collection<V2> get(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this, @ParametricNullness final K key) {
      return transform(key, fromMultimap.get(key));
    }

    @Override
    public boolean isEmpty(@Readonly TransformedEntriesMultimap<K, V1, V2> this) {
      return fromMultimap.isEmpty();
    }

    @Override
    @PolyMutable Set<K> createKeySet(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this) {
      return fromMultimap.keySet();
    }

    @Override
    @PolyMutable Multiset<K> createKeys(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this) {
      return fromMultimap.keys();
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V2 value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V2> multimap) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(@CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
      return get((K) key).remove(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Readonly Collection<V2> removeAll(@Mutable TransformedEntriesMultimap<K, V1, V2> this, @CheckForNull @Readonly Object key) {
      return transform((K) key, fromMultimap.removeAll(key));
    }

    @Override
    public @Readonly Collection<V2> replaceValues(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size(@Readonly TransformedEntriesMultimap<K, V1, V2> this) {
      return fromMultimap.size();
    }

    @Override
    @PolyMutable Collection<V2> createValues(@PolyMutable TransformedEntriesMultimap<K, V1, V2> this) {
      return Collections2.transform(
          fromMultimap.entries(), Maps.<K, V1, V2>asEntryToValueFunction(transformer));
    }
  }

  @ReceiverDependentMutable
  private static final class TransformedEntriesListMultimap<
          K extends @Nullable @Immutable Object, V1 extends @Nullable @Readonly Object, V2 extends @Nullable @Readonly Object>
      extends TransformedEntriesMultimap<K, V1, V2> implements ListMultimap<K, V2> {

    TransformedEntriesListMultimap(
            @ReceiverDependentMutable ListMultimap<K, V1> fromMultimap, @ReceiverDependentMutable EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMultimap, transformer);
    }

    @Override
    @PolyMutable List<V2> transform(@ParametricNullness K key, @PolyMutable Collection<V1> values) {
      return Lists.transform((List<V1>) values, Maps.asValueToValueFunction(transformer, key));
    }

    @Override
    public @PolyMutable List<V2> get(@PolyMutable TransformedEntriesListMultimap<K, V1, V2> this, @ParametricNullness K key) {
      return transform(key, fromMultimap.get(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Readonly List<V2> removeAll(@Mutable TransformedEntriesListMultimap<K, V1, V2> this, @CheckForNull @Readonly Object key) {
      return transform((K) key, fromMultimap.removeAll(key));
    }

    @Override
    public @Readonly List<V2> replaceValues(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in an {@code Iterable} of values. Each value will be stored as
   * a value in the resulting multimap, yielding a multimap with the same size as the input
   * iterable. The key used to store that value in the multimap will be the result of calling the
   * function on that value. The resulting multimap is created as an immutable snapshot. In the
   * returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   *
   * <p>For example,
   *
   * <pre>{@code
   * List<String> badGuys =
   *     Arrays.asList("Inky", "Blinky", "Pinky", "Pinky", "Clyde");
   * Function<String, Integer> stringLengthFunction = ...;
   * Multimap<Integer, String> index =
   *     Multimaps.index(badGuys, stringLengthFunction);
   * System.out.println(index);
   * }</pre>
   *
   * <p>prints
   *
   * <pre>{@code
   * {4=[Inky], 6=[Blinky], 5=[Pinky, Pinky, Clyde]}
   * }</pre>
   *
   * <p>The returned multimap is serializable if its keys and values are all serializable.
   *
   * @param values the values to use when constructing the {@code ImmutableListMultimap}
   * @param keyFunction the function used to produce the key for each value
   * @return {@code ImmutableListMultimap} mapping the result of evaluating the function {@code
   *     keyFunction} on each value in the input collection to that value
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   */
  public static <K extends @Immutable Object, V extends @Immutable Object> ImmutableListMultimap<K, V> index(
      Iterable<V> values, Function<? super V, K> keyFunction) {
    return index(values.iterator(), keyFunction);
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in an {@code Iterator} of values. Each value will be stored as
   * a value in the resulting multimap, yielding a multimap with the same size as the input
   * iterator. The key used to store that value in the multimap will be the result of calling the
   * function on that value. The resulting multimap is created as an immutable snapshot. In the
   * returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   *
   * <p>For example,
   *
   * <pre>{@code
   * List<String> badGuys =
   *     Arrays.asList("Inky", "Blinky", "Pinky", "Pinky", "Clyde");
   * Function<String, Integer> stringLengthFunction = ...;
   * Multimap<Integer, String> index =
   *     Multimaps.index(badGuys.iterator(), stringLengthFunction);
   * System.out.println(index);
   * }</pre>
   *
   * <p>prints
   *
   * <pre>{@code
   * {4=[Inky], 6=[Blinky], 5=[Pinky, Pinky, Clyde]}
   * }</pre>
   *
   * <p>The returned multimap is serializable if its keys and values are all serializable.
   *
   * @param values the values to use when constructing the {@code ImmutableListMultimap}
   * @param keyFunction the function used to produce the key for each value
   * @return {@code ImmutableListMultimap} mapping the result of evaluating the function {@code
   *     keyFunction} on each value in the input collection to that value
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   * @since 10.0
   */
  public static <K extends @Immutable Object, V> ImmutableListMultimap<K, @Immutable V> index(
      Iterator<V> values, Function<? super V, K> keyFunction) {
    checkNotNull(keyFunction);
    ImmutableListMultimap.Builder<K, @Immutable V> builder = ImmutableListMultimap.builder();
    while (values.hasNext()) {
      V value = values.next();
      checkNotNull(value, values);
      builder.put(keyFunction.apply(value), value);
    }
    return builder.build();
  }

  @ReceiverDependentMutable
  static class Keys<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractMultiset<K> {
    @Weak final Multimap<K, V> multimap;

    Keys(@ReceiverDependentMutable Multimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    Iterator<Multiset.@PolyMutable Entry<K>> entryIterator(@PolyMutable Keys<K, V> this) {
      return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(
          multimap.asMap().entrySet().iterator()) {
        @Override
        Multiset.Entry<K> transform(final Map.Entry<K, Collection<V>> backingEntry) {
          return new Multisets.AbstractEntry<K>() {
            @Override
            @ParametricNullness
            public K getElement() {
              return backingEntry.getKey();
            }

            @Override
            public int getCount() {
              return backingEntry.getValue().size();
            }
          };
        }
      };
    }

    @Override
    public Spliterator<K> spliterator(@PolyMutable Keys<K, V> this) {
      return CollectSpliterators.map(multimap.entries().spliterator(), Map.Entry::getKey);
    }

    @Override
    public void forEach(Consumer<? super K> consumer) {
      checkNotNull(consumer);
      multimap.entries().forEach(entry -> consumer.accept(entry.getKey()));
    }

    @Override
    int distinctElements(@Readonly Keys<K, V> this) {
      return multimap.asMap().size();
    }

    @Override
    public @NonNegative int size(@Readonly Keys<K, V> this) {
      return multimap.size();
    }

    @Override
    public boolean contains(@Readonly Keys<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object element) {
      return multimap.containsKey(element);
    }

    @Override
    public Iterator<K> iterator(@PolyMutable Keys<K, V> this) {
      return Maps.keyIterator(multimap.entries().iterator());
    }

    @Override
    public @NonNegative int count(@Readonly Keys<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object element) {
      Collection<V> values = Maps.safeGet(multimap.asMap(), element);
      return (values == null) ? 0 : values.size();
    }

    @Override
    public int remove(@Mutable Keys<K, V> this, @CheckForNull @Readonly Object element, int occurrences) {
      checkNonnegative(occurrences, "occurrences");
      if (occurrences == 0) {
        return count(element);
      }

      Collection<V> values = Maps.safeGet(multimap.asMap(), element);

      if (values == null) {
        return 0;
      }

      int oldCount = values.size();
      if (occurrences >= oldCount) {
        values.clear();
      } else {
        Iterator<V> iterator = values.iterator();
        for (int i = 0; i < occurrences; i++) {
          iterator.next();
          iterator.remove();
        }
      }
      return oldCount;
    }

    @Override
    public void clear(@Mutable Keys<K, V> this) {
      multimap.clear();
    }

    @Override
    public @PolyMutable Set<K> elementSet(@PolyMutable Keys<K, V> this) {
      return multimap.keySet();
    }

    @Override
    Iterator<K> elementIterator() {
      throw new AssertionError("should never be called");
    }
  }

  /** A skeleton implementation of {@link Multimap#entries()}. */
  @ReceiverDependentMutable
  abstract static class Entries<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends AbstractCollection<Map.@ReceiverDependentMutable Entry<K, V>> {
    abstract @PolyMutable Multimap<K, V> multimap(@PolyMutable Entries<K, V> this);

    @Override
    public @NonNegative int size(@Readonly Entries<K, V> this) {
      return multimap().size();
    }

    @Override
    public boolean contains(@Readonly Entries<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        return multimap().containsEntry(entry.getKey(), entry.getValue());
      }
      return false;
    }

    @Override
    public boolean remove(@Mutable Entries<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        return multimap().remove(entry.getKey(), entry.getValue());
      }
      return false;
    }

    @Override
    public void clear(@Mutable Entries<K, V> this) {
      multimap().clear();
    }
  }

  /** A skeleton implementation of {@link Multimap#asMap()}. */
  @ReceiverDependentMutable
  static final class AsMap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      extends Maps.ViewCachingAbstractMap<K, @Readonly Collection<V>> {
    @Weak private final Multimap<K, V> multimap;

    AsMap(@ReceiverDependentMutable Multimap<K, V> multimap) {
      this.multimap = checkNotNull(multimap);
    }

    @Override
    public @NonNegative int size(@Readonly AsMap<K, V> this) {
      return multimap.keySet().size();
    }

    @Override
    protected @PolyMutable Set<Entry<K, @PolyMutable Collection<V>>> createEntrySet(@PolyMutable AsMap<K, V> this) {
      return new EntrySet();
    }

    void removeValuesForKey(@CheckForNull @Readonly Object key) {
      multimap.keySet().remove(key);
    }

    @WeakOuter
    @ReceiverDependentMutable
    @CFComment("PICO: outer receiver dependency")
    class EntrySet extends Maps.EntrySet<K, @Readonly Collection<V>> {
      @Override
      @PolyMutable Map<K, @PolyMutable Collection<V>> map(@PolyMutable AsMap<K, V>.@Readonly EntrySet this) {
        return AsMap.this;
      }

      @Override
      public Iterator<Entry<K, Collection<V>>> iterator() {
        return Maps.asMapEntryIterator(multimap.keySet(), key -> multimap.get(key));
      }

      @Override
      public boolean remove(@Mutable EntrySet this, @CheckForNull @UnknownSignedness @Readonly Object o) {
        if (!contains(o)) {
          return false;
        }
        // requireNonNull is safe because of the contains check.
        Map.Entry<?, ?> entry = requireNonNull((Map.Entry<?, ?>) o);
        removeValuesForKey(entry.getKey());
        return true;
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    @CheckForNull
    public @PolyMutable Collection<V> get(@PolyMutable AsMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return containsKey(key) ? multimap.get((K) key) : null;
    }

    @Override
    @CheckForNull
    public @Readonly Collection<V> remove(@Mutable AsMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return containsKey(key) ? multimap.removeAll(key) : null;
    }

    @Override
    public @PolyMutable Set<@KeyFor({"this"}) K> keySet(@PolyMutable AsMap<K, V> this) {
      return multimap.keySet();
    }

    @Override
    public boolean isEmpty(@Readonly AsMap<K, V> this) {
      return multimap.isEmpty();
    }

    @Override
    public boolean containsKey(@Mutable AsMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      return multimap.containsKey(key);
    }

    @Override
    public void clear(@Mutable AsMap<K, V> this) {
      multimap.clear();
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> Multimap<K, V> filterKeys(
      Multimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof SetMultimap) {
      return filterKeys((SetMultimap<K, V>) unfiltered, keyPredicate);
    } else if (unfiltered instanceof ListMultimap) {
      return filterKeys((ListMultimap<K, V>) unfiltered, keyPredicate);
    } else if (unfiltered instanceof FilteredKeyMultimap) {
      FilteredKeyMultimap<K, V> prev = (FilteredKeyMultimap<K, V>) unfiltered;
      return new FilteredKeyMultimap<>(
          prev.unfiltered, Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else if (unfiltered instanceof FilteredMultimap) {
      FilteredMultimap<K, V> prev = (FilteredMultimap<K, V>) unfiltered;
      return filterFiltered(prev, Maps.<K>keyPredicateOnEntries(keyPredicate));
    } else {
      return new FilteredKeyMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> filterKeys(
          SetMultimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof FilteredKeySetMultimap) {
      FilteredKeySetMultimap<K, V> prev = (FilteredKeySetMultimap<K, V>) unfiltered;
      return new FilteredKeySetMultimap<>(
          prev.unfiltered(), Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else if (unfiltered instanceof FilteredSetMultimap) {
      FilteredSetMultimap<K, V> prev = (FilteredSetMultimap<K, V>) unfiltered;
      return filterFiltered(prev, Maps.<K>keyPredicateOnEntries(keyPredicate));
    } else {
      return new FilteredKeySetMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      ListMultimap<K, V> filterKeys(
          ListMultimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof FilteredKeyListMultimap) {
      FilteredKeyListMultimap<K, V> prev = (FilteredKeyListMultimap<K, V>) unfiltered;
      return new FilteredKeyListMultimap<>(
          prev.unfiltered(), Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else {
      return new FilteredKeyListMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a value that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose value satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      Multimap<K, V> filterValues(
          Multimap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a value that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose value satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> filterValues(
          SetMultimap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned multimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key/value pair that
   * doesn't satisfy the predicate, multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 11.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      Multimap<K, V> filterEntries(
          Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    if (unfiltered instanceof SetMultimap) {
      return filterEntries((SetMultimap<K, V>) unfiltered, entryPredicate);
    }
    return (unfiltered instanceof FilteredMultimap)
        ? filterFiltered((FilteredMultimap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryMultimap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned multimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key/value pair that
   * doesn't satisfy the predicate, multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> filterEntries(
          SetMultimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredSetMultimap)
        ? filterFiltered((FilteredSetMultimap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntrySetMultimap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Support removal operations when filtering a filtered multimap. Since a filtered multimap has
   * iterators that don't support remove, passing one to the FilteredEntryMultimap constructor would
   * lead to a multimap whose removal operations would fail. This method combines the predicates to
   * avoid that problem.
   */
  private static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      Multimap<K, V> filterFiltered(
          FilteredMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.<Entry<K, V>>and(multimap.entryPredicate(), entryPredicate);
    return new FilteredEntryMultimap<>(multimap.unfiltered(), predicate);
  }

  /**
   * Support removal operations when filtering a filtered multimap. Since a filtered multimap has
   * iterators that don't support remove, passing one to the FilteredEntryMultimap constructor would
   * lead to a multimap whose removal operations would fail. This method combines the predicates to
   * avoid that problem.
   */
  private static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
      SetMultimap<K, V> filterFiltered(
          FilteredSetMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.<Entry<K, V>>and(multimap.entryPredicate(), entryPredicate);
    return new FilteredEntrySetMultimap<>(multimap.unfiltered(), predicate);
  }

  static boolean equalsImpl(@Readonly Multimap<?, ?> multimap, @CheckForNull @UnknownSignedness @Readonly Object object) {
    if (object == multimap) {
      return true;
    }
    if (object instanceof Multimap) {
      Multimap<?, ?> that = (Multimap<?, ?>) object;
      return multimap.asMap().equals(that.asMap());
    }
    return false;
  }

  // TODO(jlevy): Create methods that filter a SortedSetMultimap.
}
