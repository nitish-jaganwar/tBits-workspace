package com.svn.crud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class AdvancedSvnManager {

	private static final String REPO_PHYSICAL_PATH = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\SVN\\repo1";
	private static final String REPO_URL = "file:///C:/Users/NITISH%20JAGANWAR/Desktop/test/SVN/repo1";
	private static final String USERNAME = "nitish";
	private static final String PASSWORD = "123";

	public static void main(String[] args) throws Exception {

//        STEP 1: Create Repo (Run once, then comment out)
		// createLocalRepo(REPO_PHYSICAL_PATH);

		// STEP 2: Connect
		System.out.println("Connecting to repo...");
		SVNRepository repository = connectToRepo(REPO_URL, USERNAME, PASSWORD);
		System.out.println("Connected successfully!\n");

		// STEP 3: Multiple Files in ONE Commit + Metadata Properties

		Map<String, String> newFiles = new HashMap<>();
		newFiles.put("document1.txt", "Hello from doc 1!");
		newFiles.put("document2.txt", "Hello from doc 2!");

		Map<String, String> properties = new HashMap<>();
		properties.put("custom-author", "Nitish");
		properties.put("custom-uuid", UUID.randomUUID().toString()); // Unique ID

		// commitMultipleNewFiles(repository, newFiles, properties, "Initial commit with
		// 2 files and metadata");

		// addFolder(repository, "MyFolder1");
		// STEP 4: Read File Properties (Metadata)
		readFileMetadata(repository, "document1.txt");

		// STEP 5: Get History by Time Frame (e.g., changes in the last 1 hour)

		Calendar cal = Calendar.getInstance();
		Date endDate = cal.getTime(); // Now
		cal.add(Calendar.HOUR, -1);
		Date startDate = cal.getTime(); // 1 Hour ago

		getHistoryByTimeFrame(repository, startDate, endDate);

		readRepoHistoryWithDetails(repository);
		// deleteEntry(repository, "document2.txt");

		// STEP 6: Complete Audit History
		readCompleteAuditHistory(repository);
	}

	// -----------------------------------------------------------------------
	// 1. SETUP & CONNECTION
	// -----------------------------------------------------------------------

	public static void createLocalRepo(String physicalPath) throws SVNException {
		SVNURL url = SVNRepositoryFactory.createLocalRepository(new File(physicalPath), true, false);
		System.out.println("Repository created at: " + url);
	}

	public static void readRepoHistoryWithDetails(SVNRepository repository) {

		System.out.println("--- READ: Repository History (Detailed) ---");

		try {
			System.out.println("Repo Root : " + repository.getRepositoryRoot(true));
			System.out.println("Repo UUID : " + repository.getRepositoryUUID(true));

			long startRevision = 0;
			long endRevision = -1; // HEAD

			Collection logEntries = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);

			for (Object obj : logEntries) {

				SVNLogEntry logEntry = (SVNLogEntry) obj;

				System.out.println("\n=========================================");
				System.out.println("Revision    : " + logEntry.getRevision());
				System.out.println("Author      : " + logEntry.getAuthor());
				System.out.println("Date        : " + logEntry.getDate());
				System.out.println("Log message : " + logEntry.getMessage());

				if (logEntry.getChangedPaths() != null && !logEntry.getChangedPaths().isEmpty()) {

					System.out.println("Changed Files:");

					for (Object pathKey : logEntry.getChangedPaths().keySet()) {

						SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(pathKey);

						String path = entryPath.getPath();
						char type = entryPath.getType();

						System.out.println("  [" + type + "] " + path);

						// 🔥 Fetch file properties (ONLY if not deleted)
						if (type != 'D') {
							printFileProperties(repository, path, logEntry.getRevision());
						}
					}
				}
			}

			System.out.println("\n--- READ DONE ---");

		} catch (SVNException e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	private static void printFileProperties(SVNRepository repository, String path, long revision) {

		try {
			String cleanPath = path.startsWith("/") ? path.substring(1) : path;

			SVNNodeKind nodeKind = repository.checkPath(cleanPath, revision);

			if (nodeKind != SVNNodeKind.FILE) {
				return; // skip folders
			}

			SVNProperties properties = new SVNProperties();

			repository.getFile(cleanPath, revision, properties, null);

			String size = properties.getStringValue(SVNProperty.WORKING_SIZE);
			String checksum = properties.getStringValue(SVNProperty.CHECKSUM);
			String mime = properties.getStringValue(SVNProperty.MIME_TYPE);

			System.out.println("       Size     : " + size);
			System.out.println("       Checksum : " + checksum);
			System.out.println("       MIME     : " + mime);

// Custom properties
			for (Object key : properties.nameSet()) {
				String propName = (String) key;

				if (!propName.equals(SVNProperty.WORKING_SIZE) && !propName.equals(SVNProperty.CHECKSUM)
						&& !propName.equals(SVNProperty.MIME_TYPE)) {

					System.out.println("       " + propName + " : " + properties.getStringValue(propName));
				}
			}

		} catch (SVNException e) {
			System.out.println("       [WARN] Could not fetch properties");
		}
	}

	public static SVNRepository connectToRepo(String url, String username, String password) throws SVNException {
		FSRepositoryFactory.setup();
		SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
		repository.setAuthenticationManager(authManager);
		return repository;
	}

	public static void deleteEntry(SVNRepository repository, String entryPath) throws SVNException {

		System.out.println("--- DELETE: Removing '" + entryPath + "' ---");

		ISVNEditor editor = repository.getCommitEditor("Deleted: " + entryPath, null);

		editor.openRoot(-1);
		editor.deleteEntry(entryPath, -1); // marks the entry for deletion
		editor.closeDir();

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("SUCCESS: '" + entryPath + "' deleted at " + commitInfo + "\n");
	}
	// -----------------------------------------------------------------------
	// 2. BATCH COMMIT (Multiple Files) & PROPERTIES
	// -----------------------------------------------------------------------

	/**
	 * Commits multiple new files in a SINGLE revision, avoiding "auto commit" per
	 * file. Also attaches custom properties (Metadata) to each file.
	 */
	public static void commitMultipleNewFiles(SVNRepository repository, Map<String, String> filesToAdd,
			Map<String, String> customProps, String commitMessage) throws SVNException {

		System.out.println("--- Starting Batch Commit ---");

		// Open Editor ONCE
		ISVNEditor editor = repository.getCommitEditor(commitMessage, null);
		editor.openRoot(-1);

		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

		for (Map.Entry<String, String> fileEntry : filesToAdd.entrySet()) {
			String filePath = fileEntry.getKey();
			byte[] fileData = fileEntry.getValue().getBytes();

			// Add File
			editor.addFile(filePath, null, -1);
			editor.applyTextDelta(filePath, null);
			String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(fileData), editor, true);

			// Add Custom Properties (Metadata)
			if (customProps != null) {
				for (Map.Entry<String, String> prop : customProps.entrySet()) {
					editor.changeFileProperty(filePath, prop.getKey(), SVNPropertyValue.create(prop.getValue()));
				}
			}

			editor.closeFile(filePath, checksum);
			System.out.println("Staged: " + filePath);
		}

		editor.closeDir(); // close root

		// Commit EVERYTHING together
		SVNCommitInfo info = editor.closeEdit();
		System.out.println("--- Generated File URLs ---");
		SVNURL repoRootUrl = repository.getLocation(); // Gets the base URL of your connection
		long newRevision = info.getNewRevision();
		for (String filePath : filesToAdd.keySet()) {
			// Append the file path to the root URL
			SVNURL exactFileUrl = repoRootUrl.appendPath(filePath, false);

			System.out.println("File: " + filePath);
			System.out.println("  -> Latest URL : " + exactFileUrl.toString());
			System.out.println("  -> Audit URL  : " + exactFileUrl.toString() + "@" + newRevision); // Task 8: Unique
																									// URL for this
																									// commit
		}
		System.out.println("---------------------------\n");
		System.out.println("SUCCESS: All files committed in Revision: " + info.getNewRevision() + "\n");
	}

	public static void addFolder(SVNRepository repository, String folderName) throws SVNException {

		System.out.println("--- CREATE: Adding folder '" + folderName + "' ---");

		long latestRevision = repository.getLatestRevision();
		System.out.println("Current revision before adding: " + latestRevision);

		// Open commit editor
		ISVNEditor editor = repository.getCommitEditor("Added folder: " + folderName, null);

		editor.openRoot(-1); // open root dir
		editor.addDir(folderName, null, -1); // add new folder inside root
		editor.closeDir(); // close the new folder
		editor.closeDir(); // close root

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("SUCCESS: Folder '" + folderName + "' added at " + commitInfo + "\n");
	}
	// -----------------------------------------------------------------------
	// 3. READ METADATA / PROPERTIES
	// -----------------------------------------------------------------------

	public static void readFileMetadata(SVNRepository repository, String filePath) throws SVNException {
		System.out.println("--- Reading Metadata for: " + filePath + " ---");

		SVNProperties properties = new SVNProperties();
		repository.getFile(filePath, -1, properties, null); // passing null for output stream as we only want properties

		for (String propName : properties.nameSet()) {
			System.out.println("Property Name: " + propName + " | Value: " + properties.getStringValue(propName));
		}
		System.out.println();
	}

	// -----------------------------------------------------------------------
	// 4. HISTORY BY TIME FRAME
	// -----------------------------------------------------------------------

	public static void getHistoryByTimeFrame(SVNRepository repository, Date startDate, Date endDate)
			throws SVNException {
		System.out.println("--- History from " + startDate + " to " + endDate + " ---");

		// Convert Dates to Revisions
		long startRev = repository.getDatedRevision(startDate);
		long endRev = repository.getDatedRevision(endDate);

		System.out.println("Resolved Revisions: r" + startRev + " to r" + endRev);

		if (startRev > endRev) {
			System.out.println("No changes in this time frame.");
			return;
		}

		Collection logEntries = repository.log(new String[] { "" }, null, startRev, endRev, true, true);

		for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
			SVNLogEntry logEntry = (SVNLogEntry) entries.next();
			System.out.println("Revision: " + logEntry.getRevision() + " | Date: " + logEntry.getDate());
			System.out.println("Message: " + logEntry.getMessage());

			if (logEntry.getChangedPaths() != null) {
				for (Object pathEntry : logEntry.getChangedPaths().values()) {
					SVNLogEntryPath path = (SVNLogEntryPath) pathEntry;
					System.out.println("  [" + path.getType() + "] " + path.getPath());
				}
			}
			System.out.println("-");
		}
	}

	// -----------------------------------------------------------------------
	// 5. COMPLETE AUDIT (Same as yours, cleaned up)
	// -----------------------------------------------------------------------

	public static void readCompleteAuditHistory(SVNRepository repository) throws SVNException {
		System.out.println("--- Complete Audit History ---");
		Collection logEntries = repository.log(new String[] { "" }, null, 0, -1, true, true);

		for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
			SVNLogEntry logEntry = (SVNLogEntry) entries.next();
			System.out.println(
					"[r" + logEntry.getRevision() + "] " + logEntry.getAuthor() + " - " + logEntry.getMessage());

			if (logEntry.getChangedPaths() != null) {
				for (Object pathEntry : logEntry.getChangedPaths().values()) {
					SVNLogEntryPath path = (SVNLogEntryPath) pathEntry;
					System.out.println("  -> " + path.getType() + " " + path.getPath());
				}
			}
		}
		System.out.println("--- END ---");
	}
}