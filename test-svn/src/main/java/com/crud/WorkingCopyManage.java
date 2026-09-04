package com.crud;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class WorkingCopyManage {

	private static SVNClientManager ourClientManager;
	private static ISVNEventHandler myCommitEventHandler;
	private static ISVNEventHandler myUpdateEventHandler;
	private static ISVNEventHandler myWCEventHandler;

	public static void main(String[] args) throws SVNException {
		 FSRepositoryFactory.setup();
		DAVRepositoryFactory.setup();
		SVNURL repositoryURL = null;
		try {
			repositoryURL = SVNURL.parseURIEncoded("file:///C:/Users/NITISH-PC/Desktop/New folder");
		} catch (SVNException e) {
			//
		}

		String myWorkingCopyPath = "file:///C:/Users/NITISH-PC/Desktop/newWorking";
		String importDir = "/importDir";
		String importFile = importDir + "/importFile.txt";
		String importFileText = "This unversioned file is imported into a repository";
		String newDir = "/newDir";
		String newFile = newDir + "/newFile.txt";
		String fileText = "This is a new file added to the working copy";
		SVNURL url = repositoryURL.appendPath("MyRepos", false);
		SVNURL copyURL = repositoryURL.appendPath("MyReposCopy", false);
		SVNURL importToURL = url.appendPath(importDir, false);

		myCommitEventHandler = new CommitEventHandler();

		myUpdateEventHandler = new UpdateEventHandler();

		myWCEventHandler = new WCEventHandler();

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);

		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();

		ourClientManager = SVNClientManager.newInstance(options, authManager);
		ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
		ourClientManager.getUpdateClient().setEventHandler(myUpdateEventHandler);
		ourClientManager.getWCClient().setEventHandler(myWCEventHandler);
		long committedRevision = -1;
		System.out.println("Making a new directory at '" + url + "'...");
		try {

			committedRevision = makeDirectory(url, "making a new directory at '" + url + "'").getNewRevision();
		} catch (SVNException svne) {
			error("error while making a new directory at '" + url + "'", svne);
		}
		System.out.println("Committed to revision " + committedRevision);
		System.out.println();

		File anImportDir = new File(importDir);
		File anImportFile = new File(anImportDir, SVNPathUtil.tail(importFile));

		createLocalDir(anImportDir, new File[] { anImportFile }, new String[] { importFileText });

		System.out.println("Importing a new directory into '" + importToURL + "'...");
		try {

			boolean isRecursive = true;
			committedRevision = importDirectory(anImportDir, importToURL,
					"importing a new directory '" + anImportDir.getAbsolutePath() + "'", isRecursive).getNewRevision();
		} catch (SVNException svne) {
			error("error while importing a new directory '" + anImportDir.getAbsolutePath() + "' into '" + importToURL
					+ "'", svne);
		}
		System.out.println("Committed to revision " + committedRevision);
		System.out.println();

		File wcDir = new File(myWorkingCopyPath);
		if (wcDir.exists()) {
			error("the destination directory '" + wcDir.getAbsolutePath() + "' already exists!", null);
		}
		wcDir.mkdirs();

		System.out.println("Checking out a working copy from '" + url + "'...");
		try {
			checkout(url, SVNRevision.HEAD, wcDir, true);
		} catch (SVNException svne) {
			error("error while checking out a working copy for the location '" + url + "'", svne);
		}
		System.out.println();

		/*
		 * recursively displays info for wcDir at the current working revision in the
		 * manner of 'svn info -R' command
		 */

		File aNewDir = new File(wcDir, newDir);
		File aNewFile = new File(aNewDir, SVNPathUtil.tail(newFile));
		/*
		 * creates a new local directory - 'wcDir/newDir' and a new file -
		 * '/MyWorkspace/newDir/newFile.txt'
		 */
		createLocalDir(aNewDir, new File[] { aNewFile }, new String[] { fileText });

		System.out
				.println("Recursively scheduling a new directory '" + aNewDir.getAbsolutePath() + "' for addition...");
		try {
			/*
			 * recursively schedules aNewDir for addition
			 */
			addEntry(aNewDir);
		} catch (SVNException svne) {
			error("error while recursively adding the directory '" + aNewDir.getAbsolutePath() + "'", svne);
		}
		System.out.println();

		boolean isRecursive = true;
		boolean isRemote = true;
		boolean isReportAll = false;
		boolean isIncludeIgnored = true;
		boolean isCollectParentExternals = false;
		System.out.println("Status for '" + wcDir.getAbsolutePath() + "':");

		System.out.println("Updating '" + wcDir.getAbsolutePath() + "'...");
		try {
			/*
			 * recursively updates wcDir to the latest revision (SVNRevision.HEAD)
			 */
			update(wcDir, SVNRevision.HEAD, true);
		} catch (SVNException svne) {
			error("error while recursively updating the working copy at '" + wcDir.getAbsolutePath() + "'", svne);
		}
		System.out.println("");

		System.out.println("Committing changes for '" + wcDir.getAbsolutePath() + "'...");
		try {
			/*
			 * commits changes in wcDir to the repository with not leaving items locked (if
			 * any) after the commit succeeds; this will add aNewDir & aNewFile to the
			 * repository.
			 */
			committedRevision = commit(wcDir, false, "'/newDir' with '/newDir/newFile.txt' were added")
					.getNewRevision();
		} catch (SVNException svne) {
			error("error while committing changes to the working copy at '" + wcDir.getAbsolutePath() + "'", svne);
		}
		System.out.println("Committed to revision " + committedRevision);
		System.out.println();

		System.out.println(
				"Locking (with stealing if the entry is already locked) '" + aNewFile.getAbsolutePath() + "'.");

		try {
			committedRevision = commit(wcDir, false,
					"deleting '" + aNewDir.getAbsolutePath() + "' from the filesystem as well as from the repository")
					.getNewRevision();
		} catch (SVNException svne) {
			error("error while committing changes to the working copy '" + wcDir.getAbsolutePath() + "'", svne);
		}
		System.out.println("Committed to revision " + committedRevision);
		System.exit(0);
	}

	private static final void createLocalDir(File aNewDir, File[] localFiles, String[] fileContents) {
		if (!aNewDir.mkdirs()) {
			error("failed to create a new directory '" + aNewDir.getAbsolutePath() + "'.", null);
		}

		for (int i = 0; i < localFiles.length; i++) {
			File aNewFile = localFiles[i];
			try {
				if (!aNewFile.createNewFile()) {
					error("failed to create a new file '" + aNewFile.getAbsolutePath() + "'.", null);
				}
			} catch (IOException ioe) {
				aNewFile.delete();
				error("error while creating a new file '" + aNewFile.getAbsolutePath() + "'", ioe);
			}

			String contents = null;
			if (i > fileContents.length - 1) {
				continue;
			}
			contents = fileContents[i];

			/*
			 * writing a text into the file
			 */
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(aNewFile);
				fos.write(contents.getBytes());
			} catch (FileNotFoundException fnfe) {
				error("the file '" + aNewFile.getAbsolutePath() + "' is not found", fnfe);
			} catch (IOException ioe) {
				error("error while writing into the file '" + aNewFile.getAbsolutePath() + "'", ioe);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException ioe) {
						//
					}
				}
			}
		}
	}

	private static void error(String message, Exception e) {
		System.err.println(message + (e != null ? ": " + e.getMessage() : ""));
		System.exit(1);
	}

	public static class CommitEventHandler implements ISVNEventHandler {

		public void handleEvent(SVNEvent event, double progress) {
			SVNEventAction action = event.getAction();
			if (action == SVNEventAction.COMMIT_MODIFIED) {
				System.out.println("Sending   ");
			} else if (action == SVNEventAction.COMMIT_DELETED) {
				System.out.println("Deleting   ");
			} else if (action == SVNEventAction.COMMIT_REPLACED) {
				System.out.println("Replacing   ");
			} else if (action == SVNEventAction.COMMIT_DELTA_SENT) {
				System.out.println("Transmitting file data....");
			} else if (action == SVNEventAction.COMMIT_ADDED) {
				/*
				 * Gets the MIME-type of the item.
				 */
				String mimeType = event.getMimeType();
				if (SVNProperty.isBinaryMimeType(mimeType)) {
					/*
					 * If the item is a binary file
					 */
					System.out.println("Adding  (bin)  ");
				} else {
					System.out.println("Adding         ");
				}
			}

		}

		public void checkCancelled() throws SVNCancelException {
		}
	}

	public static class UpdateEventHandler implements ISVNEventHandler {

		public void handleEvent(SVNEvent event, double progress) {
			/*
			 * Gets the current action. An action is represented by SVNEventAction. In case
			 * of an update an action can be determined via comparing SVNEvent.getAction()
			 * and SVNEventAction.UPDATE_-like constants.
			 */
			SVNEventAction action = event.getAction();
			String pathChangeType = " ";
			if (action == SVNEventAction.UPDATE_ADD) {
				/*
				 * the item was added
				 */
				pathChangeType = "A";
			} else if (action == SVNEventAction.UPDATE_DELETE) {
				/*
				 * the item was deleted
				 */
				pathChangeType = "D";
			} else if (action == SVNEventAction.UPDATE_UPDATE) {
				/*
				 * Find out in details what state the item is (after having been updated).
				 * 
				 * Gets the status of file/directory item contents. It is SVNStatusType who
				 * contains information on the state of an item.
				 */
				SVNStatusType contentsStatus = event.getContentsStatus();
				if (contentsStatus == SVNStatusType.CHANGED) {
					/*
					 * the item was modified in the repository (got the changes from the repository
					 */
					pathChangeType = "U";
				} else if (contentsStatus == SVNStatusType.CONFLICTED) {
					/*
					 * The file item is in a state of Conflict. That is, changes received from the
					 * repository during an update, overlap with local changes the user has in his
					 * working copy.
					 */
					pathChangeType = "C";
				} else if (contentsStatus == SVNStatusType.MERGED) {
					/*
					 * The file item was merGed (those changes that came from the repository did not
					 * overlap local changes and were merged into the file).
					 */
					pathChangeType = "G";
				}
			} else if (action == SVNEventAction.UPDATE_EXTERNAL) {
				/* for externals definitions */
				System.out.println("Fetching external item into '" + event.getFile().getAbsolutePath() + "'");
				System.out.println("External at revision " + event.getRevision());
				return;
			} else if (action == SVNEventAction.UPDATE_COMPLETED) {
				/*
				 * Working copy update is completed. Prints out the revision.
				 */
				System.out.println("At revision " + event.getRevision());
				return;
			} else if (action == SVNEventAction.ADD) {
				System.out.println("A     ");
				return;
			} else if (action == SVNEventAction.DELETE) {
				System.out.println("D     ");
				return;
			} else if (action == SVNEventAction.LOCKED) {
				System.out.println("L     ");
				return;
			} else if (action == SVNEventAction.LOCK_FAILED) {
				System.out.println("failed to lock    ");
				return;
			}

			/*
			 * Status of properties of an item. SVNStatusType also contains information on
			 * the properties state.
			 */
			SVNStatusType propertiesStatus = event.getPropertiesStatus();
			String propertiesChangeType = " ";
			if (propertiesStatus == SVNStatusType.CHANGED) {
				/*
				 * Properties were updated.
				 */
				propertiesChangeType = "U";
			} else if (propertiesStatus == SVNStatusType.CONFLICTED) {
				/*
				 * Properties are in conflict with the repository.
				 */
				propertiesChangeType = "C";
			} else if (propertiesStatus == SVNStatusType.MERGED) {
				/*
				 * Properties that came from the repository were merged with the local ones.
				 */
				propertiesChangeType = "G";
			}

			/*
			 * Gets the status of the lock.
			 */
			String lockLabel = " ";
			SVNStatusType lockType = event.getLockStatus();

			if (lockType == SVNStatusType.LOCK_UNLOCKED) {
				/*
				 * The lock is broken by someone.
				 */
				lockLabel = "B";
			}

			System.out.println(pathChangeType + propertiesChangeType + lockLabel + "       ");
		}

		public void checkCancelled() throws SVNCancelException {
		}
	}

	public static class InfoHandler implements ISVNInfoHandler {

		public void handleInfo(SVNInfo info) {
			System.out.println("-----------------INFO-----------------");
			System.out.println("Local Path: " + info.getFile().getPath());
			System.out.println("URL: " + info.getURL());

			if (info.isRemote() && info.getRepositoryRootURL() != null) {
				System.out.println("Repository Root URL: " + info.getRepositoryRootURL());
			}

			if (info.getRepositoryUUID() != null) {
				System.out.println("Repository UUID: " + info.getRepositoryUUID());
			}

			System.out.println("Revision: " + info.getRevision().getNumber());
			System.out.println("Node Kind: " + info.getKind().toString());

			if (!info.isRemote()) {
				System.out.println("Schedule: " + (info.getSchedule() != null ? info.getSchedule() : "normal"));
			}

			System.out.println("Last Changed Author: " + info.getAuthor());
			System.out.println("Last Changed Revision: " + info.getCommittedRevision().getNumber());
			System.out.println("Last Changed Date: " + info.getCommittedDate());

			if (info.getPropTime() != null) {
				System.out.println("Properties Last Updated: " + info.getPropTime());
			}

			if (info.getKind() == SVNNodeKind.FILE && info.getChecksum() != null) {
				if (info.getTextTime() != null) {
					System.out.println("Text Last Updated: " + info.getTextTime());
				}
				System.out.println("Checksum: " + info.getChecksum());
			}

			if (info.getLock() != null) {
				if (info.getLock().getID() != null) {
					System.out.println("Lock Token: " + info.getLock().getID());
				}

				System.out.println("Lock Owner: " + info.getLock().getOwner());
				System.out.println("Lock Created: " + info.getLock().getCreationDate());

				if (info.getLock().getExpirationDate() != null) {
					System.out.println("Lock Expires: " + info.getLock().getExpirationDate());
				}

				if (info.getLock().getComment() != null) {
					System.out.println("Lock Comment: " + info.getLock().getComment());
				}
			}
		}
	}

	public static class WCEventHandler implements ISVNEventHandler {

		public void handleEvent(SVNEvent event, double progress) {
			SVNEventAction action = event.getAction();

			if (action == SVNEventAction.ADD) {
				/*
				 * The item is scheduled for addition.
				 */
				System.out.println("A      + event.getPath()");
				return;
			} else if (action == SVNEventAction.COPY) {
				/*
				 * The item is scheduled for addition with history (copied, in other words).
				 */
				System.out.println("A  +   + event.getPath()");
				return;
			} else if (action == SVNEventAction.DELETE) {
				/*
				 * The item is scheduled for deletion.
				 */
				System.out.println("D      + event.getPath()");
				return;
			} else if (action == SVNEventAction.LOCKED) {
				/*
				 * The item is locked.
				 */
				// System.out.println("L " + event.getPath());
				return;
			} else if (action == SVNEventAction.LOCK_FAILED) {
				/*
				 * Locking operation failed.
				 */
				System.out.println("failed to lock     event.getPath()");
				return;
			}
		}

		public void checkCancelled() throws SVNCancelException {
		}
	}

	private static long update(File wcPath, SVNRevision updateToRevision, boolean isRecursive) throws SVNException {

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the update
		 */
		updateClient.setIgnoreExternals(false);
		/*
		 * returns the number of the revision wcPath was updated to
		 */
		return updateClient.doUpdate(wcPath, updateToRevision, isRecursive);
	}

	private static long checkout(SVNURL url, SVNRevision revision, File destPath, boolean isRecursive)
			throws SVNException {

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the checkout
		 */
		updateClient.setIgnoreExternals(false);

		return updateClient.doCheckout(url, destPath, revision, revision, isRecursive);
	}

	private static SVNCommitInfo commit(File wcPath, boolean keepLocks, String commitMessage) throws SVNException {
		return ourClientManager.getCommitClient().doCommit(new File[] { wcPath }, keepLocks, commitMessage, false,
				true);
	}

	private static SVNCommitInfo importDirectory(File localPath, SVNURL dstURL, String commitMessage,
			boolean isRecursive) throws SVNException {
		return ourClientManager.getCommitClient().doImport(localPath, dstURL, commitMessage, isRecursive);
	}

	private static SVNCommitInfo makeDirectory(SVNURL url, String commitMessage) throws SVNException {
		return ourClientManager.getCommitClient().doMkDir(new SVNURL[] { url }, commitMessage);
	}

	private static void addEntry(File wcPath) throws SVNException {
		ourClientManager.getWCClient().doAdd(wcPath, false, false, false, true);
	}

	private static void lock(File wcPath, boolean isStealLock, String lockComment) throws SVNException {
		ourClientManager.getWCClient().doLock(new File[] { wcPath }, isStealLock, lockComment);
	}

}