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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps.ViewCachingAbstractMap;
import com.google.j2objc.annotations.WeakOuter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;

/**
 * Implementation of {@link Multimaps#filterEntries(Multimap, Predicate)}.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
class FilteredEntryMultimap<K extends @Nullable @Immutable Object, V extends @Nullable @Immutable Object>
    extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
  final Multimap<K, V> unfiltered;
  final @Readonly Predicate<? super @Readonly Entry<K, V>> predicate;

  FilteredEntryMultimap(@ReceiverDependentMutable Multimap<K, V> unfiltered, @Readonly Predicate<? super @Readonly Entry<K, V>> predicate) {
    this.unfiltered = checkNotNull(unfiltered);
    this.predicate = checkNotNull(predicate);
  }

  @Override
  public @ReceiverDependentMutable Multimap<K, V> unfiltered() {
    return unfiltered;
  }

  @Override
  public @Readonly Predicate<? super @Readonly Entry<K, V>> entryPredicate() {
    return predicate;
  }

  @Override
  public int size() {
    return entries().size();
  }

  private boolean satisfies(@ParametricNullness K key, @ParametricNullness V value) {
    return predicate.apply(Maps.immutableEntry(key, value));
  }

  final class ValuePredicate implements Predicate<V> {
    @ParametricNullness private final K key;

    ValuePredicate(@ParametricNullness K key) {
      this.key = key;
    }

    @Override
    public boolean apply(@ParametricNullness V value) {
      return satisfies(key, value);
    }
  }

  static <E extends @Nullable @Readonly Object> @PolyMutable Collection<E> filterCollection(
      @PolyMutable Collection<E> collection, @Readonly Predicate<? super E> predicate) {
    if (collection instanceof Set) {
      return Sets.filter((@PolyMutable Set<E>) collection, predicate);
    } else {
      return Collections2.filter(collection, predicate);
    }
  }

  @Override
  public boolean containsKey(@CheckForNull @UnknownSignedness @Readonly Object key) {
    return asMap().get(key) != null;
  }

  @Override
  public @Readonly Collection<V> removeAll(@Mutable FilteredEntryMultimap<K, V> this, @CheckForNull @Readonly Object key) {
    return MoreObjects.firstNonNull(asMap().remove(key), unmodifiableEmptyCollection());
  }

  @Immutable Collection<V> unmodifiableEmptyCollection() {
    // These return false, rather than throwing a UOE, on remove calls.
    return (unfiltered instanceof SetMultimap)
        ? Collections.<V>emptySet()
        : Collections.<V>emptyList();
  }

  @Override
  public void clear(@Mutable FilteredEntryMultimap<K, V> this) {
    entries().clear();
  }

  @Override
  public @ReceiverDependentMutable Collection<V> get(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this, @ParametricNullness K key) {
    return filterCollection(unfiltered.get(key), new ValuePredicate(key));
  }

  @Override
  @ReceiverDependentMutable Collection<@ReceiverDependentMutable Entry<K, V>> createEntries(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this) {
    return filterCollection(unfiltered.entries(), predicate);
  }

  @Override
  @ReceiverDependentMutable Collection<V> createValues(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this) {
    return new @ReceiverDependentMutable FilteredMultimapValues<>(this);
  }

  @Override
  @Mutable Iterator<@ReceiverDependentMutable Entry<K, V>> entryIterator(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this) {
    throw new AssertionError("should never be called");
  }

  @Override
  @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> createAsMap(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this) {
    return new @ReceiverDependentMutable AsMap();
  }

  @Override
  @ReceiverDependentMutable Set<K> createKeySet() {
    return asMap().keySet();
  }

  boolean removeEntriesIf(@Mutable FilteredEntryMultimap<K, V> this, Predicate<? super @Readonly Entry<K, Collection<V>>> predicate) {
    Iterator<Entry<K, Collection<V>>> entryIterator = unfiltered.asMap().entrySet().iterator();
    boolean changed = false;
    while (entryIterator.hasNext()) {
      Entry<K, Collection<V>> entry = entryIterator.next();
      K key = entry.getKey();
      Collection<V> collection = filterCollection(entry.getValue(), new ValuePredicate(key));
      if (!collection.isEmpty() && predicate.apply(Maps.immutableEntry(key, collection))) {
        if (collection.size() == entry.getValue().size()) {
          entryIterator.remove();
        } else {
          collection.clear();
        }
        changed = true;
      }
    }
    return changed;
  }

  @WeakOuter
  @ReceiverDependentMutable
  class AsMap extends ViewCachingAbstractMap<K, @ReceiverDependentMutable Collection<V>> {
    @Override
    public boolean containsKey(@CheckForNull @UnknownSignedness @Readonly Object key) {
      return get(key) != null;
    }

    @Override
    public void clear(@Mutable AsMap this) {
      FilteredEntryMultimap.this.clear();
    }

    @Override
    @CheckForNull
    @SuppressWarnings("mutability:return.type.incompatible") // Outer.this in an inner class is @Mutable, not the enclosing instance's qualifier: https://github.com/eisop/checker-framework/issues/2322
    public @ReceiverDependentMutable Collection<V> get(@ReceiverDependentMutable AsMap this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      Collection<V> result = unfiltered.asMap().get(key);
      if (result == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      result = filterCollection(result, new ValuePredicate(k));
      return result.isEmpty() ? null : result;
    }

    @Override
    @CheckForNull
    public @Readonly Collection<V> remove(@Mutable AsMap this, @CheckForNull @UnknownSignedness @Readonly Object key) {
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // it's definitely equal to a K
      K k = (K) key;
      List<V> result = Lists.newArrayList();
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          itr.remove();
          result.add(v);
        }
      }
      if (result.isEmpty()) {
        return null;
      } else if (unfiltered instanceof SetMultimap) {
        return Collections.unmodifiableSet(Sets.newLinkedHashSet(result));
      } else {
        return Collections.unmodifiableList(result);
      }
    }

    @Override
    @ReceiverDependentMutable Set<K> createKeySet(@ReceiverDependentMutable AsMap this) {
      @WeakOuter
      @ReceiverDependentMutable
      class KeySetImpl extends Maps.KeySet<K, @ReceiverDependentMutable Collection<V>> {
        @SuppressWarnings("mutability:argument.type.incompatible") // Outer.this in a local or anonymous class is @Mutable, not the enclosing instance's qualifier, and the class has no receiver syntax to state it: https://github.com/eisop/checker-framework/issues/2322
        KeySetImpl() {
          super(AsMap.this);
        }

        @Override
        public boolean removeAll(@Mutable KeySetImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(Maps.<K>keyPredicateOnEntries(in(c)));
        }

        @Override
        public boolean retainAll(@Mutable KeySetImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(Maps.<K>keyPredicateOnEntries(not(in(c))));
        }

        @Override
        public boolean remove(@Mutable KeySetImpl this, @CheckForNull @UnknownSignedness @Readonly Object o) {
          return AsMap.this.remove(o) != null;
        }
      }
      return new @ReceiverDependentMutable KeySetImpl();
    }

    @Override
    @ReceiverDependentMutable Set<@ReceiverDependentMutable Entry<K, @ReceiverDependentMutable Collection<V>>> createEntrySet(@ReceiverDependentMutable AsMap this) {
      @WeakOuter
      @ReceiverDependentMutable
      class EntrySetImpl extends Maps.EntrySet<K, @ReceiverDependentMutable Collection<V>> {
        @Override
        @SuppressWarnings("mutability:return.type.incompatible") // Outer.this in a local or anonymous class is @Mutable, not the enclosing instance's qualifier, and the class has no receiver syntax to state it: https://github.com/eisop/checker-framework/issues/2322
        @ReceiverDependentMutable Map<K, @ReceiverDependentMutable Collection<V>> map(@ReceiverDependentMutable EntrySetImpl this) {
          return AsMap.this;
        }

        @Override
        @SuppressWarnings("mutability:override.return.invalid") // each entry is an immutableEntry snapshot of a key and its live value collection, and does not support setValue, so the entries are @Immutable where the map contract states receiver-dependent ones
        public @Mutable Iterator<@Immutable Entry<K, Collection<V>>> iterator() {
          return new @Mutable AbstractIterator<@Immutable Entry<K, Collection<V>>>() {
            final Iterator<Entry<K, Collection<V>>> backingIterator =
                unfiltered.asMap().entrySet().iterator();

            @Override
            @CheckForNull
            protected @Immutable Entry<K, Collection<V>> computeNext() {
              while (backingIterator.hasNext()) {
                Entry<K, Collection<V>> entry = backingIterator.next();
                K key = entry.getKey();
                Collection<V> collection =
                    filterCollection(entry.getValue(), new ValuePredicate(key));
                if (!collection.isEmpty()) {
                  return Maps.immutableEntry(key, collection);
                }
              }
              return endOfData();
            }
          };
        }

        @Override
        public boolean removeAll(@Mutable EntrySetImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(in(c));
        }

        @Override
        public boolean retainAll(@Mutable EntrySetImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(not(in(c)));
        }

        @Override
        public @NonNegative int size() {
          return Iterators.size(iterator());
        }
      }
      return new @ReceiverDependentMutable EntrySetImpl();
    }

    @Override
    @ReceiverDependentMutable Collection<@ReceiverDependentMutable Collection<V>> createValues(@ReceiverDependentMutable AsMap this) {
      @WeakOuter
      @ReceiverDependentMutable
      class ValuesImpl extends Maps.Values<K, @ReceiverDependentMutable Collection<V>> {
        @SuppressWarnings("mutability:argument.type.incompatible") // Outer.this in a local or anonymous class is @Mutable, not the enclosing instance's qualifier, and the class has no receiver syntax to state it: https://github.com/eisop/checker-framework/issues/2322
        ValuesImpl() {
          super(AsMap.this);
        }

        @Override
        public boolean remove(@Mutable ValuesImpl this, @CheckForNull @UnknownSignedness @Readonly Object o) {
          if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            Iterator<Entry<K, Collection<V>>> entryIterator =
                unfiltered.asMap().entrySet().iterator();
            while (entryIterator.hasNext()) {
              Entry<K, Collection<V>> entry = entryIterator.next();
              K key = entry.getKey();
              Collection<V> collection =
                  filterCollection(entry.getValue(), new ValuePredicate(key));
              if (!collection.isEmpty() && c.equals(collection)) {
                if (collection.size() == entry.getValue().size()) {
                  entryIterator.remove();
                } else {
                  collection.clear();
                }
                return true;
              }
            }
          }
          return false;
        }

        @Override
        public boolean removeAll(@Mutable ValuesImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(Maps.<Collection<V>>valuePredicateOnEntries(in(c)));
        }

        @Override
        public boolean retainAll(@Mutable ValuesImpl this, @Readonly Collection<?> c) {
          return removeEntriesIf(Maps.<Collection<V>>valuePredicateOnEntries(not(in(c))));
        }
      }
      return new @ReceiverDependentMutable ValuesImpl();
    }
  }

  @Override
  @ReceiverDependentMutable Multiset<K> createKeys(@ReceiverDependentMutable FilteredEntryMultimap<K, V> this) {
    return new @ReceiverDependentMutable Keys();
  }

  @WeakOuter
  @ReceiverDependentMutable
  class Keys extends Multimaps.Keys<K, V> {
    @SuppressWarnings("mutability:argument.type.incompatible") // Outer.this in an inner-class constructor is @Mutable, not the enclosing instance's qualifier: https://github.com/eisop/checker-framework/issues/2322
    Keys() {
      super(FilteredEntryMultimap.this);
    }

    @Override
    public int remove(@CheckForNull @Readonly Object key, int occurrences) {
      checkNonnegative(occurrences, "occurrences");
      if (occurrences == 0) {
        return count(key);
      }
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return 0;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      int oldCount = 0;
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          oldCount++;
          if (oldCount <= occurrences) {
            itr.remove();
          }
        }
      }
      return oldCount;
    }

    @Override
    public @ReceiverDependentMutable Set<Multiset.@Readonly Entry<K>> entrySet(@ReceiverDependentMutable Keys this) {
      return new Multisets.@ReceiverDependentMutable EntrySet<K>() {

        @Override
        Multiset<K> multiset() {
          return Keys.this;
        }

        @Override
        public Iterator<Multiset.Entry<K>> iterator() {
          return Keys.this.entryIterator();
        }

        @Override
        public @NonNegative int size() {
          return FilteredEntryMultimap.this.keySet().size();
        }

        private boolean removeEntriesIf(Predicate<? super Multiset.@Readonly Entry<K>> predicate) {
          return FilteredEntryMultimap.this.removeEntriesIf(
              (Map.@Readonly Entry<K, Collection<V>> entry) ->
                  predicate.apply(
                      Multisets.immutableEntry(entry.getKey(), entry.getValue().size())));
        }

        @Override
        public boolean removeAll(@Readonly Collection<?> c) {
          return removeEntriesIf(in(c));
        }

        @Override
        public boolean retainAll(@Readonly Collection<?> c) {
          return removeEntriesIf(not(in(c)));
        }
      };
    }
  }
}
