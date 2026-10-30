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

import static com.google.common.graph.GraphConstants.EXPECTED_DEGREE;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * An implementation of {@link NetworkConnections} for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class DirectedNetworkConnections<N extends @Immutable Object, E extends @Immutable Object> extends AbstractDirectedNetworkConnections<N, E> {

  DirectedNetworkConnections(@ReceiverDependentMutable Map<E, N> inEdgeMap, @ReceiverDependentMutable Map<E, N> outEdgeMap, int selfLoopCount) {
    super(inEdgeMap, outEdgeMap, selfLoopCount);
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Mutable DirectedNetworkConnections<N, E> of() {
    return new @Mutable DirectedNetworkConnections<>(
        HashBiMap.<E, N>create(EXPECTED_DEGREE), HashBiMap.<E, N>create(EXPECTED_DEGREE), 0);
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Immutable DirectedNetworkConnections<N, E> ofImmutable(
      @Readonly Map<E, N> inEdges, @Readonly Map<E, N> outEdges, int selfLoopCount) {
    return new @Immutable DirectedNetworkConnections<>(
        ImmutableBiMap.copyOf(inEdges), ImmutableBiMap.copyOf(outEdges), selfLoopCount);
  }

  @Override
  public @Readonly Set<N> predecessors(@ReceiverDependentMutable DirectedNetworkConnections<N, E> this) {
    return Collections.unmodifiableSet(((BiMap<E, N>) inEdgeMap).values());
  }

  @Override
  public @Readonly Set<N> successors(@ReceiverDependentMutable DirectedNetworkConnections<N, E> this) {
    return Collections.unmodifiableSet(((BiMap<E, N>) outEdgeMap).values());
  }

  @Override
  public @Readonly Set<E> edgesConnecting(@ReceiverDependentMutable DirectedNetworkConnections<N, E> this, N node) {
    return new EdgesConnecting<>(((BiMap<E, N>) outEdgeMap).inverse(), node);
  }
}
