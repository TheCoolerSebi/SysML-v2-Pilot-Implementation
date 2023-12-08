/*******************************************************************************
 * Copyright (c) 2015, 2021 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2023 Budapest University of Technology and Economics (BME)
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Kristóf Marussy, BME
 *******************************************************************************/

package org.omg.sysml.xtext.serializer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.serializer.ISerializationContext;
import org.eclipse.xtext.serializer.analysis.GrammarConstraintProvider;
import org.eclipse.xtext.serializer.analysis.GrammarElementDeclarationOrder;
import org.eclipse.xtext.serializer.analysis.IContextTypePDAProvider;
import org.eclipse.xtext.serializer.analysis.IGrammarConstraintProvider;
import org.eclipse.xtext.serializer.analysis.ISemanticSequencerNfaProvider;
import org.eclipse.xtext.serializer.analysis.ISemanticSequencerNfaProvider.ISemState;
import org.eclipse.xtext.serializer.analysis.ISerState;
import org.eclipse.xtext.serializer.analysis.SerializationContextMap;
import org.eclipse.xtext.serializer.analysis.SerializationContextMap.Entry;
import org.eclipse.xtext.util.formallang.Nfa;
import org.eclipse.xtext.util.formallang.Pda;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Patched version of {@link GrammarConstraintProvider} that uses Tarjan's
 * algorithm to find strongly connected components in NFA instead of
 * {@link org.eclipse.xtext.util.formallang.NfaUtil#findCycles(Nfa)}.
 * 
 * In complex Xtext grammars with rule fragments, such as in the SysML grammar,
 * there can be infinite paths through a node in the corresponding NFA that are
 * not a single cycle. The original {@link GrammarConstraintProvider} causes the
 * stack to overflow in
 * {@link Constraint#computeUpperBound(int, ISemState, Map, Map)} due to
 * recursive invocations. We opt to use <a href=
 * "https://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm">Tarjan's
 * algorithm</a> instead to find all strongly connected components and thus all
 * possible infinite paths.
 * 
 * Published under the Eclipse Public License - v 2.0, because of code copied
 * verbatim from {@link GrammarConstraintProvider}.
 * 
 * @author Moritz Eysholdt - Initial contribution and API of original
 *         {@link GrammarConstraintProvider}
 * @author Kristóf Marussy - Modifications for SysMl
 */
@SuppressWarnings("restriction")
@Singleton
public class SysMLGrammarConstraintProvider extends GrammarConstraintProvider {

	protected static class Constraint extends GrammarConstraintProvider.Constraint {

		private final Nfa<ISemState> nfa;

		private final EClass type;

		public Constraint(Grammar grammar, EClass type, Nfa<ISemState> nfa) {
			super(grammar, type, nfa);
			this.type = type;
			this.nfa = nfa;
		}

		@Override
		protected void setName(String name) {
			// Make the setName method visible to the enclosing class by re-declaring it.
			super.setName(name);
		}

		protected int[] computeUpperBounds() {
			Map<ISemState, Set<ISemState>> cycles = new Tarjan(nfa).computeComponents();
			int[] bounds = new int[type.getFeatureCount()];
			for (Set<ISemState> cycle : cycles.values()) {
				for (ISemState node : cycle) {
					int featureId = node.getFeatureID();
					if (featureId >= 0) {
						bounds[featureId] = IGrammarConstraintProvider.MAX;
					}
				}
			}

			// Handle the remaining features with the linear-time longest path algorithm.
			for (int i = 0; i < bounds.length; i++) {
				if (bounds[i] != IGrammarConstraintProvider.MAX) {
					bounds[i] = computeUpperBound(i, nfa.getStart(), cycles, Maps.newHashMap());
				}
			}
			return bounds;
		}

		/**
		 * This longest path algorithm runs in linear time because each cycle is treated
		 * as a single node.
		 */
		private int computeUpperBound(int featureId, ISemState node, Map<ISemState, Set<ISemState>> cycles,
				Map<ISemState, Integer> computedDistances) {
			Integer distance = computedDistances.get(node);
			if (distance != null) {
				// We have already visited the given node.
				return distance;
			} else if (cycles.containsKey(node)) {
				// Same procedure as for regular nodes, but consider all outgoing edges of all
				// nodes in the cycle.
				Set<ISemState> cycle = cycles.get(node);
				int maxDistance = 0;
				for (ISemState cycleNode : cycle) {
					for (ISemState follower : cycleNode.getFollowers()) {
						if (!cycle.contains(follower)) {
							int followerDistance = computeUpperBound(featureId, follower, cycles, computedDistances);
							maxDistance = Math.max(maxDistance, followerDistance);
						}
					}
				}
				for (ISemState cycleNode : cycle) {
					computedDistances.put(cycleNode, maxDistance);
				}
				return maxDistance;
			} else {
				// Compute the maximum of all longest paths from any follower to the stop node.
				int increment = node.getFeatureID() == featureId ? 1 : 0;
				int maxDistance = 0;
				for (ISemState follower : node.getFollowers()) {
					if (follower.equals(node)) {
						if (increment > 0) {
							// Since Tarjan's algorithms did not distinguish between 1-node SCC with and
							// without a self loop, we have to pay extra attention to self loops here.
							maxDistance = IGrammarConstraintProvider.MAX;
						}
					} else {
						int followerDistance = computeUpperBound(featureId, follower, cycles, computedDistances);
						maxDistance = Math.max(maxDistance, followerDistance + increment);
					}
				}
				computedDistances.put(node, maxDistance);
				return maxDistance;
			}
		}
	}

	/**
	 * Tarjan's algorithm for strongly-connected components.
	 * 
	 * Implemented according to
	 * https://en.wikipedia.org/w/index.php?title=Tarjan's_strongly_connected_components_algorithm&oldid=1186519335
	 */
	private static class Tarjan {
		private final Nfa<ISemState> nfa;

		private final Map<ISemState, Integer> index = new HashMap<>();

		private final Map<ISemState, Integer> lowLink = new HashMap<>();

		private final Set<ISemState> onStack = new HashSet<>();

		private final Deque<ISemState> stack = new ArrayDeque<>();

		private int nextIndex = 0;

		private List<Set<ISemState>> components = new ArrayList<>();

		public Tarjan(Nfa<ISemState> nfa) {
			this.nfa = nfa;
		}

		public Map<ISemState, Set<ISemState>> computeComponents() {
			strongConnect(nfa.getStart());
			Map<ISemState, Set<ISemState>> componentsMap = new HashMap<>();
			for (Set<ISemState> component : components) {
				if (component.size() >= 2) {
					for (ISemState state : component) {
						componentsMap.put(state, component);
					}
				}
			}
			return componentsMap;
		}

		private void strongConnect(ISemState state) {
			index.put(state, nextIndex);
			lowLink.put(state, nextIndex);
			nextIndex += 1;
			stack.addLast(state);
			onStack.add(state);
			for (ISemState follower : state.getFollowers()) {
				Integer followerIndex = index.get(follower);
				if (followerIndex == null) {
					strongConnect(follower);
					lowLink.put(state, Math.min(lowLink.get(state), lowLink.get(follower)));
				} else if (onStack.contains(follower)) {
					lowLink.put(state, Math.min(lowLink.get(state), followerIndex));
				}
			}
			if (lowLink.get(state).equals(index.get(state))) {
				Set<ISemState> component = new HashSet<>();
				ISemState popped;
				do {
					popped = stack.removeLast();
					onStack.remove(popped);
					component.add(popped);
				} while (!state.equals(popped));
				components.add(component);
			}
		}
	}

	private Map<Grammar, SerializationContextMap<IConstraint>> cache = Maps.newHashMap();

	@Inject
	private ISemanticSequencerNfaProvider nfaProvider;

	@Inject
	private IContextTypePDAProvider typeProvider;

	@Override
	public SerializationContextMap<IConstraint> getConstraints(Grammar grammar) {
		SerializationContextMap<IConstraint> cached = cache.get(grammar);
		if (cached != null)
			return cached;
		SerializationContextMap.Builder<IConstraint> builder = SerializationContextMap.builder();
		GrammarElementDeclarationOrder.get(grammar);
		SerializationContextMap<Nfa<ISemState>> nfas = nfaProvider.getSemanticSequencerNFAs(grammar);
		for (Entry<Nfa<ISemState>> e : nfas.values()) {
			Nfa<ISemState> nfa = e.getValue();
			for (EClass type : e.getTypes()) {
				Constraint constraint = new Constraint(grammar, type, nfa);
				List<ISerializationContext> contexts = e.getContexts(type);
				constraint.getContexts().addAll(contexts);
				builder.put(contexts, constraint);
			}
		}
		SerializationContextMap<IConstraint> result = builder.create();
		SerializationContextMap<Pda<ISerState, RuleCall>> typePDAs = typeProvider.getContextTypePDAs(grammar);
		for (Entry<IConstraint> e : result.values()) {
			Constraint constraint = (Constraint) e.getValue();
			constraint.setName(findBestConstraintName(grammar, typePDAs, constraint));
		}
		cache.put(grammar, result);
		return result;
	}
}
