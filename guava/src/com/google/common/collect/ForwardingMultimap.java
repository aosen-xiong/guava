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

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A multimap which forwards all its method calls to another multimap. Subclasses should override
 * one or more methods to modify the behavior of the backing multimap as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingMultimap}.
 *
 * @author Robert Konigsberg
 * @since 2.0
 */
@AnnotatedFor({"nullness", "mutability"})
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
public abstract class ForwardingMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Readonly Object>
    extends ForwardingObject implements Multimap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingMultimap() {}

  @Override
  protected abstract @PolyMutable Multimap<K, V> delegate(@PolyMutable ForwardingMultimap<K, V> this);

  @Override
  public @PolyMutable Map<K, @PolyMutable Collection<V>> asMap(@PolyMutable ForwardingMultimap<K, V> this) {
    return delegate().asMap();
  }

  @Override
  public void clear(@Mutable ForwardingMultimap<K, V> this) {
    delegate().clear();
  }

  @Pure
  @Override
  public boolean containsEntry(@Readonly ForwardingMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
    return delegate().containsEntry(key, value);
  }

  @Pure
  @Override
  public boolean containsKey(@Readonly ForwardingMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
    return delegate().containsKey(key);
  }

  @Pure
  @Override
  public boolean containsValue(@Readonly ForwardingMultimap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
    return delegate().containsValue(value);
  }

  @SideEffectFree
  @Override
  public @PolyMutable Collection<@PolyMutable Entry<K, V>> entries(@PolyMutable ForwardingMultimap<K, V> this) {
    return delegate().entries();
  }

  @Override
  public @PolyMutable Collection<V> get(@PolyMutable ForwardingMultimap<K, V> this, @ParametricNullness K key) {
    return delegate().get(key);
  }

  @Pure
  @Override
  public boolean isEmpty(@Readonly ForwardingMultimap<K, V> this) {
    return delegate().isEmpty();
  }

  @Override
  public @PolyMutable Multiset<K> keys(@PolyMutable ForwardingMultimap<K, V> this) {
    return delegate().keys();
  }

  @SideEffectFree
  @Override
  public @PolyMutable Set<K> keySet(@PolyMutable ForwardingMultimap<K, V> this) {
    return delegate().keySet();
  }

  @CanIgnoreReturnValue
  @Override
  public boolean put(@Mutable ForwardingMultimap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
    return delegate().put(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(@Mutable ForwardingMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    return delegate().putAll(key, values);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean putAll(@Mutable ForwardingMultimap<K, V> this, @Readonly Multimap<? extends K, ? extends V> multimap) {
    return delegate().putAll(multimap);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@Mutable ForwardingMultimap<K, V> this, @CheckForNull @Readonly Object key, @CheckForNull @Readonly Object value) {
    return delegate().remove(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  public @Readonly Collection<V> removeAll(@Mutable ForwardingMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    return delegate().removeAll(key);
  }

  @CanIgnoreReturnValue
  @Override
  public @Readonly Collection<V> replaceValues(@Mutable ForwardingMultimap<K, V> this, @ParametricNullness K key, Iterable<? extends V> values) {
    return delegate().replaceValues(key, values);
  }

  @Pure
  @Override
  public int size(@Readonly ForwardingMultimap<K, V> this) {
    return delegate().size();
  }

  @SideEffectFree
  @Override
  public @PolyMutable Collection<V> values(@PolyMutable ForwardingMultimap<K, V> this) {
    return delegate().values();
  }

  @Pure
  @Override
  public boolean equals(@Readonly ForwardingMultimap<K, V> this, @CheckForNull @Readonly Object object) {
    return object == this || delegate().equals(object);
  }

  @Pure
  @Override
  public int hashCode(@UnknownSignedness @Readonly ForwardingMultimap<K, V> this) {
    return delegate().hashCode();
  }
}
