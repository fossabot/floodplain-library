package com.dexels.navajo.functions;


import com.dexels.navajo.document.Operand;
import com.dexels.navajo.expression.api.FunctionInterface;
import com.dexels.navajo.expression.api.TMLExpressionException;
import com.dexels.navajo.parser.Expression;

import java.util.List;


/**
 * Title:        Navajo
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      Dexels
 * @author Arjen Schoneveld en Martin Bergman
 * @version $Id$
 */

public final class FormatStringList extends FunctionInterface {

    public FormatStringList() {}

    @Override
	@SuppressWarnings("rawtypes")
	public final Object evaluate() throws com.dexels.navajo.expression.api.TMLExpressionException {
        Object a = this.getOperands().get(0);
        Object b = this.getOperands().get(1);

        if (a instanceof String)
            return a;
        if (!(a instanceof List))
            throw new TMLExpressionException("FormatStringList: invalid operand: " + a.getClass().getName());
        if (!(b instanceof String))
            throw new TMLExpressionException("FormatStringList: invalid operand: " + a.getClass().getName());
        List strings = (List) a;
        String sep = (String) b;
        StringBuffer result = new StringBuffer(20 * strings.size());

        for (int i = 0; i < strings.size(); i++) {
            String el = (String) strings.get(i);

            result.append(el);
            if (i < (strings.size() - 1))
                result.append(sep);
        }
        return result.toString();
    }

    @Override
	public String usage() {
        return "FormatStringList(list of Strings, separator). Example FormatStringList(\"{\"Navajo\", \"Dexels\"}\", \";\") returns \"Navajo;Dexels\"";
    }

    @Override
	public String remarks() {
        return "Turns a list of strings in a single string using supplied delimiter.";
    }

    public static void main(String args[]) throws Exception {
        String expr = "Contains({'Aap', 'Noot'},'Vuur')";
        Operand o = Expression.evaluate(expr);
        System.err.println("o = " + o.value);
    }
}
