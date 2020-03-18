package com.dexels.navajo.parser.compiled.api;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.*;
import com.dexels.navajo.expression.api.TMLExpressionException;
import com.dexels.navajo.expression.api.TipiLink;
import com.dexels.navajo.mapping.MappingUtils;
import com.dexels.navajo.parser.DefaultExpressionEvaluator;
import com.dexels.navajo.parser.Expression;
import com.dexels.navajo.script.api.Access;
import com.dexels.navajo.script.api.MappableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class CachedExpressionEvaluator extends DefaultExpressionEvaluator implements ExpressionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(CachedExpressionEvaluator.class);

	@Override
	public Operand evaluate(String clause, Navajo inMessage, Object mappableTreeNode, Message parent, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		ExpressionCache ce = ExpressionCache.getInstance();
		
		Object val;
		String type;
		try {
			val = ce.evaluate(clause,  (MappableTreeNode)mappableTreeNode, null,null,immutableMessage,paramMessage);
			type = MappingUtils.determineNavajoType(val);
			return new Operand(val, type, "");
		} catch (TMLExpressionException e) {
		    if (inMessage != null) {
                // Only log if we have useful context
                logger.error("TML parsing issue with expression: {} exception", clause, e );
            }
            throw new TMLExpressionException("TML parsing issue");
		}
	}

	@Override
	public Operand evaluate(String clause, Navajo inMessage, Object mappableTreeNode, Message parent,
			Message currentParam, Selection selection, Object tipiLink, Map<String,Object> params, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		try {
			ExpressionCache ce = ExpressionCache.getInstance();
			Access access = params == null? null : (Access)params.get(Expression.ACCESS);
			Operand val =ce.evaluate(clause, (MappableTreeNode)mappableTreeNode, (TipiLink) tipiLink, access,immutableMessage,paramMessage);
			if(val==null) {
				throw new TMLExpressionException("Clause resolved to null, shouldnt happen:  expression: "+clause);
			}
			return val;
		} catch (TMLExpressionException e) {
		    if (inMessage != null) {
		        // Only log if we have useful context
		        logger.error("TML parsing issue with expression: {} exception", clause, e );
		    }
			throw new TMLExpressionException(e.getMessage(), e);
		}
	}

	@Override
	public Operand evaluate(String clause, Navajo inMessage, Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
		try {
			ExpressionCache ce = ExpressionCache.getInstance();
			return ce.evaluate(clause, null,null, null, immutableMessage,paramMessage);
		} catch (TMLExpressionException e) {
		    if (inMessage != null) {
                // Only log if we have useful context
                logger.error("TML parsing issue with expression: {} exception", clause, e );
            }
            throw new TMLExpressionException("TML parsing issue");
		}
	}

}
