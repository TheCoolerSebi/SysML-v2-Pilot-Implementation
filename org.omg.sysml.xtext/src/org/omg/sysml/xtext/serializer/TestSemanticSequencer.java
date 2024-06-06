package org.omg.sysml.xtext.serializer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;


//Lehet hogy AbstractSysMLSyntacticSequencer AbstractSysMLSemanticSequencer?
public class TestSemanticSequencer extends AbstractSysMLSyntacticSequencer {

	public void acceptAssignedCrossRefDatatype(RuleCall datatypeRC, String token, EObject value, int index,
			ICompositeNode node) {
		super.acceptAssignedCrossRefDatatype(datatypeRC, token, value, index, node);
		System.out.println(datatypeRC.getRule().getName() + " >>> " + value);
	}

	
	public void acceptAssignedCrossRefEnum(RuleCall enumRC, String token, EObject value, int index,
			ICompositeNode node) {
		super.acceptAssignedCrossRefEnum(enumRC, token, value, index, node);
		System.out.println(enumRC.getRule().getName() + " >>> " + value);
		
	}

	
	public void acceptAssignedCrossRefTerminal(RuleCall terminalRC, String token, EObject value, int index,
			ILeafNode node) {
		super.acceptAssignedCrossRefTerminal(terminalRC, token, value, index, node);
		System.out.println(terminalRC.getRule().getName() + " >>> " + value);
	}

	
	public void acceptAssignedCrossRefKeyword(Keyword kw, String token, EObject value, int index, ILeafNode node) {
		System.out.println("kw: "+ kw);
		super.acceptAssignedCrossRefKeyword(kw, token, value, index, node);
	}

	
	public void acceptAssignedDatatype(RuleCall datatypeRC, String token, Object value, int index,
			ICompositeNode node) {
		super.acceptAssignedDatatype(datatypeRC, token, value, index, node);
		System.out.println(datatypeRC.getRule().getName() + " >>> " + value);
		
	}

	
	public void acceptAssignedEnum(RuleCall enumRC, String token, Object value, int index, ICompositeNode node) {
		super.acceptAssignedEnum(enumRC, token, value, index, node);
		System.out.println(enumRC.getRule().getName() + " >>> " + value);
		
	}

	
	public void acceptAssignedKeyword(Keyword keyword, String token, Object value, int index, ILeafNode node) {
		super.acceptAssignedKeyword(keyword, token, value, index, node);
		System.out.println("kw: "+ keyword);
		
	}


	public void acceptAssignedTerminal(RuleCall terminalRC, String token, Object value, int index, ILeafNode node) {
		super.acceptAssignedTerminal(terminalRC, token, value, index, node);
		System.out.println(terminalRC.getRule().getName() + " >>> " + value);
		
	}

	
	public boolean enterAssignedAction(Action action, EObject semanticChild, ICompositeNode node) {
		System.out.println("enter action: "+ action);
		return super.enterAssignedAction(action, semanticChild, node);
	}


	public boolean enterAssignedParserRuleCall(RuleCall rc, EObject semanticChild, ICompositeNode node) {
		System.out.println("enter "+rc.getRule().getName() + " >>> " + semanticChild);
		return super.enterAssignedParserRuleCall(rc, semanticChild, node);
	}

	
	public void finish() {
		super.finish();
		
	}

	
	public void leaveAssignedAction(Action action, EObject semanticChild) {
		System.out.println("leave action: "+ action);
		super.leaveAssignedAction(action, semanticChild);
		
	}


	public void leaveAssignedParserRuleCall(RuleCall rc, EObject semanticChild) {
		super.leaveAssignedParserRuleCall(rc, semanticChild);
		System.out.println("leave " + rc.getRule().getName() + " >>> " + semanticChild);
		
	}

}
