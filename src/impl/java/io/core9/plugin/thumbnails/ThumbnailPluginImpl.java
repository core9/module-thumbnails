package io.core9.plugin.thumbnails;

import io.core9.plugin.database.repository.CrudRepository;
import io.core9.plugin.database.repository.NoCollectionNamePresentException;
import io.core9.plugin.database.repository.RepositoryFactory;
import io.core9.plugin.filesmanager.FileRepository;
import io.core9.plugin.filesmanager.handler.StaticFilesHandler;
import io.core9.plugin.server.Server;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.request.Request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.coobird.thumbnailator.Thumbnails;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.PluginLoaded;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import com.google.common.io.ByteStreams;

// TODO DELETE CACHE WHEN PROFILE CHANGED
// TODO DELETE CACHE WHEN PROFILE DELETED

@PluginImplementation
public class ThumbnailPluginImpl implements ThumbnailPlugin {
	private static final byte[] DUMMY = new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, 0, 0, 0, 33, -7, 4, 1, 10, 0, 1, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 76, 1, 0, 59};
	private CrudRepository<ImageProfile> profileRepository;
	
	private Map<VirtualHost, Map<String, ImageProfile>> profiles = new HashMap<VirtualHost, Map<String, ImageProfile>>();
		
	@PluginLoaded
	public void onRepositoryFactoryLoaded(RepositoryFactory factory) throws NoCollectionNamePresentException {
		profileRepository = factory.getRepository(ImageProfile.class);
	}
	
	@InjectPlugin
	private FileRepository fileRepository;
	
	@InjectPlugin
	private StaticFilesHandler staticHandler;
	
	@InjectPlugin
	private Server server;
	
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
		VirtualHost vhost = req.getVirtualHost();
		String profileName = (String) req.getParams().get("p");
		String filename = req.getPath().substring(7);
		if(profileName == null) {
			byte[] bin = null;
			try {
				Map<String,Object> contents = staticHandler.getFileContents(vhost, filename);
				InputStream in = (InputStream) contents.get("stream");
				bin = ByteStreams.toByteArray(in);	
				req.getResponse().sendBinary(bin);
				in.close();
			} catch (Exception e) {
				req.getResponse().setStatusCode(404);
				req.getResponse().setStatusMessage("File not found");
			}
		} else if(profileName.equals("d")) {
			req.getResponse().putHeader("Content-Type", "image/gif");
			req.getResponse().sendBinary(DUMMY);
		} else {
			InputStream in = retrieveImage(vhost, profiles.get(vhost).get(profileName), filename);
			if(in == null) {
				req.getResponse().setStatusCode(404);
			} else {
				req.getResponse().sendBinary(ByteStreams.toByteArray(in));
				in.close();
			}
		}
	}
	
	/**
	 * Retrieves the Image
	 * @param vhost
	 * @param profile
	 * @param filePath
	 * @return
	 * @throws IOException
	 * @throws ProfileDoesntExistException
	 */
	private InputStream retrieveImage(VirtualHost vhost, ImageProfile profile, String filePath) throws IOException, ProfileDoesntExistException {
		if(profile == null) {
			throw new ProfileDoesntExistException();
		}
		Map<String,Object> file = fileRepository.getFileContentsByName(profile.retrieveDatabase(vhost), profile.retrieveBucket(), "/" + profile.getName() + filePath);
		InputStream result = null;
		if(file == null) {
			file = staticHandler.getFileContents(vhost, filePath);
			if(file != null) {
				result = generateThumbnail(vhost, profile, (InputStream) file.get("stream"), filePath);
			}
		} else {
			result = (InputStream) file.get("stream");
		}
		return result;
	}

	/**
	 * Generate an image from an original and a profile
	 * @throws IOException 
	 * @throws ProfileDoesntExistException 
	 */
	public InputStream generateThumbnail(VirtualHost vhost, ImageProfile profile, InputStream original, String path) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Thumbnails.of(original).size(profile.getWidth(), profile.getHeight()).toOutputStream(os);
		String folder = "/" + profile.getName() + path.substring(0, path.lastIndexOf('/') + 1);
		String filename  = path.substring(path.lastIndexOf('/') + 1);
		Map<String,Object> file = new HashMap<String, Object>();
		file.put("filename", filename);
		
		fileRepository.ensureFolderExists(profile.retrieveDatabase(vhost), profile.retrieveBucket(), folder);
		Map<String,Object> metadata = new HashMap<String, Object>();
		metadata.put("profile", profile.getName());
		metadata.put("folder", folder);
		file.put("metadata", metadata);
		byte[] tempFile = os.toByteArray();
		fileRepository.addFile(profile.retrieveDatabase(vhost), profile.retrieveBucket(), file, new ByteArrayInputStream(tempFile));
		return new ByteArrayInputStream(tempFile);
	}

	@Override
	public void createProfiles(VirtualHost vhost) {
		List<ImageProfile> profiles = profileRepository.getAll(vhost);
		Map<String,ImageProfile> vhostProfiles = new HashMap<String,ImageProfile>();
		for(ImageProfile profile : profiles) {
			vhostProfiles.put(profile.getName(), profile);
		}
		this.profiles.put(vhost, vhostProfiles);
	}

	@Override
	public Map<String, ImageProfile> getVirtualHostRegistry(VirtualHost vhost) {
		return this.profiles.get(vhost);
	}

	@Override
	public void addVirtualHost(VirtualHost vhost) {
		createProfiles(vhost);
	}

	@Override
	public void removeVirtualHost(VirtualHost vhost) {
		this.profiles.remove(vhost);		
	}
}
