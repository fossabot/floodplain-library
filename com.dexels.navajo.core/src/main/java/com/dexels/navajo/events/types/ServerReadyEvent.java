package com.dexels.navajo.events.types;

import com.dexels.navajo.document.Navajo;
import com.dexels.navajo.events.NavajoEvent;

public class ServerReadyEvent implements NavajoEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6663557291877696973L;

	@Override
	public Navajo getEventNavajo() {
		return null;
	}
	
	@Override
    public boolean isSynchronousEvent() {
        return false;
    }

}