package com.dexels.navajo.document.typecheck;

import com.dexels.navajo.document.Property;
import com.dexels.navajo.document.PropertyTypeException;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public abstract class TypeChecker {
  public abstract String verify(Property p, String value) throws PropertyTypeException;
  public abstract String getType();

}