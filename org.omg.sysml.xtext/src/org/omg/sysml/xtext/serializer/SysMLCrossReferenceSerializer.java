/*******************************************************************************
 * Copyright (c) 2008, 2018 itemis AG (http://www.itemis.eu) and others.
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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.serializer.diagnostic.ISerializationDiagnostic.Acceptor;
import org.eclipse.xtext.serializer.tokens.CrossReferenceSerializer;
import org.eclipse.xtext.serializer.tokens.SerializerScopeProviderBinding;
import org.omg.kerml.xtext.naming.KerMLQualifiedEffectiveNameProvider;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.OwningMembership;

import com.google.inject.Inject;

/**
 * Stub implementation for a {@link CrossReferenceSerializer} that does not
 * depend on the {@link IScopeProvider}.
 * 
 * {@link org.omg.sysml.xtext.scoping.SysMLScopeProvider} is extremely slow when
 * serializing a model, so we always output the fully qualified name instead of
 * looking into the scope. This may be incorrect for elements that have no fully
 * qualified names (but appear as local names in the scope) and for positions in
 * the grammar where a qualified name is not valid (but a simple name is valid).
 * 
 * Published under the Eclipse Public License - v 2.0, because of code copied
 * verbatim from {@link CrossReferenceSerializer}.
 * 
 * @author Moritz Eysholdt - Initial contribution and API of original
 *         {@link CrossReferenceSerializer}
 * @author Kristóf Marussy - Modifications for SysMl
 */
@SuppressWarnings("restriction")
public class SysMLCrossReferenceSerializer extends CrossReferenceSerializer {
	@Inject
	private KerMLQualifiedEffectiveNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@SerializerScopeProviderBinding
	private IScopeProvider scopeProvider;

	@Override
	public String serializeCrossRef(EObject semanticObject, CrossReference crossref, EObject target, INode node,
			Acceptor errors) {
		if ((target == null || target.eIsProxy()) && node != null)
			return tokenUtil.serializeNode(node);

		final EReference ref = GrammarUtil.getReference(crossref, semanticObject.eClass());
		final IScope scope = scopeProvider.getScope(semanticObject, ref);
		if (scope == null) {
			if (errors != null)
				errors.accept(diagnostics.getNoScopeFoundDiagnostic(semanticObject, crossref, target));
			return null;
		}

		if (target != null && target.eIsProxy()) {
			target = handleProxy(target, semanticObject, ref);
		}

		// TODO Replace once the performance of SysMLScopeProvider has been improved.
		QualifiedName qualifiedName = qualifiedNameProvider.getFullyQualifiedName(target);
		if (qualifiedName == null && target instanceof OwningMembership) {
			OwningMembership membership = (OwningMembership) target;
			qualifiedName = qualifiedNameProvider.getFullyQualifiedName(membership.getOwnedMemberElement());
		}
		URI targetURI = EcoreUtil2.getPlatformResourceOrNormalizedURI(target);
		if (qualifiedName == null || !validInScope(scope, qualifiedName, targetURI)) {
			if (!(target instanceof Element)) {
				throw new IllegalArgumentException("Unexpected cross reference: " + target);
			}
			Element element = (Element) target;
			String effectiveName = element.effectiveName();
			if (effectiveName == null) {
				throw new IllegalArgumentException("Element without effective name: " + target);
			}
			qualifiedName = qualifiedNameConverter.toQualifiedName(effectiveName);
			if (!validInScope(scope, qualifiedName, targetURI)) {
				throw new IllegalArgumentException("Effective name not valid in scope: " + effectiveName);
			}
		}
		return qualifiedNameConverter.toString(qualifiedName);
	}
	
	private boolean validInScope(IScope scope, QualifiedName qualifiedName, URI targetURI) {
		IEObjectDescription desc = scope.getSingleElement(qualifiedName);
		return desc != null && desc.getEObjectURI().equals(targetURI);
	}
}
