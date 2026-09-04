package com.svn.crud;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * WorkingCopyManage.java
 *
 * This class contains event handler inner classes. SVNKit fires "events"
 * whenever something happens (file added, committed, etc.) CommitEventHandler
 * listens to those events and prints what happened.
 */
public class WorkingCopyManage {

	/**
	 * CommitEventHandler - listens to SVN commit events and prints status. This is
	 * used inside SvnRepoCRUD when doing doCommit() operations.
	 */
	public static class CommitEventHandler implements ISVNEventHandler {

		/**
		 * Called automatically by SVNKit whenever a commit action happens. For example:
		 * file added, file deleted, file modified, data sent.
		 */
		public void handleEvent(SVNEvent event, double progress) {
			SVNEventAction action = event.getAction();

			if (action == SVNEventAction.COMMIT_ADDED) {
				System.out.println("  [COMMIT] Added     : " + event.getFile());

			} else if (action == SVNEventAction.COMMIT_MODIFIED) {
				System.out.println("  [COMMIT] Modified  : " + event.getFile());

			} else if (action == SVNEventAction.COMMIT_DELETED) {
				System.out.println("  [COMMIT] Deleted   : " + event.getFile());

			} else if (action == SVNEventAction.COMMIT_REPLACED) {
				System.out.println("  [COMMIT] Replaced  : " + event.getFile());

			} else if (action == SVNEventAction.COMMIT_DELTA_SENT) {
				System.out.println("  [COMMIT] Sending file data...");
			}
		}

		/**
		 * Called by SVNKit to check if the operation was cancelled. We don't cancel
		 * anything, so this is left empty.
		 */
		public void checkCancelled() throws SVNCancelException {
			// not cancelling
		}
	}
}