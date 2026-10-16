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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;

/**
 * {@code entrySet()} implementation for {@link ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
@Immutable
abstract class ImmutableMapEntrySet<K extends @Immutable Object, V> extends ImmutableSet.CachingAsList<Entry<K, V>> {
  @Immutable
  static final class RegularEntrySet<K extends @Immutable Object, V> extends ImmutableMapEntrySet<K, V> {
    private final transient ImmutableMap<K, V> map;
    private final transient ImmutableList<@Immutable Entry<K, V>> entries;

    RegularEntrySet(ImmutableMap<K, V> map, @Immutable Entry<K, V>[] entries) {
      this(map, ImmutableList.<Entry<K, V>>asImmutableList(entries));
    }

    RegularEntrySet(ImmutableMap<K, V> map, ImmutableList<@Immutable Entry<K, V>> entries) {
      this.map = map;
      this.entries = entries;
    }

    @Override
    ImmutableMap<K, V> map() {
      return map;
    }

    @Override
    @GwtIncompatible("not used in GWT")
    int copyIntoArray(@Nullable Object[] dst, int offset) {
      return entries.copyIntoArray(dst, offset);
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return entries.iterator();
    }

    @Override
    public Spliterator<Entry<K, V>> spliterator() {
      return entries.spliterator();
    }

    @Override
    public void forEach(Consumer<? super Entry<K, V>> action) {
      entries.forEach(action);
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new RegularImmutableAsList<>(this, entries);
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  ImmutableMapEntrySet() {}

  abstract ImmutableMap<K, V> map();

  @Override
  public @NonNegative int size() {
    return map().size();
  }

  @Override
  public boolean contains(@CheckForNull @UnknownSignedness @Readonly Object object) {
    if (object instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) object;
      V value = map().get(entry.getKey());
      return value != null && value.equals(entry.getValue());
    }
    return false;
  }

  @Override
  boolean isPartialView() {
    return map().isPartialView();
  }

  @Override
  @GwtIncompatible // not used in GWT
  boolean isHashCodeFast() {
    return map().isHashCodeFast();
  }

  @Override
  public int hashCode(@UnknownSignedness ImmutableMapEntrySet<K, V> this) {
    return map().hashCode();
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  @Override
  Object writeReplace() {
    return new EntrySetSerializedForm<>(map());
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use EntrySetSerializedForm");
  }

  @GwtIncompatible // serialization
  @J2ktIncompatible
  private static class EntrySetSerializedForm<K, V> implements Serializable {
    final ImmutableMap<K, V> map;

    EntrySetSerializedForm(ImmutableMap<K, V> map) {
      this.map = map;
    }

    @Immutable Object readResolve() {
      return map.entrySet();
    }

    private static final long serialVersionUID = 0;
  }
}
