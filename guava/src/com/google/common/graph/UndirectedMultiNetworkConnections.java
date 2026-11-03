/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.INNER_CAPACITY;
import static com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * An implementation of {@link NetworkConnections} for undirected networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class UndirectedMultiNetworkConnections<N extends @Immutable Object, E extends @Immutable Object>
    extends AbstractUndirectedNetworkConnections<N, E> {

  private UndirectedMultiNetworkConnections(@ReceiverDependentMutable Map<E, N> incidentEdges) {
    super(incidentEdges);
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Mutable UndirectedMultiNetworkConnections<N, E> of() {
    return new @Mutable UndirectedMultiNetworkConnections<>(
        new HashMap<E, N>(INNER_CAPACITY, INNER_LOAD_FACTOR));
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Immutable UndirectedMultiNetworkConnections<N, E> ofImmutable(@Readonly Map<E, N> incidentEdges) {
    return new @Immutable UndirectedMultiNetworkConnections<>(ImmutableMap.copyOf(incidentEdges));
  }

  @CheckForNull @LazyInit private transient @Assignable @Mutable Reference<@Mutable Multiset<N>> adjacentNodesReference;

  @Override
  public @Readonly Set<N> adjacentNodes(@ReceiverDependentMutable UndirectedMultiNetworkConnections<N, E> this) {
    return Collections.unmodifiableSet(adjacentNodesMultiset().elementSet());
  }

  private Multiset<N> adjacentNodesMultiset(@ReceiverDependentMutable UndirectedMultiNetworkConnections<N, E> this) {
    Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
    if (adjacentNodes == null) {
      adjacentNodes = HashMultiset.create(incidentEdgeMap.values());
      adjacentNodesReference = new SoftReference<>(adjacentNodes);
    }
    return adjacentNodes;
  }

  @Override
  public @Readonly Set<E> edgesConnecting(@ReceiverDependentMutable UndirectedMultiNetworkConnections<N, E> this, N node) {
    return new MultiEdgesConnecting<E>(incidentEdgeMap, node) {
      @Override
      public @NonNegative int size() {
        return adjacentNodesMultiset().count(node);
      }
    };
  }

  @Override
  @CheckForNull
  public N removeInEdge(@Mutable UndirectedMultiNetworkConnections<N, E> this, E edge, boolean isSelfLoop) {
    if (!isSelfLoop) {
      return removeOutEdge(edge);
    }
    return null;
  }

  @Override
  public N removeOutEdge(@Mutable UndirectedMultiNetworkConnections<N, E> this, E edge) {
    N node = super.removeOutEdge(edge);
    Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
    if (adjacentNodes != null) {
      checkState(adjacentNodes.remove(node));
    }
    return node;
  }

  @Override
  public void addInEdge(@Mutable UndirectedMultiNetworkConnections<N, E> this, E edge, N node, boolean isSelfLoop) {
    if (!isSelfLoop) {
      addOutEdge(edge, node);
    }
  }

  @Override
  public void addOutEdge(@Mutable UndirectedMultiNetworkConnections<N, E> this, E edge, N node) {
    super.addOutEdge(edge, node);
    Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
    if (adjacentNodes != null) {
      checkState(adjacentNodes.add(node));
    }
  }

  @CheckForNull
  private static <T> T getReference(@CheckForNull Reference<T> reference) {
    return (reference == null) ? null : reference.get();
  }
}
