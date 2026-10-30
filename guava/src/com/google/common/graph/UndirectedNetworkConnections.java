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
 * An implementation of {@link NetworkConnections} for undirected networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
final class UndirectedNetworkConnections<N extends @Immutable Object, E extends @Immutable Object> extends AbstractUndirectedNetworkConnections<N, E> {

  UndirectedNetworkConnections(@ReceiverDependentMutable Map<E, N> incidentEdgeMap) {
    super(incidentEdgeMap);
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Mutable UndirectedNetworkConnections<N, E> of() {
    return new @Mutable UndirectedNetworkConnections<>(HashBiMap.<E, N>create(EXPECTED_DEGREE));
  }

  static <N extends @Immutable Object, E extends @Immutable Object> @Immutable UndirectedNetworkConnections<N, E> ofImmutable(@Readonly Map<E, N> incidentEdges) {
    return new @Immutable UndirectedNetworkConnections<>(ImmutableBiMap.copyOf(incidentEdges));
  }

  @Override
  public @Readonly Set<N> adjacentNodes(@ReceiverDependentMutable UndirectedNetworkConnections<N, E> this) {
    return Collections.unmodifiableSet(((BiMap<E, N>) incidentEdgeMap).values());
  }

  @Override
  public @Readonly Set<E> edgesConnecting(@ReceiverDependentMutable UndirectedNetworkConnections<N, E> this, N node) {
    return new EdgesConnecting<>(((BiMap<E, N>) incidentEdgeMap).inverse(), node);
  }
}
