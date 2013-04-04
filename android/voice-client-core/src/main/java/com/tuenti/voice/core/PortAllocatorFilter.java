package com.tuenti.voice.core;

public enum PortAllocatorFilter {

	NO_FILTER, 
	TURN;

	private static final PortAllocatorFilter[] filterValues = PortAllocatorFilter
			.values();

	public static PortAllocatorFilter fromInteger(int i) {
		return filterValues[i];
	}

}