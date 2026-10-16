/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.CFComment;

/**
 * Skeletal, implementation-agnostic implementation of the {@link Table} interface.
 *
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractTable<
        R extends @Nullable @Immutable Object, C extends @Nullable @Immutable Object, V extends @Readonly @Nullable Object>
    implements Table<R, C, V> {

  @Override
  public boolean containsRow(@Readonly AbstractTable<R, C, V> this, @CheckForNull @Readonly Object rowKey) {
    return Maps.safeContainsKey(rowMap(), rowKey);
  }

  @Override
  public boolean containsColumn(@Readonly AbstractTable<R, C, V> this, @CheckForNull @Readonly Object columnKey) {
    return Maps.safeContainsKey(columnMap(), columnKey);
  }

  @Override
  public @PolyMutable Set<R> rowKeySet(@PolyMutable AbstractTable<R, C, V> this) {
    return rowMap().keySet();
  }

  @Override
  public @PolyMutable Set<C> columnKeySet(@PolyMutable AbstractTable<R, C, V> this) {
    return columnMap().keySet();
  }

  @Override
  public boolean containsValue(@Readonly AbstractTable<R, C, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
    for (Map<C, V> row : rowMap().values()) {
      if (row.containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(@Readonly AbstractTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
    Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
    return row != null && Maps.safeContainsKey(row, columnKey);
  }

  @Override
  @CheckForNull
  public V get(@Readonly AbstractTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
    Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
    return (row == null) ? null : Maps.safeGet(row, columnKey);
  }

  @Override
  public boolean isEmpty(@Readonly AbstractTable<R, C, V> this) {
    return size() == 0;
  }

  @Override
  public void clear(@Mutable AbstractTable<R, C, V> this) {
    Iterators.clear(cellSet().iterator());
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V remove(@Mutable AbstractTable<R, C, V> this, @CheckForNull @Readonly Object rowKey, @CheckForNull @Readonly Object columnKey) {
    Map<C, V> row = Maps.safeGet(rowMap(), rowKey);
    return (row == null) ? null : Maps.safeRemove(row, columnKey);
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V put(@Mutable AbstractTable<R, C, V> this,
      @ParametricNullness R rowKey, @ParametricNullness C columnKey, @ParametricNullness V value) {
    return row(rowKey).put(columnKey, value);
  }

  @Override
  public void putAll(@Mutable AbstractTable<R, C, V> this, Table<? extends R, ? extends C, ? extends V> table) {
    for (Table.Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
      put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }
  }
  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Set<Cell<R, C, V>> cellSet;

  @Override
  public @PolyMutable Set<Cell<R, C, V>> cellSet(@PolyMutable AbstractTable<R, C, V> this) {
    Set<Cell<R, C, V>> result = cellSet;
    return (result == null) ? cellSet = createCellSet() : result;
  }

  @PolyMutable Set<Cell<R, C, V>> createCellSet(@PolyMutable AbstractTable<R, C, V> this) {
    return new @PolyMutable CellSet();
  }

  abstract Iterator<Table.Cell<R, C, V>> cellIterator(@Readonly AbstractTable<R, C, V> this);

  abstract Spliterator<Table.Cell<R, C, V>> cellSpliterator(@Readonly AbstractTable<R, C, V> this);

  @WeakOuter
  @ReceiverDependentMutable 
  class CellSet extends AbstractSet<Cell<R, C, V>> {
    @Override
    public boolean contains(@Readonly CellSet this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
        Map<C, V> row = Maps.safeGet(rowMap(), cell.getRowKey());
        return row != null
            && Collections2.safeContains(
                row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
      }
      return false;
    }

    @Override
    public boolean remove(@Mutable CellSet this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      if (o instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
        Map<C, V> row = Maps.safeGet(rowMap(), cell.getRowKey());
        return row != null
            && Collections2.safeRemove(
                row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
      }
      return false;
    }

    @Override
    public void clear(@Mutable CellSet this) {
      AbstractTable.this.clear();
    }

    @Override
    public Iterator<Table.Cell<R, C, V>> iterator() {
      return cellIterator();
    }

    @Override
    public Spliterator<Cell<R, C, V>> spliterator() {
      return cellSpliterator();
    }

    @Override
    public @NonNegative int size(@Readonly CellSet this) {
      return AbstractTable.this.size();
    }
  }

  @CFComment("Change to @LazyFinal later")
  @LazyInit @CheckForNull private transient @Assignable Collection<V> values;

  @Override
  public @PolyMutable Collection<V> values(@PolyMutable AbstractTable<R, C, V> this) {
    Collection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }

  @PolyMutable Collection<V> createValues(@PolyMutable AbstractTable<R, C, V> this) {
    return new @PolyMutable Values();
  }

  Iterator<V> valuesIterator(@Readonly AbstractTable<R, C, V> this) {
    return new TransformedIterator<Cell<R, C, V>, V>(cellSet().iterator()) {
      @Override
      @ParametricNullness
      V transform(Cell<R, C, V> cell) {
        return cell.getValue();
      }
    };
  }

  Spliterator<V> valuesSpliterator(@Readonly AbstractTable<R, C, V> this) {
    return CollectSpliterators.map(cellSpliterator(), Table.Cell::getValue);
  }

  @WeakOuter
  @ReceiverDependentMutable
  class Values extends AbstractCollection<V> {
    @Override
    public Iterator<V> iterator(@Readonly Values this) {
      return valuesIterator();
    }

    @Override
    public Spliterator<V> spliterator(@Readonly Values this) {
      return valuesSpliterator();
    }

    @Override
    public boolean contains(@Readonly Values this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      return containsValue(o);
    }

    @Override
    public void clear(@Mutable Values this) {
      AbstractTable.this.clear();
    }

    @Override
    public @NonNegative int size(@Readonly Values this) {
      return AbstractTable.this.size();
    }
  }

  @Override
  public boolean equals(@Readonly AbstractTable<R, C, V> this, @CheckForNull @Readonly Object obj) {
    return Tables.equalsImpl(this, obj);
  }

  @Override
  public int hashCode(@UnknownSignedness @Readonly AbstractTable<R, C, V> this) {
    return cellSet().hashCode();
  }

  /** Returns the string representation {@code rowMap().toString()}. */
  @Override
  public String toString(@Readonly AbstractTable<R, C, V> this) {
    return rowMap().toString();
  }
}
