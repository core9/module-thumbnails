package io.core9.plugin.thumbnails;

import io.core9.plugin.database.repository.Collection;
import io.core9.plugin.features.processors.ConfigEntity;
import io.core9.plugin.server.VirtualHost;

import java.util.HashMap;
import java.util.Map;

@Collection("configuration")
public class ImageProfile extends ConfigEntity {

	private static final long serialVersionUID = -5161036626378452804L;

	/**
	 * Set the image profile width
	 * 
	 * @param width
	 * @return
	 */
	public ImageProfile setWidth(int width) {
		this.put("width", width);
		return this;
	}

	/**
	 * Get the profile width
	 * 
	 * @return
	 */
	public int getWidth() {
		return (int) this.get("width");
	}

	/**
	 * Set the profile height
	 * 
	 * @param height
	 * @return
	 */
	public ImageProfile setHeight(int height) {
		this.put("height", height);
		return this;
	}

	/**
	 * Get the profile height
	 * 
	 * @return
	 */
	public int getHeight() {
		return (int) this.get("height");
	}
	
	/**
	 * Return the profile name
	 * @return
	 */
	public String getName() {
		return (String) this.get("name");
	}

	/**
	 * Set the profile name
	 * @param name
	 */
	public void setName(String name) {
		this.put("name", name);
	}
	
	public String getDatabase() {
		return (String) this.get("database");
	}

	public void setDatabase(String database) {
		this.put("database", database);
	}

	public String getBucket() {
		return (String) this.get("bucket");
	}

	public void setBucket(String bucket) {
		this.put("bucket", bucket);
	}
	
	@Override
	public Map<String,Object> retrieveDefaultQuery() {
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("configtype", "imageprofile");
		return query;
	}

	public String retrieveDatabase(VirtualHost vhost) {
		return (String) this.getOrDefault("database", vhost.getContext("database"));
	}
	
	public String retrieveBucket() {
		return (String) this.getOrDefault("bucket", "imagecache");
	}
}
