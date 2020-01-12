/* Generated By:JJTree: Do not edit this line. ASTKeyValueNode.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.dexels.navajo.parser.compiled;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.dexels.navajo.expression.api.ContextExpression;
import com.dexels.navajo.expression.api.FunctionClassification;
import com.dexels.navajo.parser.NamedExpression;


public class ASTKeyValueNode extends SimpleNode {

    int args = 0;
    String val = "";

    ASTKeyValueNode(int id) {
    	super(id);
    }

  public ASTKeyValueNode(CompiledParser p, int id) {
    super(id);
  }

@Override
public ContextExpression interpretToLambda(List<String> problems, String originalExpression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {
	int num = jjtGetNumChildren();
	if(num!=1) {
		problems.add("Incorrect # of params in named parameter");
	}
	ContextExpression exp = jjtGetChild(0).interpretToLambda(problems, originalExpression,functionClassifier,mapResolver);
	return new NamedExpression(val, exp);
}

}
/* JavaCC - OriginalChecksum=52c601dbc6b20940070dc38767537d98 (do not edit this line) */
