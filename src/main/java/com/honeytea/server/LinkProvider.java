package com.honeytea.server;

import java.util.Collection;

public interface LinkProvider {

	String getName();

	Collection<String> fetchLinks();

}
