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
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * An interface for representing and manipulating an origin node's adjacent nodes and edge values in
 * a {@link Graph}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
@ElementTypesAreNonnullByDefault
@ReceiverDependentMutable
interface GraphConnections<N, V> {

  @Readonly Set<N> adjacentNodes(@ReceiverDependentMutable GraphConnections<N, V> this);

  @Readonly Set<N> predecessors(@ReceiverDependentMutable GraphConnections<N, V> this);

  @Readonly Set<N> successors(@ReceiverDependentMutable GraphConnections<N, V> this);

  /**
   * Returns an iterator over the incident edges.
   *
   * @param thisNode The node that this all of the connections in this class are connected to.
   */
  Iterator<EndpointPair<N>> incidentEdgeIterator(@ReceiverDependentMutable GraphConnections<N, V> this, N thisNode);

  /**
   * Returns the value associated with the edge connecting the origin node to {@code node}, or null
   * if there is no such edge.
   */
  @CheckForNull
  V value(@ReceiverDependentMutable GraphConnections<N, V> this, N node);

  /** Remove {@code node} from the set of predecessors. */
  void removePredecessor(@Mutable GraphConnections<N, V> this, N node);

  /**
   * Remove {@code node} from the set of successors. Returns the value previously associated with
   * the edge connecting the two nodes.
   */
  @CanIgnoreReturnValue
  @CheckForNull
  V removeSuccessor(@Mutable GraphConnections<N, V> this, N node);

  /**
   * Add {@code node} as a predecessor to the origin node. In the case of an undirected graph, it
   * also becomes a successor. Associates {@code value} with the edge connecting the two nodes.
   */
  void addPredecessor(@Mutable GraphConnections<N, V> this, N node, V value);

  /**
   * Add {@code node} as a successor to the origin node. In the case of an undirected graph, it also
   * becomes a predecessor. Associates {@code value} with the edge connecting the two nodes. Returns
   * the value previously associated with the edge connecting the two nodes.
   */
  @CanIgnoreReturnValue
  @CheckForNull
  V addSuccessor(@Mutable GraphConnections<N, V> this, N node, V value);
}
