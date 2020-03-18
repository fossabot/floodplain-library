/* Generated By:JJTree&JavaCC: Do not edit this line. ASTOrNode.java */
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

final class ASTOrNode extends SimpleNode {
    ASTOrNode(int id) {
        super(id);
    }
	@Override
	public ContextExpression interpretToLambda(List<String> problems, String expression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {
		ContextExpression expA = jjtGetChild(0).interpretToLambda(problems,expression,functionClassifier,mapResolver);
		checkOrAdd("Or expression failed, first expression is not a boolean but a "+expA.returnType().orElse("<unknown>"), problems, expA.returnType(), Property.BOOLEAN_PROPERTY);
		ContextExpression expB = jjtGetChild(1).interpretToLambda(problems,expression,functionClassifier,mapResolver);
		checkOrAdd("Or expression failed, second expression is not a boolean but a "+expB.returnType().orElse("<unknown>"), problems, expB.returnType(), Property.BOOLEAN_PROPERTY);
		return new ContextExpression() {
			
			@Override
			public boolean isLiteral() {
				return expA.isLiteral() && expB.isLiteral();
			}
			
			@Override
			public Operand apply(MappableTreeNode mapNode, TipiLink tipiLink, Access access, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		        Object a = expA.apply(mapNode,tipiLink,access,immutableMessage,paramMessage).value;
		        Boolean ba = (Boolean) a;
		        if(a==null) {
		        		ba = Boolean.FALSE;
		        }
		        if (ba.booleanValue())
		            return Operand.ofBoolean(true);

		        Object b = expB.apply(mapNode,tipiLink,access,immutableMessage,paramMessage).value;
		        Boolean bb = (Boolean) b;
		        if(b==null) {
		        		bb = Boolean.FALSE;
		        }

		        return Operand.ofBoolean(ba.booleanValue() || bb.booleanValue());
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
