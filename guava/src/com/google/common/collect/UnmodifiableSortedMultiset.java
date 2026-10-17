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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multisets.UnmodifiableMultiset;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Comparator;
import java.util.NavigableSet;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.CFComment;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * Implementation of {@link Multisets#unmodifiableSortedMultiset(SortedMultiset)}, split out into
 * its own file so it can be GWT emulated (to deal with the differing elementSet() types in GWT and
 * non-GWT).
 *
 * @author Louis Wasserman
 */
@AnnotatedFor("mutability")
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class UnmodifiableSortedMultiset<E extends @Nullable @Readonly Object> extends UnmodifiableMultiset<E>
    implements SortedMultiset<E> {
  UnmodifiableSortedMultiset(@ReceiverDependentMutable SortedMultiset<E> delegate) {
    super(delegate);
  }

  @Override
  protected @ReceiverDependentMutable SortedMultiset<E> delegate(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this) {
    return (@ReceiverDependentMutable SortedMultiset<E>) super.delegate();
  }

  @Override
  public @Readonly Comparator<? super E> comparator(UnmodifiableSortedMultiset<E> this) {
    return delegate().comparator();
  }

  @Override
  @ReceiverDependentMutable NavigableSet<E> createElementSet(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this) {
    return Sets.unmodifiableNavigableSet(delegate().elementSet());
  }

  @Override
  public @ReceiverDependentMutable NavigableSet<E> elementSet(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this) {
    return (NavigableSet<E>) super.elementSet();
  }

  @LazyInit @CheckForNull private transient @Assignable UnmodifiableSortedMultiset<E> descendingMultiset;

  @Override
  public @ReceiverDependentMutable SortedMultiset<E> descendingMultiset(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this) {
    UnmodifiableSortedMultiset<E> result = descendingMultiset;
    if (result == null) {
      result = new @ReceiverDependentMutable UnmodifiableSortedMultiset<>(delegate().descendingMultiset());
      result.descendingMultiset = this;
      return descendingMultiset = result;
    }
    return result;
  }

  @Override
  @CheckForNull
  public @Readonly Entry<E> firstEntry(UnmodifiableSortedMultiset<E> this) {
    return delegate().firstEntry();
  }

  @Override
  @CheckForNull
  public @Readonly Entry<E> lastEntry(UnmodifiableSortedMultiset<E> this) {
    return delegate().lastEntry();
  }

  @Override
  @CheckForNull
  public Entry<E> pollFirstEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  @CheckForNull
  public Entry<E> pollLastEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @ReceiverDependentMutable SortedMultiset<E> headMultiset(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this, @ParametricNullness E upperBound, BoundType boundType) {
    return Multisets.unmodifiableSortedMultiset(delegate().headMultiset(upperBound, boundType));
  }

  @Override
  public @ReceiverDependentMutable SortedMultiset<E> subMultiset(
          @ReceiverDependentMutable UnmodifiableSortedMultiset<E> this,
      @ParametricNullness E lowerBound,
      BoundType lowerBoundType,
      @ParametricNullness E upperBound,
      BoundType upperBoundType) {
    return Multisets.unmodifiableSortedMultiset(
        delegate().subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType));
  }

  @Override
  public @ReceiverDependentMutable SortedMultiset<E> tailMultiset(@ReceiverDependentMutable UnmodifiableSortedMultiset<E> this, @ParametricNullness E lowerBound, BoundType boundType) {
    return Multisets.unmodifiableSortedMultiset(delegate().tailMultiset(lowerBound, boundType));
  }

  private static final long serialVersionUID = 0;
}
