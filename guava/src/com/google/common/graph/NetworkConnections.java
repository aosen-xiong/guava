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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * An interface for representing and manipulating an origin node's adjacent nodes and incident edges
 * in a {@link Network}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
interface NetworkConnections<N, E> {

  @Readonly Set<N> adjacentNodes(@ReceiverDependentMutable NetworkConnections<N, E> this);

  @Readonly Set<N> predecessors(@ReceiverDependentMutable NetworkConnections<N, E> this);

  @Readonly Set<N> successors(@ReceiverDependentMutable NetworkConnections<N, E> this);

  @Readonly Set<E> incidentEdges(@ReceiverDependentMutable NetworkConnections<N, E> this);

  @Readonly Set<E> inEdges(@ReceiverDependentMutable NetworkConnections<N, E> this);

  @Readonly Set<E> outEdges(@ReceiverDependentMutable NetworkConnections<N, E> this);

  /**
   * Returns the set of edges connecting the origin node to {@code node}. For networks without
   * parallel edges, this set cannot be of size greater than one.
   */
  @Readonly Set<E> edgesConnecting(@ReceiverDependentMutable NetworkConnections<N, E> this, N node);

  /**
   * Returns the node that is adjacent to the origin node along {@code edge}.
   *
   * <p>In the directed case, {@code edge} is assumed to be an outgoing edge.
   */
  N adjacentNode(@ReceiverDependentMutable NetworkConnections<N, E> this, E edge);

  /**
   * Remove {@code edge} from the set of incoming edges. Returns the former predecessor node.
   *
   * <p>In the undirected case, returns {@code null} if {@code isSelfLoop} is true.
   */
  @CanIgnoreReturnValue
  @CheckForNull
  N removeInEdge(@Mutable NetworkConnections<N, E> this, E edge, boolean isSelfLoop);

  /** Remove {@code edge} from the set of outgoing edges. Returns the former successor node. */
  @CanIgnoreReturnValue
  N removeOutEdge(@Mutable NetworkConnections<N, E> this, E edge);

  /**
   * Add {@code edge} to the set of incoming edges. Implicitly adds {@code node} as a predecessor.
   */
  void addInEdge(@Mutable NetworkConnections<N, E> this, E edge, N node, boolean isSelfLoop);

  /** Add {@code edge} to the set of outgoing edges. Implicitly adds {@code node} as a successor. */
  void addOutEdge(@Mutable NetworkConnections<N, E> this, E edge, N node);
}
