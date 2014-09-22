package io.core9.plugin.thumbnails;

import io.core9.plugin.server.VirtualHost;

import java.io.InputStream;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class GenerateThumbnailTask implements Delayed {

	private VirtualHost vhost;
	private ImageProfile profile;
	private InputStream original;
	private String path;
	private long origin;
	private long delay;

	public VirtualHost getVhost() {
		return vhost;
	}

	public void setVhost(VirtualHost vhost) {
		this.vhost = vhost;
	}

	public ImageProfile getProfile() {
		return profile;
	}

	public void setProfile(ImageProfile profile) {
		this.profile = profile;
	}

	public InputStream getOriginal() {
		return original;
	}

	public void setOriginal(InputStream original) {
		this.original = original;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public GenerateThumbnailTask(VirtualHost vhost, ImageProfile profile, InputStream original, String path, long delay) {
		this.origin = System.currentTimeMillis();
		this.vhost = vhost;
		this.profile = profile;
		this.original = original;
		this.path = path;
		this.delay = delay;
	}

	@Override
	public int compareTo(Delayed delayed) {
		if (delayed == this) {
			return 0;
		}
		if (delayed instanceof GenerateThumbnailTask) {
			long diff = delay - ((GenerateThumbnailTask) delayed).delay;
			return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
		}
		long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
		return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getPath() == null) ? 0 : getPath().hashCode())
				+ ((getProfile() == null) ? 0 : getProfile().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof GenerateThumbnailTask)) {
			return false;
		}

		final GenerateThumbnailTask other = (GenerateThumbnailTask) obj;
		if (other.getPath() != this.getPath()) {
			return false;
		} else if (other.getProfile() != this.getProfile()) {
			return false;
		}

		return true;
	}

}
