package com.svn.crud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * SvnRepoCRUD.java
 *
 * Demonstrates full CRUD operations on a LOCAL SVN repository using SVNKit.
 *
 * CRUD = Create, Read, Update, Delete
 *
 * Repo path : C:\Users\NITISH JAGANWAR\Desktop\test\SVN\repo Repo URL :
 * file:///C:/Users/NITISH%20JAGANWAR/Desktop/test/SVN/repo
 *
 * HOW TO RUN: Step 1 - Run main() with STEP 1 uncommented → creates the repo
 * folder Step 2 - Comment out STEP 1, uncomment STEP 2 → adds files/folders
 * Step 3 - Uncomment READ to see history Step 4 - Try UPDATE and DELETE one by
 * one
 */
public class SvnRepoCRUD {

	// -----------------------------------------------------------------------
	// REPO PATH CONSTANTS (change only these if your path changes)
	// -----------------------------------------------------------------------

	// Physical folder where the SVN repo will be created on your disk
	private static final String REPO_PHYSICAL_PATH = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\SVN\\repo";

	// URL format SVNKit uses to connect (spaces → %20)
	private static final String REPO_URL =
		    "file:///C:/Users/NITISH JAGANWAR/Desktop/test/SVN/repo";

	// Credentials (for local repos these can be anything)
	private static final String USERNAME = "nitish";
	private static final String PASSWORD = "123";

	// -----------------------------------------------------------------------
	// MAIN - Run operations one by one
	// -----------------------------------------------------------------------
	public static void main(String[] args) throws SVNException {

		// ===================================================================
		// STEP 1 : CREATE LOCAL REPOSITORY
		// Run this ONCE to create the repo folder.
		// After it succeeds, comment this line out before running again.
		// ===================================================================
		// createLocalRepo(REPO_PHYSICAL_PATH);

		// ===================================================================
		// STEP 2 : CONNECT TO THE REPOSITORY
		// Always required before calling any other method.
		// ===================================================================
		System.out.println("Connecting to repo...");
		SVNRepository repository = connectToRepo(REPO_URL, USERNAME, PASSWORD);
		System.out.println("Connected successfully!\n");

		// ===================================================================
		// STEP 3 : CREATE - Add a file to the repo
		// ===================================================================
//		addFile(repository, "hello.txt", "Hello! This is my first SVN file.");
//		addFile(repository, "notes.txt", "These are my notes.");

		// ===================================================================
		// STEP 4 : CREATE - Add a folder to the repo
		// ===================================================================
		//addFolder(repository, "MyFolder");

		// ===================================================================
		// STEP 5 : READ - View full history of the repo
		// ===================================================================
		readRepoHistory(repository);

		// ===================================================================
		// STEP 6 : READ - View content of a specific file
		// ===================================================================
		//readFileContent("hello.txt", repository);

		// ===================================================================
		// STEP 7 : UPDATE - Modify an existing file
		// oldContent must EXACTLY match what is currently in the file
		// ===================================================================
//		updateFile(repository, "hello.txt", "Hello! This is my first SVN file.", // current content
//				"Hello! This file has been updated now." // new content
//		);

		// ===================================================================
		// STEP 8 : DELETE - Remove a file from the repo
		// ===================================================================
		 //deleteEntry(repository, "notes.txt");
		 readRepoHistory(repository);
		// ===================================================================
		// STEP 9 : READ again - Confirm all changes are in history
		// ===================================================================
		//readRepoHistory(repository);
	}

	// -----------------------------------------------------------------------
	// CREATE REPO
	// -----------------------------------------------------------------------

	/**
	 * Creates a brand new local SVN repository at the given folder path. Run this
	 * only ONCE. If folder already exists it will throw an error.
	 *
	 * @param physicalPath Full path on disk e.g. C:\Users\...\repo
	 */
	public static void createLocalRepo(String physicalPath) {
		try {
			System.out.println("Creating local SVN repository at: " + physicalPath);
			SVNURL url = SVNRepositoryFactory.createLocalRepository(new File(physicalPath), true, // enableRevisionProperties
																									// - allows storing
																									// extra revision
																									// info
					false // force - don't overwrite if already exists
			);
			System.out.println("Repository created successfully!");
			System.out.println("URL: " + url + "\n");
		} catch (SVNException e) {
			System.err.println("ERROR creating repo: " + e.getMessage());
		}
	}

	// -----------------------------------------------------------------------
	// CONNECT
	// -----------------------------------------------------------------------

	/**
	 * Connects to an existing SVN repository and returns the connection object. All
	 * CRUD operations need this connection (SVNRepository) to work.
	 *
	 * @param url      Repository URL e.g. file:///C:/path/to/repo
	 * @param username Any name for local repos
	 * @param password Any password for local repos
	 * @return SVNRepository connection object
	 */
	public static SVNRepository connectToRepo(String url, String username, String password)
	        throws SVNException {

	    FSRepositoryFactory.setup();

	    // replace space with %20 ONCE, then use parseURIEncoded
	    SVNRepository repository = SVNRepositoryFactory.create(
	        SVNURL.parseURIEncoded(url.replace(" ", "%20"))
	    );

	    ISVNAuthenticationManager authManager =
	        SVNWCUtil.createDefaultAuthenticationManager(username, password);
	    repository.setAuthenticationManager(authManager);

	    return repository;
	}

	// -----------------------------------------------------------------------
	// CREATE - Add File
	// -----------------------------------------------------------------------

	/**
	 * Adds a NEW file with given content directly into the SVN repository. This is
	 * a direct repo commit — no working copy needed.
	 *
	 * @param repository Active SVN connection
	 * @param fileName   Name of file to create e.g. "hello.txt"
	 * @param content    Text content to write into the file
	 */
	public static void addFile(SVNRepository repository, String fileName, String content) throws SVNException {

		System.out.println("--- CREATE: Adding file '" + fileName + "' ---");

		long latestRevision = repository.getLatestRevision();
		System.out.println("Current revision before adding: " + latestRevision);

		// Convert file content string to bytes
		byte[] fileData = content.getBytes();

		// Open a commit editor with a log message
		ISVNEditor editor = repository.getCommitEditor("Added file: " + fileName, null);

		// Open the root directory of the repo
		editor.openRoot(-1);

		// Tell SVNKit we are adding a new file (not editing existing one)
		editor.addFile(fileName, null, -1);

		// Apply the text content (delta = difference from empty to new content)
		editor.applyTextDelta(fileName, null);

		// SVNDeltaGenerator computes the binary diff and sends it
		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(fileName, new ByteArrayInputStream(fileData), editor, true);

		// Close the file and root, then commit
		editor.closeFile(fileName, checksum);
		editor.closeDir(); // close root

		SVNCommitInfo commitInfo = editor.closeEdit(); // this actually commits
		System.out.println("SUCCESS: '" + fileName + "' added at " + commitInfo + "\n");
	}

	// -----------------------------------------------------------------------
	// CREATE - Add Folder
	// -----------------------------------------------------------------------

	/**
	 * Adds a NEW empty folder (directory) into the SVN repository.
	 *
	 * @param repository Active SVN connection
	 * @param folderName Name of the folder to create e.g. "MyFolder"
	 */
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
	// READ - Repo History
	// -----------------------------------------------------------------------

	/**
	 * Reads and prints the full commit history of the repository. Shows: revision
	 * number, author, date, log message, changed files.
	 *
	 * @param repository Active SVN connection
	 */
	public static void readRepoHistory(SVNRepository repository) {
		System.out.println("--- READ: Repository History ---");
		try {
			System.out.println("Repo Root : " + repository.getRepositoryRoot(true));
			System.out.println("Repo UUID : " + repository.getRepositoryUUID(true));

			long startRevision = 0;
			long endRevision = -1; // -1 means HEAD (latest)

			// Fetch all log entries from revision 0 to HEAD
			Collection logEntries = repository.log(new String[] { "" }, // "" means root of repo
					null, startRevision, endRevision, true, // changedPath - include changed paths in each entry
					true // strictNode - be strict about node history
			);

			for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
				SVNLogEntry logEntry = (SVNLogEntry) entries.next();
				System.out.println("---------------------------------------------");
				System.out.println("Revision    : " + logEntry.getRevision());
				System.out.println("Author      : " + logEntry.getAuthor());
				System.out.println("Date        : " + logEntry.getDate());
				System.out.println("Log message : " + logEntry.getMessage());

				// Show which files/folders changed in this revision
				if (logEntry.getChangedPaths().size() > 0) {
					System.out.println("Changed paths:");
					Set changedPathsSet = logEntry.getChangedPaths().keySet();
					for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
						SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths()
								.get(changedPaths.next());

						// entryPath.getType() returns A=Added, M=Modified, D=Deleted, R=Replaced
						System.out.println("  [" + entryPath.getType() + "] " + entryPath.getPath());
					}
				}
			}

			long latestRevision = repository.getLatestRevision();
			System.out.println("---------------------------------------------");
			System.out.println("Latest revision: " + latestRevision);
			System.out.println("--- READ DONE ---\n");

		} catch (SVNException e) {
			System.err.println("ERROR reading history: " + e.getMessage());
		}
	}

	// -----------------------------------------------------------------------
	// READ - File Content
	// -----------------------------------------------------------------------

	/**
	 * Reads and prints the content of a specific file from the repository.
	 *
	 * @param filePath   Path of file in repo e.g. "hello.txt" or
	 *                   "MyFolder/notes.txt"
	 * @param repository Active SVN connection
	 */
	public static void readFileContent(String filePath, SVNRepository repository) throws SVNException {

		System.out.println("--- READ: File content of '" + filePath + "' ---");

		// First check if the path exists and is a file (not a folder)
		SVNNodeKind nodeKind = repository.checkPath(filePath, -1);

		if (nodeKind == SVNNodeKind.NONE) {
			System.err.println("ERROR: No file found at path '" + filePath + "'");
			return;
		} else if (nodeKind == SVNNodeKind.DIR) {
			System.err.println("ERROR: '" + filePath + "' is a directory, not a file.");
			return;
		}

		// SVNProperties holds file metadata (mime-type etc.)
		SVNProperties fileProperties = new SVNProperties();

		// ByteArrayOutputStream captures the file content bytes
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// -1 means HEAD (latest) revision
		repository.getFile(filePath, -1, fileProperties, outputStream);

		// Check if the file is plain text
		String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
		boolean isText = SVNProperty.isTextMimeType(mimeType);

		if (isText || mimeType == null) {
			System.out.println("Content:");
			System.out.println(outputStream.toString());
		} else {
			System.out.println("(Binary file - cannot display content)");
		}

		System.out.println("--- READ FILE DONE ---\n");
	}

	// -----------------------------------------------------------------------
	// UPDATE - Modify File
	// -----------------------------------------------------------------------

	/**
	 * Updates (modifies) an existing file in the SVN repository.
	 *
	 * IMPORTANT: oldContent must EXACTLY match the current content of the file in
	 * the repository. SVNKit uses it to compute the diff (delta).
	 *
	 * @param repository Active SVN connection
	 * @param filePath   Path of file to modify e.g. "hello.txt"
	 * @param oldContent Current content of the file (must be exact)
	 * @param newContent New content to replace it with
	 */
	public static void updateFile(SVNRepository repository, String filePath, String oldContent, String newContent)
			throws SVNException {

		System.out.println("--- UPDATE: Modifying file '" + filePath + "' ---");

		byte[] oldData = oldContent.getBytes();
		byte[] newData = newContent.getBytes();

		// Open a commit editor
		ISVNEditor editor = repository.getCommitEditor("Modified file: " + filePath, null);

		editor.openRoot(-1); // open root
		editor.openFile(filePath, -1); // open EXISTING file (not addFile)
		editor.applyTextDelta(filePath, null);

		// sendDelta with both old and new data computes the binary difference
		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(oldData), 0, // old content
				new ByteArrayInputStream(newData), // new content
				editor, true);

		editor.closeFile(filePath, checksum);
		editor.closeDir(); // close root

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("SUCCESS: '" + filePath + "' updated at " + commitInfo + "\n");
	}

	// -----------------------------------------------------------------------
	// DELETE - Remove File or Folder
	// -----------------------------------------------------------------------

	/**
	 * Deletes a file OR folder from the SVN repository. The entry is permanently
	 * removed in a new revision (history is kept).
	 *
	 * @param repository Active SVN connection
	 * @param entryPath  Path to delete e.g. "hello.txt" or "MyFolder"
	 */
	public static void deleteEntry(SVNRepository repository, String entryPath) throws SVNException {

		System.out.println("--- DELETE: Removing '" + entryPath + "' ---");

		ISVNEditor editor = repository.getCommitEditor("Deleted: " + entryPath, null);

		editor.openRoot(-1);
		editor.deleteEntry(entryPath, -1); // marks the entry for deletion
		editor.closeDir();

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("SUCCESS: '" + entryPath + "' deleted at " + commitInfo + "\n");
	}
}