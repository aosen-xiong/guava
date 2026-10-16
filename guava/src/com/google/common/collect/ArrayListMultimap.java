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

import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Implementation of {@code Multimap} that uses an {@code ArrayList} to store the values for a given
 * key. A {@link HashMap} associates each key with an {@link ArrayList} of values.
 *
 * <p>When iterating through the collections supplied by this class, the ordering of values for a
 * given key agrees with the order in which the values were added.
 *
 * <p>This multimap allows duplicate key-value pairs. After adding a new key-value pair equal to an
 * existing key-value pair, the {@code ArrayListMultimap} will contain entries for both the new
 * value and the old value.
 *
 * <p>Keys and values may be null. All optional multimap methods are supported, and all returned
 * views are modifiable.
 *
 * <p>The lists returned by {@link #get}, {@link #removeAll}, and {@link #replaceValues} all
 * implement {@link java.util.RandomAccess}.
 *
 * <p>This class is not threadsafe when any concurrent operations update the multimap. Concurrent
 * read operations will work correctly. To allow concurrent update operations, wrap your multimap
 * with a call to {@link Multimaps#synchronizedListMultimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap">{@code Multimap}</a>.
 *
 * @author Jared Levy
 * @since 2.0
 */
@AnnotatedFor({"nullness", "mutability"})
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
public final class ArrayListMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends ArrayListMultimapGwtSerializationDependencies<K, V> {
  // Default from ArrayList
  private static final int DEFAULT_VALUES_PER_KEY = 3;

  @VisibleForTesting transient int expectedValuesPerKey;

  /**
   * Creates a new, empty {@code ArrayListMultimap} with the default initial capacities.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys().arrayListValues().build()}.
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> ArrayListMultimap<K, V> create() {
    return new ArrayListMultimap<>();
  }

  /**
   * Constructs an empty {@code ArrayListMultimap} with enough capacity to hold the specified
   * numbers of keys and values without resizing.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys(expectedKeys).arrayListValues(expectedValuesPerKey).build()}.
   *
   * @param expectedKeys the expected number of distinct keys
   * @param expectedValuesPerKey the expected average number of values per key
   * @throws IllegalArgumentException if {@code expectedKeys} or {@code expectedValuesPerKey} is
   *     negative
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
  ArrayListMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
    return new ArrayListMultimap<>(expectedKeys, expectedValuesPerKey);
  }

  /**
   * Constructs an {@code ArrayListMultimap} with the same mappings as the specified multimap.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys().arrayListValues().build(multimap)}.
   *
   * @param multimap the multimap whose contents are copied to this multimap
   */
  public static <K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    @PolyMutable ArrayListMultimap<K, V> create(@PolyMutable Multimap<? extends K, ? extends V> multimap) {
    return new @PolyMutable ArrayListMultimap<>(multimap);
  }

  private ArrayListMultimap() {
    this(12, DEFAULT_VALUES_PER_KEY);
  }

  private ArrayListMultimap(int expectedKeys, int expectedValuesPerKey) {
    super(Platform.<K, @ReceiverDependentMutable Collection<V>>newHashMapWithExpectedSize(expectedKeys));
    checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
    this.expectedValuesPerKey = expectedValuesPerKey;
  }

  private ArrayListMultimap(@ReceiverDependentMutable Multimap<? extends K, ? extends V> multimap) {
    this(
        multimap.keySet().size(),
        (multimap instanceof ArrayListMultimap)
            ? ((ArrayListMultimap<?, ?>) multimap).expectedValuesPerKey
            : DEFAULT_VALUES_PER_KEY);
    putAll(multimap);
  }

  /**
   * Creates a new, empty {@code ArrayList} to hold the collection of values for an arbitrary key.
   */
  @Override
  @PolyMutable List<V> createCollection(@PolyMutable ArrayListMultimap<K, V> this) {
    return new @PolyMutable ArrayList<V>(expectedValuesPerKey);
  }

  /**
   * Reduces the memory used by this {@code ArrayListMultimap}, if feasible.
   *
   * @deprecated For a {@link ListMultimap} that automatically trims to size, use {@link
   *     ImmutableListMultimap}. If you need a mutable collection, remove the {@code trimToSize}
   *     call, or switch to a {@code HashMap<K, ArrayList<V>>}.
   */
  @Deprecated
  public void trimToSize(@Mutable ArrayListMultimap<K, V> this) {
    for (Collection<V> collection : backingMap().values()) {
      ArrayList<V> arrayList = (ArrayList<V>) collection;
      arrayList.trimToSize();
    }
  }

  /**
   * @serialData expectedValuesPerKey, number of distinct keys, and then for each distinct key: the
   *     key, number of values for that key, and the key's values
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  @J2ktIncompatible
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Serialization.writeMultimap(this, stream);
  }

  @GwtIncompatible // java.io.ObjectOutputStream
  @J2ktIncompatible
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;
    int distinctKeys = Serialization.readCount(stream);
    Map<K, Collection<V>> map = Maps.newHashMap();
    setMap(map);
    Serialization.populateMultimap(this, stream, distinctKeys);
  }

  @GwtIncompatible // Not needed in emulated source.
  @J2ktIncompatible
  private static final long serialVersionUID = 0;

@Override
public boolean containsEntry(@Readonly ArrayListMultimap<K, V> this, @Nullable @Readonly Object arg0, @Nullable @Readonly Object arg1) { return super.containsEntry(arg0, arg1); }

@Override
public boolean containsKey(@Readonly ArrayListMultimap<K, V> this, @Nullable @UnknownSignedness @Readonly Object arg0) { return super.containsKey(arg0); }

@Override
public boolean containsValue(@Readonly ArrayListMultimap<K, V> this, @Nullable @UnknownSignedness @Readonly Object arg0) { return super.containsValue(arg0); }

@Override
public boolean equals(@Readonly ArrayListMultimap<K, V> this, @Nullable @Readonly Object arg0) { return super.equals(arg0); }

@Pure
@Override
public boolean isEmpty(@Readonly ArrayListMultimap<K, V> this) { return super.isEmpty(); }

@Override
public @PolyMutable List<V> get(@PolyMutable ArrayListMultimap<K, V> this, @Nullable K arg0) { return super.get(arg0); }

@Override
public boolean remove(@Mutable ArrayListMultimap<K, V> this, @Nullable @Readonly Object arg0, @Nullable @Readonly Object arg1) { return super.remove(arg0, arg1); }

@Override
public @Readonly List<V> removeAll(@Mutable ArrayListMultimap<K, V> this, @Nullable @Readonly Object arg0) { return super.removeAll(arg0); }

@Pure
@Override
public int size(@Readonly ArrayListMultimap<K, V> this) { return super.size(); }
}
