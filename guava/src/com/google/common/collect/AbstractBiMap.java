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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.KeyFor;
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
import org.checkerframework.framework.qual.CFComment;

/**
 * A general-purpose bimap implementation using any two backing {@code Map} instances.
 *
 * <p>Note that this class contains {@code equals()} calls that keep it from supporting {@code
 * IdentityHashMap} backing maps.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 */
@AnnotatedFor({"nullness", "mutability"})
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
abstract class AbstractBiMap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
    extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {

  @SuppressWarnings("nullness:initialization.field.uninitialized") // For J2KT (lateinit)
  private transient Map<K, V> delegate;

  @SuppressWarnings("nullness:initialization.field.uninitialized") // For J2KT (lateinit)
  @RetainedWith
  transient AbstractBiMap<V, K> inverse;

  /** Package-private constructor for creating a map-backed bimap. */
  AbstractBiMap(@ReceiverDependentMutable Map<K, V> forward, @ReceiverDependentMutable Map<V, K> backward) {
    setDelegates(forward, backward);
  }

  /** Private constructor for inverse bimap. */
  private AbstractBiMap(@ReceiverDependentMutable Map<K, V> backward, @ReceiverDependentMutable AbstractBiMap<V, K> forward) {
    delegate = backward;
    inverse = forward;
  }

  @Override
  protected @PolyMutable Map<K, V> delegate(@PolyMutable AbstractBiMap<K,V> this) {
    return delegate;
  }

  /** Returns its input, or throws an exception if this is not a valid key. */
  @CanIgnoreReturnValue
  @ParametricNullness
  K checkKey(@Readonly AbstractBiMap<K,V> this,  @ParametricNullness K key) {
    return key;
  }

  /** Returns its input, or throws an exception if this is not a valid value. */
  @CanIgnoreReturnValue
  @ParametricNullness
  V checkValue(@Readonly AbstractBiMap<K,V> this, @ParametricNullness V value) {
    return value;
  }

  /**
   * Specifies the delegate maps going in each direction. Called by the constructor and by
   * subclasses during deserialization.
   */
  void setDelegates(@UnderInitialization AbstractBiMap<K, V> this, @ReceiverDependentMutable Map<K, V> forward, @ReceiverDependentMutable Map<V, K> backward) {
    checkState(delegate == null);
    checkState(inverse == null);
    checkArgument(forward.isEmpty());
    checkArgument(backward.isEmpty());
    checkArgument(forward != backward);
    delegate = forward;
    inverse = makeInverse(backward);
  }

  @ReceiverDependentMutable AbstractBiMap<V, K> makeInverse(@ReceiverDependentMutable Map<V, K> backward) {
    return new @ReceiverDependentMutable Inverse<>(backward, this);
  }

  void setInverse(@UnderInitialization AbstractBiMap<K, V> this, @ReceiverDependentMutable AbstractBiMap<V, K> inverse) {
    this.inverse = inverse;
  }

  // Query Operations (optimizations)

  @Pure
  @Override
  public boolean containsValue(@Readonly AbstractBiMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object value) {
    return inverse.containsKey(value);
  }

  // Modification Operations

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V put(@Mutable AbstractBiMap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
    return putInBothMaps(key, value, false);
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V forcePut(@Mutable AbstractBiMap<K, V> this, @ParametricNullness K key, @ParametricNullness V value) {
    return putInBothMaps(key, value, true);
  }

  @CheckForNull
  private V putInBothMaps(@Mutable AbstractBiMap<K, V> this, @ParametricNullness K key, @ParametricNullness V value, boolean force) {
    checkKey(key);
    checkValue(value);
    boolean containedKey = containsKey(key);
    if (containedKey && Objects.equal(value, get(key))) {
      return value;
    }
    if (force) {
      inverse().remove(value);
    } else {
      checkArgument(!containsValue(value), "value already present: %s", value);
    }
    V oldValue = delegate.put(key, value);
    updateInverseMap(key, containedKey, oldValue, value);
    return oldValue;
  }

  private void updateInverseMap(
          @Mutable AbstractBiMap<K, V> this,
      @ParametricNullness K key,
      boolean containedKey,
      @CheckForNull V oldValue,
      @ParametricNullness V newValue) {
    if (containedKey) {
      // The cast is safe because of the containedKey check.
      removeFromInverseMap(uncheckedCastNullableTToT(oldValue));
    }
    inverse.delegate.put(newValue, key);
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V remove(@Mutable AbstractBiMap<K, V> this, @CheckForNull @UnknownSignedness @Readonly Object key) {
    return containsKey(key) ? removeFromBothMaps(key) : null;
  }

  @CanIgnoreReturnValue
  @ParametricNullness
  private V removeFromBothMaps(@Mutable AbstractBiMap<K, V> this, @CheckForNull @Readonly Object key) {
    // The cast is safe because the callers of this method first check that the key is present.
    V oldValue = uncheckedCastNullableTToT(delegate.remove(key));
    removeFromInverseMap(oldValue);
    return oldValue;
  }

  private void removeFromInverseMap(@Mutable AbstractBiMap<K, V> this, @ParametricNullness V oldValue) {
    inverse.delegate.remove(oldValue);
  }

  // Bulk Operations

  @Override
  public void putAll(@Mutable AbstractBiMap<K, V> this, @Readonly Map<? extends K, ? extends V> map) {
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void replaceAll(@Mutable AbstractBiMap<K, V> this, BiFunction<? super K, ? super V, ? extends V> function) {
    this.delegate.replaceAll(function);
    inverse.delegate.clear();
    Entry<K, V> broken = null;
    Iterator<Entry<K, V>> itr = this.delegate.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<K, V> entry = itr.next();
      K k = entry.getKey();
      V v = entry.getValue();
      K conflict = inverse.delegate.putIfAbsent(v, k);
      if (conflict != null) {
        broken = entry;
        // We're definitely going to throw, but we'll try to keep the BiMap in an internally
        // consistent state by removing the bad entry.
        itr.remove();
      }
    }
    if (broken != null) {
      throw new IllegalArgumentException("value already present: " + broken.getValue());
    }
  }

  @Override
  public void clear(@Mutable AbstractBiMap<K, V> this) {
    delegate.clear();
    inverse.delegate.clear();
  }

  // Views

  @Override
  public @PolyMutable BiMap<V, K> inverse(@PolyMutable AbstractBiMap<K, V> this) {
    return inverse;
  }

  @LazyInit @CheckForNull private transient Set<K> keySet;

  @SideEffectFree
  @Override
  public @PolyMutable Set<@KeyFor({"this"}) K> keySet(@PolyMutable AbstractBiMap<K, V> this) {
    Set<K> result = keySet;
    return (result == null) ? keySet = new KeySet() : result;
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class KeySet extends ForwardingSet<K> {
    @Override
    protected @PolyMutable Set<K> delegate(@PolyMutable KeySet this) {
      return delegate.keySet();
    }

    @Override
    public void clear(@Mutable KeySet this) {
      AbstractBiMap.this.clear();
    }

    @Override
    public boolean remove(@Mutable KeySet this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      if (!contains(key)) {
        return false;
      }
      removeFromBothMaps(key);
      return true;
    }

    @Override
    public boolean removeAll(@Mutable KeySet this, @Readonly Collection<?> keysToRemove) {
      return standardRemoveAll(keysToRemove);
    }

    @Override
    public boolean retainAll(@Mutable KeySet this, @Readonly Collection<?> keysToRetain) {
      return standardRetainAll(keysToRetain);
    }

    @Override
    public Iterator<K> iterator(@Readonly KeySet this) {
      return Maps.keyIterator(entrySet().iterator());
    }
  }

  @LazyInit @CheckForNull private transient Set<V> valueSet;

  @SideEffectFree
  @Override
  public @PolyMutable Set<V> values(@PolyMutable AbstractBiMap<K, V> this) {
    /*
     * We can almost reuse the inverse's keySet, except we have to fix the
     * iteration order so that it is consistent with the forward map.
     */
    Set<V> result = valueSet;
    return (result == null) ? valueSet = new ValueSet() : result;
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class ValueSet extends ForwardingSet<V> {
    final Set<V> valuesDelegate = inverse.keySet();

    @Override
    protected @PolyMutable Set<V> delegate(@PolyMutable ValueSet this) {
      return valuesDelegate;
    }

    @Override
    public Iterator<V> iterator(@Readonly ValueSet this) {
      return Maps.valueIterator(entrySet().iterator());
    }

    @CFComment("signedness: is same as signedness of V, which this method doesn't know")
    @Override
    public @PolyNull @PolySigned Object[] toArray(ValueSet this) {
      return standardToArray();
    }

    @Override
    @SuppressWarnings("nullness") // bug in our checker's handling of toArray signatures
    public <T extends @Nullable @UnknownSignedness @Readonly Object> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Pure
    @Override
    public String toString() {
      return standardToString();
    }
  }

  @LazyInit @CheckForNull private transient Set<Entry<K, V>> entrySet;

  @SideEffectFree
  @Override
  public @PolyMutable Set<Entry<@KeyFor({"this"}) K, V>> entrySet(@PolyMutable AbstractBiMap<K, V> this) {
    Set<Entry<K, V>> result = entrySet;
    return (result == null) ? entrySet = new EntrySet() : result;
  }

  @ReceiverDependentMutable
  class BiMapEntry extends ForwardingMapEntry<K, V> {
    private final Entry<K, V> delegate;

    BiMapEntry(@ReceiverDependentMutable Entry<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected @PolyMutable Entry<K, V> delegate(@PolyMutable BiMapEntry this) {
      return delegate;
    }

    @Override
    public V setValue(@Mutable BiMapEntry this, V value) {
      checkValue(value);
      // Preconditions keep the map and inverse consistent.
      checkState(entrySet().contains(this), "entry no longer in map");
      // similar to putInBothMaps, but set via entry
      if (Objects.equal(value, getValue())) {
        return value;
      }
      checkArgument(!containsValue(value), "value already present: %s", value);
      V oldValue = delegate.setValue(value);
      checkState(Objects.equal(value, get(getKey())), "entry no longer in map");
      updateInverseMap(getKey(), true, oldValue, value);
      return oldValue;
    }
  }

  Iterator<Entry<K, V>> entrySetIterator(@Readonly AbstractBiMap<K, V> this) {
    final Iterator<Entry<K, V>> iterator = delegate.entrySet().iterator();
    return new Iterator<Entry<K, V>>() {
      @CheckForNull Entry<K, V> entry;

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        entry = iterator.next();
        return new BiMapEntry(entry);
      }

      @Override
      public void remove() {
        if (entry == null) {
          throw new IllegalStateException("no calls to next() since the last call to remove()");
        }
        V value = entry.getValue();
        iterator.remove();
        removeFromInverseMap(value);
        entry = null;
      }
    };
  }

  @WeakOuter
  @ReceiverDependentMutable
  private class EntrySet extends ForwardingSet<Entry<K, V>> {
    final Set<Entry<K, V>> esDelegate = delegate.entrySet();

    @Override
    protected Set<Entry<K, V>> delegate(@PolyMutable EntrySet this) {
      return esDelegate;
    }

    @Override
    public void clear(@Mutable EntrySet this) {
      AbstractBiMap.this.clear();
    }

    @Override
    public boolean remove(@Mutable EntrySet this,  @CheckForNull @UnknownSignedness @Readonly Object object) {
      /*
       * `o instanceof Entry` is guaranteed by `contains`, but we check it here to satisfy our
       * nullness checker.
       */
      if (!esDelegate.contains(object) || !(object instanceof Entry)) {
        return false;
      }

      Entry<?, ?> entry = (Entry<?, ?>) object;
      inverse.delegate.remove(entry.getValue());
      /*
       * Remove the mapping in inverse before removing from esDelegate because
       * if entry is part of esDelegate, entry might be invalidated after the
       * mapping is removed from esDelegate.
       */
      esDelegate.remove(entry);
      return true;
    }

    @Override
    public Iterator<Entry<K, V>> iterator(@Readonly EntrySet this) {
      return entrySetIterator();
    }

    // See java.util.Collections.CheckedEntrySet for details on attacks.

    /*
     * standardToArray returns `@Nullable Object[]` rather than `Object[]` but only because it can
     * be used with collections that may contain null. This collection never contains nulls, so we
     * can treat it as a plain `Object[]`.
     */
    @SuppressWarnings("nullness")
    @CFComment("signedeness: is same as V, which this method does not know")
    @Override
    public @PolyNull @PolySigned Object[] toArray() {
      return standardToArray();
    }

    @Override
    @SuppressWarnings("nullness") // bug in our checker's handling of toArray signatures
    public <T extends @Nullable @UnknownSignedness @Readonly Object> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Pure
    @Override
    public boolean contains(@Readonly EntrySet this, @CheckForNull @UnknownSignedness @Readonly Object o) {
      return Maps.containsEntryImpl(delegate(), o);
    }

    @Pure
    @Override
    public boolean containsAll(@Readonly EntrySet this, @Readonly Collection<?> c) {
      return standardContainsAll(c);
    }

    @Override
    public boolean removeAll(@Mutable EntrySet this, @Readonly Collection<?> c) {
      return standardRemoveAll(c);
    }

    @Override
    public boolean retainAll(@Mutable EntrySet this, @Readonly Collection<?> c) {
      return standardRetainAll(c);
    }
  }

  /** The inverse of any other {@code AbstractBiMap} subclass. */
  @ReceiverDependentMutable
  static class Inverse<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
      extends AbstractBiMap<K, V> {
    Inverse(@ReceiverDependentMutable Map<K, V> backward, @ReceiverDependentMutable AbstractBiMap<V, K> forward) {
      super(backward, forward);
    }

    /*
     * Serialization stores the forward bimap, the inverse of this inverse.
     * Deserialization calls inverse() on the forward bimap and returns that
     * inverse.
     *
     * If a bimap and its inverse are serialized together, the deserialized
     * instances have inverse() methods that return the other.
     */

    @Override
    @ParametricNullness
    K checkKey(@Readonly Inverse<K, V> this, @ParametricNullness K key) {
      return inverse.checkValue(key);
    }

    @Override
    @ParametricNullness
    V checkValue(@Readonly Inverse<K, V> this, @ParametricNullness V value) {
      return inverse.checkKey(value);
    }

    /**
     * @serialData the forward bimap
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(inverse());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      setInverse((AbstractBiMap<V, K>) requireNonNull(stream.readObject()));
    }

    @GwtIncompatible // Not needed in the emulated source.
    @J2ktIncompatible
    Object readResolve() {
      return inverse().inverse();
    }

    @GwtIncompatible // Not needed in emulated source.
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // Not needed in emulated source.
  @J2ktIncompatible
  private static final long serialVersionUID = 0;
}
