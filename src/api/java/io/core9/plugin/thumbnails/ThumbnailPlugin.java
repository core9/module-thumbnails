package io.core9.plugin.thumbnails;

import io.core9.core.executor.Executor;
import io.core9.core.plugin.Core9Plugin;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.VirtualHostProcessor;

import java.util.Map;

public interface ThumbnailPlugin extends Core9Plugin, Executor, VirtualHostProcessor {

	/**
	 * Return the imagesprofiles for a vhost
	 * @param vhost
	 * @return
	 */
	Map<String,ImageProfile> getVirtualHostRegistry(VirtualHost vhost);
	
	/**
	 * Create new profiles for a vhost
	 * @param vhost
	 */
	void createProfiles(VirtualHost vhost);
}
