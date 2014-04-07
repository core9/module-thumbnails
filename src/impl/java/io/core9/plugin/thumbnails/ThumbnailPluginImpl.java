package io.core9.plugin.thumbnails;

import io.core9.plugin.admin.plugins.AdminConfigRepository;
import io.core9.plugin.database.mongodb.MongoDatabase;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.server.vertx.VertxServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.coobird.thumbnailator.Thumbnails;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import com.google.common.io.ByteStreams;

// TODO DELETE CACHE WHEN PROFILE CHANGED
// TODO DELETE CACHE WHEN PROFILE DELETED

@PluginImplementation
public class ThumbnailPluginImpl implements ThumbnailPlugin {
	private static final byte[] DUMMY = new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, 0, 0, 0, 33, -7, 4, 1, 10, 0, 1, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 76, 1, 0, 59};
	private static final String BUCKET = "imagecache";
	
	//TODO Create separate registry
	private Map<VirtualHost, Map<String, ImageProfile>> profiles = new HashMap<VirtualHost, Map<String, ImageProfile>>();
	
	@InjectPlugin
	private AdminConfigRepository config;

	@InjectPlugin
	private VertxServer server;
	
	@InjectPlugin
	private MongoDatabase database;

	@Override
	public void execute() {
		server.use("/images/.*", new Middleware() {
			@Override
			public void handle(Request request) {
				try {
					sendImage(request);
				} catch (ProfileDoesntExistException | IOException e) {
					request.getResponse().setStatusCode(404);
					request.getResponse().setStatusMessage("Not found");
				}
			}
		});
	}

	/**
	 * Return the image (original or thumbnail)
	 * 
	 * @param profileName
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws ProfileDoesntExistException 
	 */
	public void sendImage(Request req) throws IOException, ProfileDoesntExistException {
		String profileName = (String) req.getParams().get("p");
		String filename = req.getPath().substring(7);
		if(profileName == null) {
			byte[] bin = null;
			try {
				bin = ByteStreams.toByteArray(database.getStaticFile((String) req.getVirtualHost().getContext("database"), "static", filename));	
				req.getResponse().sendBinary(bin);
			} catch (Exception e) {
				req.getResponse().setStatusCode(404);
				req.getResponse().setStatusMessage("File not found");
			}

			
		} else if(profileName.equals("d")) {
			req.getResponse().putHeader("Content-Type", "image/gif");
			req.getResponse().sendBinary(DUMMY);
		} else {
			String db = (String) req.getVirtualHost().getContext("database");
			InputStream in = database.getStaticFile(db, BUCKET, "/" + profileName + filename);
			if(in == null) {
				InputStream original = database.getStaticFile(db, "static", filename);
				generateThumbnail(req.getVirtualHost(), db, original, filename, profileName);
				in = database.getStaticFile(db, BUCKET, "/" + profileName + filename);
			}
			req.getResponse().sendBinary(ByteStreams.toByteArray(in));
		}
	}

	/**
	 * Generate an image from an original and a profile
	 * @throws IOException 
	 * @throws ProfileDoesntExistException 
	 */
	public void generateThumbnail(VirtualHost vhost, String db, InputStream original, String path, String profileName) throws IOException, ProfileDoesntExistException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageProfile profile = profiles.get(vhost).get(profileName);
			if(profile == null) {
				throw new ProfileDoesntExistException();
			}
			Thumbnails.of(original).size(profile.getWidth(), profile.getHeight()).toOutputStream(os);
			Map<String,Object> file = new HashMap<String, Object>();
			Map<String,Object> metadata = new HashMap<String, Object>();
			metadata.put("profile", profileName);
			file.put("filename", "/" + profileName + path);
			file.put("metadata", metadata);
			database.addStaticFile(db, BUCKET, file, new ByteArrayInputStream(os.toByteArray()));
		} catch (NullPointerException e) {
			throw new ProfileDoesntExistException();
		}
	}

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
				flushProfile(request.getVirtualHost(), name);
			} else {
				flushProfiles(request.getVirtualHost());
			}
			request.getResponse().end();
			break;
		default:
			break;
		}		
	}

	/**
	 * Flush an image profile (by name)
	 * @param vhost
	 * @param name
	 */
	private void flushProfile(VirtualHost vhost, String name) {
		String db = (String) vhost.getContext("database");
		Map<String,Object> metadata = new HashMap<String, Object>();
		metadata.put("profile", name);
		Map<String,Object> query = new HashMap<String, Object>();
		query.put("metadata", metadata);
		List<Map<String,Object>> files = database.queryStaticFiles(db, BUCKET, query);
		for(Map<String,Object> file : files) {
			database.removeStaticFile(db, BUCKET, (String) file.get("_id"));
		}
	}

	/**
	 * Flush all image profiles for a vhost
	 * @param vhost
	 */
	private void flushProfiles(VirtualHost vhost) {
		for(String profile : profiles.get(vhost).keySet()) {
			flushProfile(vhost, profile);
		}
	}

	@Override
	public void process(VirtualHost[] vhosts) {
		for(VirtualHost vhost : vhosts) {
			createProfiles(vhost);
		}
	}

	/**
	 * Create the profiles for a virtual host
	 * @param vhost
	 */
	private void createProfiles(VirtualHost vhost) {
		Map<String, ImageProfile> registry = profiles.get(vhost);
		if(registry == null) {
			profiles.put(vhost, new HashMap<String,ImageProfile>());
			registry = profiles.get(vhost);
		} else {
			registry.clear();
		}
		for(Map<String,Object> conf : config.getConfigList(vhost, "imageprofile")) {
			registry.put((String) conf.get("name"), new ImageProfile((Integer) conf.get("width"), (Integer) conf.get("height")));
		}
	}
}
