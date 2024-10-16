package mobi.chouette.service;

import java.io.InputStream;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import javax.inject.Named;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.file.FileStore;

import com.google.cloud.storage.Storage;
import org.rutebanken.helper.gcp.BlobStoreHelper;


/**
 * Store permanent files in Google Cloud Storage.
 */
@Singleton(name = GoogleCloudFileStore.BEAN_NAME)
@Named
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn(JobServiceManager.BEAN_NAME)
@Log4j
public class GoogleCloudFileStore implements FileStore {

	public static final String BEAN_NAME = "googleCloudFileStore";

	@EJB
	private ContenerChecker checker;

	private Storage storage;

	private String containerName;

	private String baseFolder;


	@PostConstruct
	public void init() {
		baseFolder = System.getProperty(checker.getContext() + ".directory");
		containerName = System.getProperty(checker.getContext() + ".blobstore.gcs.container.name");
		String credentialPath = System.getProperty(checker.getContext() + ".blobstore.gcs.credential.path");
		String projectId = System.getProperty(checker.getContext() + ".blobstore.gcs.project.id");

		log.info("Initializing blob store service. ContainerName: " + containerName + ", credentialPath: " + credentialPath + ", projectId: " + projectId);

		if (credentialPath == null || credentialPath.isEmpty()) {
			// Use default gcp credentials
			storage = BlobStoreHelper.getStorage(projectId);
		} else {
			storage = BlobStoreHelper.getStorage(credentialPath, projectId);
		}
	}


	@Override
	public InputStream getFileContent(Path filePath) {
		return BlobStoreHelper.getBlob(storage, containerName, toGCSPath(filePath));
	}

	@Override
	public void writeFile(Path filePath, InputStream content) {
		BlobStoreHelper.createOrReplace(storage, containerName, toGCSPath(filePath), content, false);
	}

	@Override
	public void deleteFolder(Path folder) {
		BlobStoreHelper.deleteBlobsByPrefix(storage, containerName, toGCSPath(folder));
	}


	@Override
	public boolean exists(Path filePath) {
		return getFileContent(filePath) != null;
	}


	@Override
	public void createFolder(Path folder) {
		// Folders do not existing in GC storage
	}

	@Override
	public boolean delete(Path filePath) {
		return BlobStoreHelper.deleteBlobsByPrefix(storage, containerName, toGCSPath(filePath));
	}

	private String toGCSPath(Path path) {
		String withoutBaseFolder = path.toString().replaceFirst(baseFolder, "");
		if (withoutBaseFolder.startsWith("/")) {
			return withoutBaseFolder.replaceFirst("/", "");
		}
		return withoutBaseFolder;
	}

}
