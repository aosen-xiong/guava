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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Deque;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A deque which forwards all its method calls to another deque. Subclasses should override one or
 * more methods to modify the behavior of the backing deque as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingDeque} forward <b>indiscriminately</b> to the
 * methods of the delegate. For example, overriding {@link #add} alone <b>will not</b> change the
 * behavior of {@link #offer} which can lead to unexpected behavior. In this case, you should
 * override {@code offer} as well.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingDeque}.
 *
 * @author Kurt Alfred Kluever
 * @since 12.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
public abstract class ForwardingDeque<E extends @Nullable @Readonly Object> extends ForwardingQueue<E>
    implements Deque<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingDeque() {}

  @Override
  protected abstract @PolyMutable Deque<E> delegate(@PolyMutable ForwardingDeque<E> this);

  @Override
  public void addFirst(@Mutable ForwardingDeque<E> this, @ParametricNullness E e) {
    delegate().addFirst(e);
  }

  @Override
  public void addLast(@Mutable ForwardingDeque<E> this, @ParametricNullness E e) {
    delegate().addLast(e);
  }

  @Override
  public Iterator<E> descendingIterator(@Readonly ForwardingDeque<E> this) {
    return delegate().descendingIterator();
  }

  @Override
  @ParametricNullness
  public E getFirst(@Readonly ForwardingDeque<E> this) {
    return delegate().getFirst();
  }

  @Override
  @ParametricNullness
  public E getLast(@Readonly ForwardingDeque<E> this) {
    return delegate().getLast();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  public boolean offerFirst(@Mutable ForwardingDeque<E> this, @ParametricNullness E e) {
    return delegate().offerFirst(e);
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  public boolean offerLast(@Mutable ForwardingDeque<E> this, @ParametricNullness E e) {
    return delegate().offerLast(e);
  }

  @Override
  @CheckForNull
  public E peekFirst(@Readonly ForwardingDeque<E> this) {
    return delegate().peekFirst();
  }

  @Override
  @CheckForNull
  public E peekLast(@Readonly ForwardingDeque<E> this) {
    return delegate().peekLast();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  @CheckForNull
  public E pollFirst(@Mutable ForwardingDeque<E> this) {
    return delegate().pollFirst();
  }

  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  @Override
  @CheckForNull
  public E pollLast(@Mutable ForwardingDeque<E> this) {
    return delegate().pollLast();
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E pop(@Mutable ForwardingDeque<E> this) {
    return delegate().pop();
  }

  @Override
  public void push(@Mutable ForwardingDeque<E> this, @ParametricNullness E e) {
    delegate().push(e);
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E removeFirst(@Mutable ForwardingDeque<E> this) {
    return delegate().removeFirst();
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public E removeLast(@Mutable ForwardingDeque<E> this) {
    return delegate().removeLast();
  }

  @CanIgnoreReturnValue
  @Override
  public boolean removeFirstOccurrence(@Mutable ForwardingDeque<E> this, @CheckForNull @Readonly Object o) {
    return delegate().removeFirstOccurrence(o);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean removeLastOccurrence(@Mutable ForwardingDeque<E> this, @CheckForNull @Readonly Object o) {
    return delegate().removeLastOccurrence(o);
  }
}
