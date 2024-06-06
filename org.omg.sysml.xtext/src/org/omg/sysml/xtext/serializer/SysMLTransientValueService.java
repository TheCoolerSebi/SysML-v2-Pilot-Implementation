/**
 * SysML 2 Pilot Implementation
 * Copyright (c) 2023 Budapest University of Technology and Economics (BME)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of theGNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @license LGPL-3.0-or-later <http://spdx.org/licenses/LGPL-3.0-or-later>
 * 
 * Contributors:
 *  Kristóf Marussy, BME
 */

package org.omg.sysml.xtext.serializer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.parsetree.reconstr.impl.DefaultTransientValueService;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.EndFeatureMembership;
import org.omg.sysml.lang.sysml.Expression;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureMembership;
import org.omg.sysml.lang.sysml.FeatureReferenceExpression;
import org.omg.sysml.lang.sysml.InvocationExpression;
import org.omg.sysml.lang.sysml.ItemFlowEnd;
import org.omg.sysml.lang.sysml.Multiplicity;
import org.omg.sysml.lang.sysml.MultiplicityRange;
import org.omg.sysml.lang.sysml.OperatorExpression;
import org.omg.sysml.lang.sysml.OwningMembership;
import org.omg.sysml.lang.sysml.ParameterMembership;
import org.omg.sysml.lang.sysml.Redefinition;
import org.omg.sysml.lang.sysml.Relationship;
import org.omg.sysml.lang.sysml.ReturnParameterMembership;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.util.FeatureUtil;

public class SysMLTransientValueService extends DefaultTransientValueService {

	private static final Set<EStructuralFeature> TRANSIENT_FEATURES;
	private static final Set<EStructuralFeature> OWNING_RELATIONSHIP_TARGETS;
	private static final Set<EStructuralFeature> CHECK_ELEMENTS_INDIVIDUALLY;

	static {
		TRANSIENT_FEATURES = new HashSet<>();
		// Features initialized implicitly by setting delegates or FeatureUtil.
		TRANSIENT_FEATURES.add(SysMLPackage.Literals.ELEMENT__ELEMENT_ID);
		TRANSIENT_FEATURES.add(SysMLPackage.Literals.FEATURE__IS_COMPOSITE);
		TRANSIENT_FEATURES.add(SysMLPackage.Literals.FEATURE_TYPING__TYPED_FEATURE);
		TRANSIENT_FEATURES.add(SysMLPackage.Literals.SUBSETTING__SUBSETTING_FEATURE);
		TRANSIENT_FEATURES.add(SysMLPackage.Literals.REDEFINITION__REDEFINING_FEATURE);

		OWNING_RELATIONSHIP_TARGETS = new HashSet<>();
		OWNING_RELATIONSHIP_TARGETS.add(SysMLPackage.Literals.CONJUGATION__CONJUGATED_TYPE);
		OWNING_RELATIONSHIP_TARGETS.add(SysMLPackage.Literals.PORT_CONJUGATION__ORIGINAL_PORT_DEFINITION);
		OWNING_RELATIONSHIP_TARGETS.add(SysMLPackage.Literals.REFERENCE_SUBSETTING__REFERENCED_FEATURE);
		
		CHECK_ELEMENTS_INDIVIDUALLY = new HashSet<>();
		CHECK_ELEMENTS_INDIVIDUALLY.add(SysMLPackage.Literals.ELEMENT__OWNED_RELATIONSHIP);
		CHECK_ELEMENTS_INDIVIDUALLY.add(SysMLPackage.Literals.RELATIONSHIP__OWNED_RELATED_ELEMENT);
	}

	@Override
	public boolean isCheckElementsIndividually(EObject owner, EStructuralFeature feature) {
		if (CHECK_ELEMENTS_INDIVIDUALLY.contains(feature)) {
			return true;
		}
		return super.isCheckElementsIndividually(owner, feature);
	}

	@Override
	public boolean isTransient(EObject owner, EStructuralFeature feature, int index) {
		if (TRANSIENT_FEATURES.contains(feature)) {
			return true;
		}
		if (SysMLPackage.Literals.FEATURE__DIRECTION.equals(feature)) {
			// If direction is computed from the owningFeatureMembership, we cannot
			// serialize it.
			FeatureMembership owningFeatureMembership = ((Feature) owner).getOwningFeatureMembership();
			if (owningFeatureMembership instanceof ReturnParameterMembership) {
				return true;
			} else if (owningFeatureMembership instanceof ParameterMembership) {
				return true;
			} else if (owningFeatureMembership != null) {
				Type owningType = owningFeatureMembership.getOwningType();
				if (owningType instanceof ItemFlowEnd) {
					EList<Redefinition> redefinitions = ((Feature) owner).getOwnedRedefinition();
					if (!redefinitions.isEmpty()) {
						Feature redefinedFeature = redefinitions.get(0).getRedefinedFeature();
						if (redefinedFeature != null) {
							return true;
						}
					}
				}
			}
		} else if (SysMLPackage.Literals.FEATURE__IS_END.equals(feature)) {
			// If isEnd is computed from the owningMembership, we cannot serialize it.
			return ((Feature) owner).getOwningMembership() instanceof EndFeatureMembership;
		} else if (SysMLPackage.Literals.ELEMENT__OWNED_RELATIONSHIP.equals(feature)) {
			Relationship relationship = ((Element) owner).getOwnedRelationship().get(index);
			if (owner instanceof Expression && relationship instanceof ReturnParameterMembership) {
				if (owner instanceof OperatorExpression) {
					OperatorExpression operatorExpression = (OperatorExpression) owner;
					String operator = operatorExpression.getOperator();
					return !"all".equals(operator) && !"as".equals(operator) && !"meta".equals(operator);
				}
				if (owner instanceof FeatureReferenceExpression) {
					// TODO Is this SelfReferenceExpression?
					return ((FeatureReferenceExpression) owner).getOwnedMembership().size() != 1;
				}
				return true;
			}
			if (owner instanceof InvocationExpression && relationship instanceof ParameterMembership) {
				ParameterMembership parameterMembership = (ParameterMembership) relationship;
				if (FeatureUtil.isInputParameter(parameterMembership.getOwnedMemberFeature())) {
					return true;
				}
			}
			if (relationship instanceof OwningMembership) {
				// Multiplicity inserted by FeatureUtil.
				return isAllTransient(relationship, SysMLPackage.Literals.RELATIONSHIP__OWNED_RELATED_ELEMENT);
			}
		} else if (SysMLPackage.Literals.RELATIONSHIP__OWNED_RELATED_ELEMENT.equals(feature)) {
			Element relatedElement = ((Relationship) owner).getOwnedRelatedElement().get(index);
			if (relatedElement instanceof Multiplicity && !(relatedElement instanceof MultiplicityRange)) {
				// Multiplicity is a concrete class, but only MultiplicityRange instances are
				// can be serialized. We just omit any Multiplicity instances that are not
				// MultiplicityRange, since such instances are only created by FeatureUtil when
				// a MultiplicityRange is missing.
				return true;
			}
		} else if (SysMLPackage.Literals.INVOCATION_EXPRESSION__OPERAND.equals(feature)) {
			return false;
		} else if (OWNING_RELATIONSHIP_TARGETS.contains(feature)) {
			Relationship subsetting = (Relationship) owner;
			// TODO Check whether the target is the first owned related element?
			return !subsetting.getOwnedRelatedElement().isEmpty();
		}
		return super.isTransient(owner, feature, index);
	}

	private boolean isAllTransient(EObject owner, EStructuralFeature feature) {
		if (!feature.isMany()) {
			return isTransient(owner, feature, -1);
		}
		@SuppressWarnings("rawtypes")
		int size = ((List) owner.eGet(feature)).size();
		for (int i = 0; i < size; i++) {
			if (!isTransient(owner, feature, i)) {
				return false;
			}
		}
		return true;
	}
}
