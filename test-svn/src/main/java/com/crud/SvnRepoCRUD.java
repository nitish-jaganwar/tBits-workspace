package com.crud;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.crud.WorkingCopyManage.CommitEventHandler;

//import testsvn.WorkingCopyManage.CommitEventHandler;

// method for creating local repository 
public class SvnRepoCRUD {

	public static void CreateRepo(String tgtPath) {
		// TODO Auto-generated method

		try {
			SVNURL tgtURL = SVNRepositoryFactory.createLocalRepository(new File(tgtPath), true, false);
			System.out.println("Local Repository Created\nURL:" + tgtURL);

		} catch (SVNException e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) throws SVNException {
		String url = "file:///C:/Users/NITISH-PC/Desktop/Prop-SVN";
		String name = "nitish";
		String password = "1111";
		SVNRepository repository = getRepoConnection(url, name, password);
//		 getRepoProperties();
//		 getRepoHistory(repository);
//		 showRepoTree(repository);
//		 String filePath = "";
//		 CreateRepo("C:\\Users\\NITISH-PC\\Desktop\\DEMOSVN");
//		 getFileContentFromRepo(filePath, repository, url);
//		 addDirAndFile(repository);
//		 delFolderFromRepo(repository,"test1.txt");
//		 modifyFile(repository);
//		 copyDir(repository);
//		 addFile(repository,"test1.txt");
//		 addFolder(repository,"Demo1");
//		update(url, name, password);
//		checkout(url, name, password);
//		doCommit(name, password, "C:\\Users\\NITISH-PC\\Desktop\\Working-copy", "Add prop to Working Copy");
//		addProperties("Nitish", "123");
	}

	public static void showRepoTree(SVNRepository repository) throws SVNException {
		long startRevision = 0;
		long endRevision = -1; // HEAD (the latest) revision
		Collection logEntries = null;
		logEntries = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);
		for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
			SVNLogEntry logEntry = (SVNLogEntry) entries.next();
			System.out.println("---------------------------------------------");
			System.out.println("revision: " + logEntry.getRevision());
			System.out.println("author: " + logEntry.getAuthor());
			System.out.println("date: " + logEntry.getDate());
			System.out.println("log message: " + logEntry.getMessage());
			System.out.println(repository.getRevisionPropertyValue(logEntry.getRevision(), "A"));

			if (logEntry.getChangedPaths().size() > 0) {
				System.out.println();
				System.out.println("changed paths:");
				Set changedPathsSet = logEntry.getChangedPaths().keySet();

				for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
					SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
					System.out.println(" " + entryPath.getType() + " " + entryPath.getPath()
							+ ((entryPath.getCopyPath() != null) ? " (from " + entryPath.getCopyPath() + " revision "
									+ entryPath.getCopyRevision() + ")" : ""));
				}
			}
		}
		System.out.println("showRepoTree --- done");
	}

	public static SVNRepository getRepoConnection(String url, String name, String password) throws SVNException {
		FSRepositoryFactory.setup();
		SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
		repository.setAuthenticationManager(authManager);
		return repository;
	}

	public static void getRepoHistory(SVNRepository repository) {
		try {
			System.out.println("Repository Root: " + repository.getRepositoryRoot(true));
			System.out.println("Repository UUID: " + repository.getRepositoryUUID(true));

			long startRevision = 0;
			long endRevision = -1;
			Collection logEntries = null;

			logEntries = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);
			for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
				SVNLogEntry logEntry = (SVNLogEntry) entries.next();
				System.out.println("---------------------------------------------");
				System.out.println("revision: " + logEntry.getRevision());
				System.out.println("author: " + logEntry.getAuthor());
				System.out.println("date: " + logEntry.getDate());
				System.out.println("log message: " + logEntry.getMessage());

				if (logEntry.getChangedPaths().size() > 0) {
					System.out.println();
					System.out.println("changed paths:");
					Set changedPathsSet = logEntry.getChangedPaths().keySet();

					for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
						SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths()
								.get(changedPaths.next());
						System.out.println(" " + entryPath.getType() + " " + entryPath.getPath()
								+ ((entryPath.getCopyPath() != null) ? " (from " + entryPath.getCopyPath()
										+ " revision " + entryPath.getCopyRevision() + ")" : ""));
					}
				}
			}
			// Show all changes (revisions or commits)
			System.out.println("---------------------------------------------");
			long latestRevision = repository.getLatestRevision();
			System.out.println("Repository latest revision: " + latestRevision);
			System.out.println("---------------------------------------------");

		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void getFileContentFromRepo(String filePath, SVNRepository repository, String url)
			throws SVNException {
		SVNNodeKind nodeKind = repository.checkPath(filePath, -1);

		if (nodeKind == SVNNodeKind.NONE) {
			System.err.println("There is no entry at '" + url + "'.");
			System.exit(1);
		} else if (nodeKind == SVNNodeKind.DIR) {
			System.err.println("The entry at '" + url + "' is a directory while a file was expected.");
			System.exit(1);
		} else {
			System.out.println("file found");
		}
		System.out.println("-----------------------------------------");
		SVNProperties fileProperties = new SVNProperties();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		repository.getFile(filePath, -1, fileProperties, baos);

		String mimeType = (String) fileProperties.getStringValue(SVNProperty.MIME_TYPE);
		boolean isTextType = SVNProperty.isTextMimeType(mimeType);

		Iterator iterator = fileProperties.asMap().keySet().iterator();
		while (iterator.hasNext()) {
			String propertyName = (String) iterator.next();
			String propertyValue = (String) fileProperties.getStringValue(propertyName);
			System.out.println("File property: " + propertyName + "=" + propertyValue);
		}
		System.out.println();
		if (isTextType) {
			System.out.println("File contents:");
			System.out.println();
			try {
				baos.writeTo(System.out);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else {
			System.out.println("Not a text file.");
		}
		System.out.println("\n-----------------------------------------");

	}

	public static void delFolderFromRepo(SVNRepository repository, String dirPath) throws SVNException {

		ISVNEditor editor = repository.getCommitEditor("directory deleted", null);
		editor.openRoot(-1);
		// String dirPath = "t1.txt";
		editor.deleteEntry(dirPath, -1);
		editor.closeDir();
		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println(dirPath + " was deleted :" + commitInfo);
	}

	public static void addDirAndFile(SVNRepository repository) throws SVNException {

		long latestRevision = repository.getLatestRevision();
		System.out.println("Repository latest revision (before committing): " + latestRevision);

		byte[] data = "This is a new file".getBytes();
		String dirPath = "HOLKAR";
		String filePath = "Test1/college.txt";
		ISVNEditor editor = repository.getCommitEditor("directory and file added", null);

		editor.openRoot(-1);
		editor.addDir(dirPath, null, -1);
		editor.addFile(filePath, null, -1);
		editor.applyTextDelta(filePath, null);

		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true);

		editor.closeFile(filePath, checksum);
		editor.closeDir();
		editor.closeDir();

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("The directory was added: " + commitInfo);

	}

	public static void addFile(SVNRepository repository, String filePath) throws SVNException {

		long latestRevision = repository.getLatestRevision();
		System.out.println("Repository latest revision (before committing): " + latestRevision);

		byte[] data = "This is a new file".getBytes();

		// String filePath = "nitish.txt";
		ISVNEditor editor = repository.getCommitEditor("file added", null);

		editor.openRoot(-1);
		editor.addFile(filePath, null, -1);
		editor.applyTextDelta(filePath, null);

		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true);
		editor.closeFile(filePath, checksum);
		editor.closeDir();

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println(filePath + " was added: " + commitInfo);

	}

	public static void addFolder(SVNRepository repository, String dirPath) throws SVNException {

		long latestRevision = repository.getLatestRevision();
		System.out.println("Repository latest revision (before committing): " + latestRevision);

		// String dirPath = "Test3";

		ISVNEditor editor = repository.getCommitEditor("directory was added", null);

		editor.openRoot(-1);
		editor.addDir(dirPath, null, -1);
		editor.closeDir();
		editor.closeDir();

		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("The directory " + dirPath + " was added: " + commitInfo);

	}

	public static void modifyFile(SVNRepository repository) throws SVNException {

		// String dirPath = "Test1";
		String filePath = "test1.txt";

		byte[] newData = "This is the same file but modified a little.".getBytes();
		byte[] oldData = "This is a new file".getBytes();
		ISVNEditor editor = repository.getCommitEditor("File was modified", null);

		editor.openRoot(-1);
		// editor.openDir(dirPath, -1);
		editor.openFile(filePath, -1);
		editor.applyTextDelta(filePath, null);

		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

		String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(oldData), 0,
				new ByteArrayInputStream(newData), editor, true);

		editor.closeFile(filePath, checksum);
		editor.closeDir();
		// editor.closeDir();
		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("\n The file  was changed: " + " " + commitInfo);
	}

	public static void copyDir(SVNRepository repository) throws SVNException {

		String srcDirPath = "Test1";
		String dstDirPath = "Test2";
		long revision = repository.getLatestRevision();
		ISVNEditor editor = repository.getCommitEditor("Copied directory", null);
		editor.openRoot(-1);
		editor.addDir(dstDirPath, srcDirPath, revision);
		editor.closeDir();
		editor.closeDir();
		// return editor.closeEdit( );
		SVNCommitInfo commitInfo = editor.closeEdit();
		System.out.println("\n The directory was copied:" + " " + commitInfo);
	}

	public static void commit() throws SVNException {

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		ISVNEventHandler myCommitEventHandler;
		myCommitEventHandler = new CommitEventHandler();
		ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
		File wcPath = new File("C:\\Users\\NITISH-PC\\Desktop\\Demo_tuday\\Test1\\t33.txt");
		String commitMessage = "Add file";
		ourClientManager.getCommitClient().doCommit(new File[] { wcPath }, false, commitMessage, false, true);
	}

	public static void update(String url, String name, String password) throws SVNException {

		File wcPath = new File("D:\\SVN-practice\\Nitish\\Remote-Repos");
		FSRepositoryFactory.setup();
		SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
		repository.setAuthenticationManager(authManager);

		SVNClientManager ourClientManager;
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		// ISVNAuthenticationManager authManager =
		// SVNWCUtil.createDefaultAuthenticationManager();

		ourClientManager = SVNClientManager.newInstance(options, authManager);
		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

		updateClient.setIgnoreExternals(false);
		System.out.print("rev:");
		System.out.print(updateClient.doUpdate(wcPath, SVNRevision.HEAD, true));
		System.out.print(" Successfully updated ");
	}

	public static void checkout(String Url, String name, String password) throws SVNException {

		SVNClientManager ourClientManager;
		// SVNURL url = repositoryURL.appendPath( "MyRepos" , false );
		SVNURL url = SVNURL.parseURIEncoded(Url);
		File wcPath = new File("C:\\Users\\NITISH-PC\\Desktop\\Major-project -repo");
		FSRepositoryFactory.setup();

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);

		ourClientManager = SVNClientManager.newInstance(options, authManager);
		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		updateClient.setIgnoreExternals(false);
		System.out.println("Check Out Done at :" + wcPath);
		System.out
				.println("Revision:" + updateClient.doCheckout(url, wcPath, SVNRevision.HEAD, SVNRevision.HEAD, true));

	}

	public static void doCommit(String username, String password, String wcPath, String logMessage)
			throws SVNException {

		SVNClientManager clientManager = SVNClientManager.newInstance();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
		clientManager.setAuthenticationManager(authManager);

		SVNClientManager ourClientManager;
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);

		ourClientManager = SVNClientManager.newInstance(options, authManager);

		ISVNEventHandler myCommitEventHandler;

		myCommitEventHandler = new CommitEventHandler();
		ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
		File wPath = new File(wcPath);
		ourClientManager.getCommitClient().doCommit(new File[] { wPath }, false, logMessage, false, true);
		System.out.println("commit Successfully");

	}

	public static void getRepoProperties() throws SVNException {

		String url = "https://Nitish-PC/svn/Remote-Repos/";
		SVNRevision revision = SVNRevision.create(119);
		String propName = "svn:author";

		SVNURL repositoryUrl = SVNURL.parseURIEncoded(url);
		SVNRepository repository = SVNRepositoryFactory.create(repositoryUrl);
		repository.setAuthenticationManager(new BasicAuthenticationManager("nitish", "1111"));

		SVNNodeKind nodeKind = repository.checkPath("", revision.getNumber());
		if (nodeKind == SVNNodeKind.NONE) {
			System.err.println("Path not found: " + url);
			System.exit(1);
		} else if (nodeKind == SVNNodeKind.FILE) {
			System.err.println("Expected a directory, but found a file: " + url);
			System.exit(1);
		}

		Map<String, SVNPropertyValue> propValues = (Map<String, SVNPropertyValue>) repository
				.getRevisionProperties(revision.getNumber(), null);
		SVNPropertyValue propValue = propValues.get(propName);

		if (propValue == null) {
			System.out.println("Property not found: " + propName);
		} else {
			System.out.println(propValue.getString());
		}

	}

	public static void addProperties(String key, String value) throws SVNException {
		SVNClientManager clientManager = SVNClientManager.newInstance();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
		clientManager.setAuthenticationManager(authManager);
		SVNWCClient wcClient = clientManager.getWCClient();

		SVNPropertyValue propertyValue = SVNPropertyValue.create(value);
		File filePat = new File("C:\\Users\\NITISH-PC\\Desktop\\Working-copy");

		wcClient.doSetProperty(filePat, key, propertyValue, false, false, null);

		System.out.println("Properties added Successfully \n" + "{ Key = " + key + "  value =" + value + "}");

	}
}