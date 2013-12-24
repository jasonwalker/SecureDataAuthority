package com.jmw.sda.dbProviders;

import com.jmw.sda.dbProviders.mapDBProvider.MapDBProvider;

public final class CurrentProvider {
	protected static IDatabase provider = new MapDBProvider();
	public static IDatabase get(){
		return provider;
	}

}
