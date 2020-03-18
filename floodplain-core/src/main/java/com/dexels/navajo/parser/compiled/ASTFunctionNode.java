/* Generated By:JJTree&JavaCC: Do not edit this line. ASTFunctionNode.java */
package com.dexels.navajo.parser.compiled;

import com.dexels.config.runtime.RuntimeConfig;
import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.Message;
import com.dexels.navajo.document.Navajo;
import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.Selection;
import com.dexels.navajo.expression.api.*;
import com.dexels.navajo.functions.util.FunctionFactoryFactory;
import com.dexels.navajo.parser.NamedExpression;
import com.dexels.navajo.parser.compiled.api.CacheSubexpression;
import com.dexels.navajo.parser.compiled.api.ReactiveParseItem;
import com.dexels.navajo.reactive.api.Reactive;
import com.dexels.navajo.script.api.Access;
import com.dexels.navajo.script.api.MappableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


final class ASTFunctionNode extends SimpleNode {

	
	private static final Logger typechecklogger = LoggerFactory.getLogger("navajo.typecheck");

	
	private static final Logger logger = LoggerFactory.getLogger(ASTFunctionNode.class);

	String functionName;
	int args = 0;
	
	ASTFunctionNode(int id) {
		super(id);
	}
	
	private FunctionInterface getFunction() {
		return FunctionFactoryFactory.getInstance().getInstance(getClass().getClassLoader(), functionName);
	}
	
	@Override
	public ContextExpression interpretToLambda(List<String> problems,String expression, Function<String, FunctionClassification> functionClassifier, Function<String,Optional<Node>> mapResolver) {


		List<ContextExpression> unnamed = new LinkedList<>();
		// TODO make lazy?
		Map<String,ContextExpression> named = new HashMap<>();
		Map<String,ContextExpression> namedParam = new HashMap<>();

		for (int i = 0; i <jjtGetNumChildren(); i++) {
			Node sn = jjtGetChild(i);
			ContextExpression cn = sn.interpretToLambda(problems, expression,functionClassifier,mapResolver);
			if(cn instanceof NamedExpression) {
				NamedExpression ne = (NamedExpression)cn;
				if(ne.isParam) {
					namedParam.put(ne.name, ne.expression);
				} else {
					named.put(ne.name, ne.expression);
				}
				
			} else {
				unnamed.add(cn);
			}
		}
		
		FunctionClassification mode = functionClassifier.apply(functionName);
		switch (mode) {
			
			case REACTIVE_HEADER:
				
				break;
			case REACTIVE_SOURCE:
				return new ReactiveParseItem(functionName, Reactive.ReactiveItemType.REACTIVE_SOURCE, named, unnamed,namedParam, expression,this);
			case REACTIVE_TRANSFORMER:
				return new ReactiveParseItem(functionName, Reactive.ReactiveItemType.REACTIVE_TRANSFORMER, named, unnamed,namedParam, expression,this);
	
			case REACTIVE_REDUCER:
				return new ReactiveParseItem(functionName, Reactive.ReactiveItemType.REACTIVE_MAPPER, named, unnamed,namedParam, expression,this);
			case DEFAULT:
				default:
		}
		return resolveNormalFunction(unnamed, named, problems, expression);

	}
	
	private ContextExpression resolveNormalFunction(List<ContextExpression> l, Map<String, ContextExpression> named,
			List<String> problems, String expression) {
		FunctionInterface typeCheckInstance = getFunction();
		if(typeCheckInstance==null) {
			throw new NullPointerException("Function: "+functionName+" can not be resolved!");
		}

		try {
			List<String> typeProblems = typeCheckInstance.typeCheck(l,expression);
			if(!typeProblems.isEmpty() && RuntimeConfig.STRICT_TYPECHECK.getValue()!=null) {
				problems.addAll(typeProblems);
			}
		} catch (Throwable e2) {
			typechecklogger.error("Typechecker itself failed when parsing: "+expression+" function definition: "+typeCheckInstance+" Error: ", e2);
		}
		boolean isImmutable = typeCheckInstance.isPure() && l.stream().allMatch(e->e.isLiteral());

		ContextExpression dynamic = new ContextExpression() {
			
			@Override
			public boolean isLiteral() {
				// TODO also check named params
				return isImmutable;
			}
			
			@Override
			public Operand apply(MappableTreeNode mapNode,Optional<ImmutableMessage> immutableMessage, Optional<ImmutableMessage> paramMessage) {
				FunctionInterface f = getFunction();
				Map<String,Operand> resolvedNamed = named.entrySet().stream().collect(Collectors.toMap(e->e.getKey(),e->e.getValue().apply(mapNode,immutableMessage,paramMessage)));
				f.setNamedParameter(resolvedNamed);
				if(f instanceof StatefulFunctionInterface) {
					StatefulFunctionInterface sfi = (StatefulFunctionInterface)f;
					immutableMessage.ifPresent(sfi::setInputMessage);
					paramMessage.ifPresent(sfi::setParamMessage);
//					sfi.setInMessage(doc);
//					sfi.setCurrentMessage(parentMsg);
//					sfi.setAccess(access);
				}
				f.reset();
				l.stream()
					.map(e->{
						try {
							Operand evaluated = e.apply(mapNode,immutableMessage,paramMessage);
							if(evaluated==null) {
								logger.warn("Problematic expression returned null object. If you really insist, return an Operand.NULL. Evaluating expression: {}",expression);
								
							}
							return evaluated;
						} catch (TMLExpressionException e1) {
							throw new TMLExpressionException("Error parsing parameters for function: "+functionName, e1);
						}
					})
					.forEach(e->f.insertOperand(e));
				return f.evaluateWithTypeCheckingOperand();
			}

			@Override
			public Optional<String> returnType() {
				return typeCheckInstance.getReturnType();
			}
			@Override
			public String expression() {
				return expression;
			}
		};
		if(isImmutable && CacheSubexpression.getCacheSubExpression()) {
			Optional<String> returnType = dynamic.returnType();
			String immutablExpression = dynamic.expression();
			Operand resolved = dynamic.apply();
			return new ContextExpression() {
				
				@Override
				public Optional<String> returnType() {
					return returnType;
				}
				
				@Override
				public boolean isLiteral() {
					return true;
				}
				
				@Override
				public String expression() {
					return immutablExpression;
				}
				
				@Override
				public Operand apply(MappableTreeNode mapNode, Optional<ImmutableMessage> immutableMessage,
						Optional<ImmutableMessage> paramMessage) {
					return resolved;
				}
			};
		} else {
			return dynamic;
		}

	}

}
