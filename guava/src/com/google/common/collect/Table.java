/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.annotations.DoNotMock;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A collection that associates an ordered pair of keys, called a row key and a column key, with a
 * single value. A table may be sparse, with only a small fraction of row key / column key pairs
 * possessing a corresponding value.
 *
 * <p>The mappings corresponding to a given row key may be viewed as a {@link Map} whose keys are
 * the columns. The reverse is also available, associating a column with a row key / value map. Note
 * that, in some implementations, data access by column key may have fewer supported operations or
 * worse performance than data access by row key.
 *
 * <p>The methods returning collections or maps always return views of the underlying table.
 * Updating the table can change the contents of those collections, and updating the collections
 * will change the table.
 *
 * <p>All methods that modify the table are optional, and the views returned by the table may or may
 * not be modifiable. When modification isn't supported, those methods will throw an {@link
 * UnsupportedOperationException}.
 *
 * <h3>Implementations</h3>
 *
 * <ul>
 *   <li>{@link ImmutableTable}
 *   <li>{@link HashBasedTable}
 *   <li>{@link TreeBasedTable}
 *   <li>{@link ArrayTable}
 *   <li>{@link Tables#newCustomTable Tables.newCustomTable}
 * </ul>
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#table">{@code Table}</a>.
 *
 * @author Jared Levy
 * @param <R> the type of the table row keys
 * @param <C> the type of the table column keys
 * @param <V> the type of the mapped values
 * @since 7.0
 */
@DoNotMock("Use ImmutableTable, HashBasedTable, or another implementation")
@GwtCompatible
@ElementTypesAreNonnullByDefault
@AnnotatedFor("mutability")
@ReceiverDependentMutable
public interface Table<
    R extends @Nullable @Immutable Object, C extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> {
  // TODO(jlevy): Consider adding methods similar to ConcurrentMap methods.

  // Accessors

  /**
   * Returns {@code true} if the table contains a mapping with the specified row and column keys.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  boolean contains(
          @Readonly Table<R, C, V> this,
      @CompatibleWith("R") @CheckForNull @Readonly Object rowKey,
      @CompatibleWith("C") @CheckForNull @Readonly Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified row key.
   *
   * @param rowKey key of row to search for
   */
  boolean containsRow(@Readonly Table<R, C, V> this, @CompatibleWith("R") @CheckForNull @Readonly Object rowKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified column.
   *
   * @param columnKey key of column to search for
   */
  boolean containsColumn(@Readonly Table<R, C, V> this, @CompatibleWith("C") @CheckForNull @Readonly Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified value.
   *
   * @param value value to search for
   */
  boolean containsValue(@Readonly Table<R, C, V> this, @CompatibleWith("V") @CheckForNull @UnknownSignedness @Readonly Object value);

  /**
   * Returns the value corresponding to the given row and column keys, or {@code null} if no such
   * mapping exists.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  @CheckForNull
  V get(
          @Readonly Table<R, C, V> this,
      @CompatibleWith("R") @CheckForNull @Readonly Object rowKey,
      @CompatibleWith("C") @CheckForNull @Readonly Object columnKey);

  /** Returns {@code true} if the table contains no mappings. */
  boolean isEmpty(@Readonly Table<R, C, V> this);

  /** Returns the number of row key / column key / value mappings in the table. */
  int size(@Readonly Table<R, C, V> this);

  /**
   * Compares the specified object with this table for equality. Two tables are equal when their
   * cell views, as returned by {@link #cellSet}, are equal.
   */
  @Override
  boolean equals(@Readonly Table<R, C, V> this, @CheckForNull @Readonly Object obj);

  /**
   * Returns the hash code for this table. The hash code of a table is defined as the hash code of
   * its cell view, as returned by {@link #cellSet}.
   */
  @Override
  int hashCode(@UnknownSignedness @Readonly Table<R, C, V> this);

  // Mutators

  /** Removes all mappings from the table. */
  void clear(@Mutable Table<R, C, V> this);

  /**
   * Associates the specified value with the specified keys. If the table already contained a
   * mapping for those keys, the old value is replaced with the specified value.
   *
   * @param rowKey row key that the value should be associated with
   * @param columnKey column key that the value should be associated with
   * @param value value to be associated with the specified keys
   * @return the value previously associated with the keys, or {@code null} if no mapping existed
   *     for the keys
   */
  @CanIgnoreReturnValue
  @CheckForNull
  V put(@Mutable Table<R, C, V> this, @ParametricNullness R rowKey, @ParametricNullness C columnKey, @ParametricNullness V value);

  /**
   * Copies all mappings from the specified table to this table. The effect is equivalent to calling
   * {@link #put} with each row key / column key / value mapping in {@code table}.
   *
   * @param table the table to add to this table
   */
  void putAll(@Mutable Table<R, C, V> this, @Readonly Table<? extends R, ? extends C, ? extends V> table);

  /**
   * Removes the mapping, if any, associated with the given keys.
   *
   * @param rowKey row key of mapping to be removed
   * @param columnKey column key of mapping to be removed
   * @return the value previously associated with the keys, or {@code null} if no such value existed
   */
  @CanIgnoreReturnValue
  @CheckForNull
  V remove(@Mutable Table<R, C, V> this,
      @CompatibleWith("R") @CheckForNull @Readonly Object rowKey,
      @CompatibleWith("C") @CheckForNull @Readonly Object columnKey);

  // Views

  /**
   * Returns a view of all mappings that have the given row key. For each row key / column key /
   * value mapping in the table with that row key, the returned map associates the column key with
   * the value. If no mappings in the table have the provided row key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice versa.
   *
   * @param rowKey key of row to search for in the table
   * @return the corresponding map from column keys to values
   */
  @PolyMutable Map<C, V> row(@PolyMutable Table<R, C, V> this, @ParametricNullness R rowKey);

  /**
   * Returns a view of all mappings that have the given column key. For each row key / column key /
   * value mapping in the table with that column key, the returned map associates the row key with
   * the value. If no mappings in the table have the provided column key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice versa.
   *
   * @param columnKey key of column to search for in the table
   * @return the corresponding map from row keys to values
   */
  @PolyMutable Map<R, V> column(@PolyMutable Table<R, C, V> this, @ParametricNullness C columnKey);

  /**
   * Returns a set of all row key / column key / value triplets. Changes to the returned set will
   * update the underlying table, and vice versa. The cell set does not support the {@code add} or
   * {@code addAll} methods.
   *
   * @return set of table cells consisting of row key / column key / value triplets
   */
  @PolyMutable Set<Cell<R, C, V>> cellSet(@PolyMutable Table<R, C, V> this);

  /**
   * Returns a set of row keys that have one or more values in the table. Changes to the set will
   * update the underlying table, and vice versa.
   *
   * @return set of row keys
   */
  @PolyMutable Set<R> rowKeySet(@PolyMutable Table<R, C, V> this);

  /**
   * Returns a set of column keys that have one or more values in the table. Changes to the set will
   * update the underlying table, and vice versa.
   *
   * @return set of column keys
   */
  @PolyMutable Set<C> columnKeySet(@PolyMutable Table<R, C, V> this);

  /**
   * Returns a collection of all values, which may contain duplicates. Changes to the returned
   * collection will update the underlying table, and vice versa.
   *
   * @return collection of values
   */
  @PolyMutable Collection<V> values(@PolyMutable Table<R, C, V> this);

  /**
   * Returns a view that associates each row key with the corresponding map from column keys to
   * values. Changes to the returned map will update this table. The returned map does not support
   * {@code put()} or {@code putAll()}, or {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code rowMap().get()} have the same behavior as those
   * returned by {@link #row}. Those maps may support {@code setValue()}, {@code put()}, and {@code
   * putAll()}.
   *
   * @return a map view from each row key to a secondary map from column keys to values
   */
  @PolyMutable Map<R, Map<C, V>> rowMap(@PolyMutable Table<R, C, V> this);

  /**
   * Returns a view that associates each column key with the corresponding map from row keys to
   * values. Changes to the returned map will update this table. The returned map does not support
   * {@code put()} or {@code putAll()}, or {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code columnMap().get()} have the same behavior as those
   * returned by {@link #column}. Those maps may support {@code setValue()}, {@code put()}, and
   * {@code putAll()}.
   *
   * @return a map view from each column key to a secondary map from row keys to values
   */
  @PolyMutable Map<C, Map<R, V>> columnMap(@PolyMutable Table<R, C, V> this);

  /**
   * Row key / column key / value triplet corresponding to a mapping in a table.
   *
   * @since 7.0
   */
  @ReceiverDependentMutable
  interface Cell<
      R extends @Nullable @Immutable Object, C extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object> {
    /** Returns the row key of this cell. */
    @ParametricNullness
    R getRowKey(@Readonly Cell<R, C, V> this);

    /** Returns the column key of this cell. */
    @ParametricNullness
    C getColumnKey(@Readonly Cell<R, C, V> this);

    /** Returns the value of this cell. */
    @ParametricNullness
    V getValue(@Readonly Cell<R, C, V> this);

    /**
     * Compares the specified object with this cell for equality. Two cells are equal when they have
     * equal row keys, column keys, and values.
     */
    @Override
    boolean equals(@Readonly Cell<R, C, V> this, @CheckForNull @Readonly Object obj);

    /**
     * Returns the hash code of this cell.
     *
     * <p>The hash code of a table cell is equal to {@link Objects#hashCode}{@code (e.getRowKey(),
     * e.getColumnKey(), e.getValue())}.
     */
    @Override
    int hashCode(@UnknownSignedness @Readonly Cell<R, C, V> this);
  }
}
