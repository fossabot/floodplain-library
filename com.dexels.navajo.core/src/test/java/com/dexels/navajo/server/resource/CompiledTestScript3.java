package com.dexels.navajo.server.resource;

import java.util.ArrayList;

import com.dexels.navajo.mapping.CompiledScript;
import com.dexels.navajo.mapping.GenericDependentResource;
import com.dexels.navajo.mapping.compiler.meta.AdapterFieldDependency;
import com.dexels.navajo.script.api.Access;
import com.dexels.navajo.script.api.Dependency;

public class CompiledTestScript3 extends CompiledScript {

	@Override
	public void execute(Access access) throws Exception {

	}

	@Override
	public void finalBlock(Access access) throws Exception {

	}

	@Override
	public void setValidations() {

	}

	@Override
	public ArrayList<Dependency> getDependentObjects() {
		ArrayList<Dependency> deps = new ArrayList<Dependency>();
		deps.add(new AdapterFieldDependency(-1,
				"com.dexels.navajo.server.resource.ResourceTestAdapter",
				"whatever", "'id1'"));
		deps.add(new AdapterFieldDependency(-1,
				"com.dexels.navajo.adapter.NavajoMap",
				GenericDependentResource.SERVICE_DEPENDENCY,
				"'CompiledTestScript4'"));
		return deps;
	}

}
