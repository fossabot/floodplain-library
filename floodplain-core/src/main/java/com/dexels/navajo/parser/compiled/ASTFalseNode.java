/* Generated By:JJTree&JavaCC: Do not edit this line. ASTFalseNode.java */
package com.dexels.navajo.parser.compiled;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.Property;
import com.dexels.navajo.expression.api.ContextExpression;
import com.dexels.navajo.expression.api.FunctionClassification;
import com.dexels.navajo.script.api.MappableTreeNode;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class ASTFalseNode extends SimpleNode {
    ASTFalseNode(int id) {
        super(id);
    }

	@Override
	public ContextExpression interpretToLambda(List<String> problems, String expression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {
		return new ContextExpression() {
			
			@Override
			public boolean isLiteral() {
				return true;
			}
			
			@Override
			public Operand apply(MappableTreeNode mapNode, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		        return Operand.ofBoolean(false);
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
