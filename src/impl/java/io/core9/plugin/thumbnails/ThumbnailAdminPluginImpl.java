package io.core9.plugin.thumbnails;

import io.core9.plugin.filesmanager.FileRepository;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Request;

import java.util.HashMap;
import java.util.Map;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

@PluginImplementation
public class ThumbnailAdminPluginImpl implements ThumbnailAdminPlugin {
	
	@InjectPlugin
	private ThumbnailPlugin thumbnails;
	
	@InjectPlugin
	private FileRepository files;

	@Override
	public String getControllerName() {
		return "images";
	}

	@Override
	public void handle(Request request) {
		String type = (String) request.getParams().get("type");
		String name = (String) request.getParams().get("id");
		switch(type) {
		case "refresh":
			createProfiles(request.getVirtualHost());
			request.getResponse().end();
			break;
		case "flush":
			if(name != null) {
				VirtualHost vhost = request.getVirtualHost();
				ImageProfile profile = getProfile(vhost, name);
				flushProfile(vhost, profile);
			} else {
				flushProfiles(request.getVirtualHost());
			}
			request.getResponse().end();
			break;
		default:
			break;
		}		
	}
	
	private ImageProfile getProfile(VirtualHost vhost, String name) {
		return thumbnails.getVirtualHostRegistry(vhost).get(name);
	}
	
	/**
	 * Create the profiles for a virtual host
	 * @param vhost
	 */
	private void createProfiles(VirtualHost vhost) {
		thumbnails.createProfiles(vhost);
	}
	
	/**
	 * Flush all image profiles for a vhost
	 * @param vhost
	 */
	private void flushProfiles(VirtualHost vhost) {
		for(ImageProfile profile : thumbnails.getVirtualHostRegistry(vhost).values()) {
			flushProfile(vhost, profile);
		}
	}
	
	/**
	 * Flush an image profile (by name)
	 * @param vhost
	 * @param name
	 */
	private void flushProfile(VirtualHost vhost, ImageProfile profile) {
		Map<String,Object> query = new HashMap<String, Object>();
		query.put("metadata.profile", profile.getName());
		files.removeFiles(profile.retrieveDatabase(vhost), profile.retrieveBucket(), query);
	}
}
