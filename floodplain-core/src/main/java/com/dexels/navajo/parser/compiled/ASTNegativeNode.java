/* Generated By:JJTree&JavaCC: Do not edit this line. ASTNegativeNode.java */
package com.dexels.navajo.parser.compiled;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.types.Money;
import com.dexels.navajo.document.types.Percentage;
import com.dexels.navajo.expression.api.ContextExpression;
import com.dexels.navajo.expression.api.FunctionClassification;
import com.dexels.navajo.expression.api.TMLExpressionException;


final class ASTNegativeNode extends SimpleNode {
    ASTNegativeNode(int id) {
        super(id);
    }

	private final Operand interpret(Operand ao) {
		Object a = ao.value;

        if (a instanceof String)
            return Operand.ofString("-" + ((String) a)); // this is just silly
        else if (a instanceof Integer)
            return Operand.ofInteger(0 - ((Integer) a).intValue());
        else if (a instanceof Double)
            return Operand.ofFloat(0 - ((Double) a).doubleValue());
        else if (a instanceof Money)
          return Operand.ofMoney(new Money(0 - ((Money) a).doubleValue()));
        else if (a instanceof Percentage)
          return Operand.ofPercentage(new Percentage( new Percentage(0 - ((Percentage) a).doubleValue())));
        else
          throw new TMLExpressionException("Illegal type encountered before negation");
    }

	@Override
	public ContextExpression interpretToLambda(List<String> problems, String expression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {
		return lazyFunction(problems,expression, a->interpret(a), Optional.empty(),functionClassifier,mapResolver);
	}

}