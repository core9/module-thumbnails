package io.core9.plugin.thumbnails;

public class ImageProfile {

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
	
	public ImageProfile(int width, int height) {
		this.width = width;
		this.height = height;
	}

}
