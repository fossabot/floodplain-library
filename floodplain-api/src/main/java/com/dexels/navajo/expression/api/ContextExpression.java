package com.dexels.navajo.expression.api;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.Message;
import com.dexels.navajo.document.Navajo;
import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.Selection;
import com.dexels.navajo.script.api.Access;
import com.dexels.navajo.script.api.MappableTreeNode;

import java.util.Optional;

public interface ContextExpression {

	public default Operand apply() {
		return apply(Optional.empty(),Optional.empty());
	}
	public default Operand apply(Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		return apply(null,null,null,immutableMessage,paramMessage);
	}


	public Operand apply(MappableTreeNode mapNode, TipiLink tipiLink, Access access, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage);
	public boolean isLiteral();
	public Optional<String> returnType();
	public String expression();

}
