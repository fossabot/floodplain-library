/* Generated By:JJTree&JavaCC: Do not edit this line. ASTAndNode.java */
package com.dexels.navajo.parser.compiled;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.*;
import com.dexels.navajo.expression.api.ContextExpression;
import com.dexels.navajo.expression.api.FunctionClassification;
import com.dexels.navajo.expression.api.TipiLink;
import com.dexels.navajo.script.api.Access;
import com.dexels.navajo.script.api.MappableTreeNode;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class ASTAndNode extends SimpleNode {
    ASTAndNode(int id) {
        super(id);
    }

	@Override
	public ContextExpression interpretToLambda(List<String> problems,String expression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {
		ContextExpression expA = jjtGetChild(0).interpretToLambda(problems,expression,functionClassifier,mapResolver);
		ContextExpression expB = jjtGetChild(1).interpretToLambda(problems,expression,functionClassifier,mapResolver);
		Optional<String> expressionA = expA.returnType();
		checkOrAdd("In AND expression the first expression is not a boolean but a "+expressionA.orElse("<unknown>"), problems, expB.returnType(), Property.BOOLEAN_PROPERTY);
		Optional<String> expressionB = expB.returnType();
		checkOrAdd("In AND expression the second expression is not a boolean but a "+expressionB.orElse("<unknown>"), problems, expB.returnType(), Property.BOOLEAN_PROPERTY);
		return new ContextExpression() {
			@Override
			public Operand apply(MappableTreeNode mapNode,Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
				Operand a = expA.apply(mapNode,immutableMessage,paramMessage);
				if(a==null) {
		        	return Operand.ofBoolean(Boolean.FALSE);
		        }
				Boolean ba = (Boolean)a.value;
		        if (!(ba.booleanValue())) {
	        		return Operand.ofBoolean(Boolean.FALSE);
				}
		        Operand b = expB.apply(mapNode,immutableMessage,paramMessage);
		        if(b==null) {
	        		return Operand.ofBoolean(Boolean.FALSE);
		        }
		        Boolean value = (Boolean)b.value;
				return Operand.ofBoolean(value!=null?value:Boolean.FALSE);
			}

			@Override
			public boolean isLiteral() {
				return expA.isLiteral() && expB.isLiteral();
			}

			@Override
			public Optional<String> returnType() {
				return Optional.of(Property.BOOLEAN_PROPERTY);
			}

			@Override
			public String expression() {
				return expression;
			}
		};
	}
}
