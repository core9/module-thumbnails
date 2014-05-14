package io.core9.plugin.thumbnails;

import io.core9.plugin.database.repository.AbstractCrudEntity;
import io.core9.plugin.database.repository.Collection;
import io.core9.plugin.database.repository.CrudEntity;
import io.core9.plugin.server.VirtualHost;

import java.util.HashMap;
import java.util.Map;

@Collection("configuration")
public class ImageProfile extends AbstractCrudEntity implements CrudEntity {

	private String database;
	private String bucket;
	private String name;
	private int width;
	private int height;

	/**
	 * Set the image profile width
	 * 
	 * @param width
	 * @return
	 */
	public ImageProfile setWidth(int width) {
		this.width = width;
		return this;
	}

	/**
	 * Get the profile width
	 * 
	 * @return
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Set the profile height
	 * 
	 * @param height
	 * @return
	 */
	public ImageProfile setHeight(int height) {
		this.height = height;
		return this;
	}

	/**
	 * Get the profile height
	 * 
	 * @return
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Return the profile name
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the profile name
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
	
	@Override
	public Map<String,Object> retrieveDefaultQuery() {
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("configtype", "imageprofile");
		return query;
	}

	public String retrieveDatabase(VirtualHost vhost) {
		if(database != null && !database.equals("")) {
			return database;
		}
		return (String) vhost.getContext("database");
	}
	
	public String retrieveBucket() {
		if(bucket != null && !bucket.equals("")) {
			return bucket;
		}
		return "imagecache";
	}
}
