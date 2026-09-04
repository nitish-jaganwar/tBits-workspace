package com.svn.crud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNTestUtils {

	private static final String REPO_PATH = "C:/SVN/repo";
	private static final String REPO_URL = "file:///C:/SVN/repo";

	private static String currentUser = "nitish";
	private static String currentPassword = "123";

	private static SVNRepository repository;

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);

		System.out.println("\n========= SVN MENU =========");
		System.out.println("1. Create Repository");
		System.out.println("2. Connect Repository");
		System.out.println("3. Add File");
		System.out.println("4. Add Folder");
		System.out.println("5. Read History");
		System.out.println("6. Read File Content");
		System.out.println("7. Update File");
		System.out.println("8. Delete File/Folder");
		System.out.println("9. Exit");
		System.out.println("10. Full History Report (Audit)");
		System.out.println("11. Changes Between Dates");
		System.out.println("12. Changes After Revision");
		System.out.println("13. Checkout Repository");
		System.out.println("14. Update Working Copy");
		System.out.println("15. Commit (Working Copy)");
		System.out.println("16. get Revision Info");
		// addProperties
		System.out.println("17. add Properties ");
		System.out.println("18. Get Repo Properties");
		System.out.println("19. Print All File URLs");
		System.out.println("21. Switch User ");

		System.out.println("22. Particular File History");
		System.out.println("23. Set Property on Particular File");
		System.out.println("24. Read Properties of Particular File");
		System.out.println("\n9. Exit");
		// Particular file history
		// prop to file
		//

		try {
			while (true) {

				System.out.print("Choose option: ");
				int choice = sc.nextInt();
				sc.nextLine(); // consume newline

				switch (choice) {

				case 1:
					createRepository(REPO_PATH);
					break;

				case 2:
					repository = connect(REPO_URL, currentUser, currentPassword);
					break;

				case 3:
					checkConnection();

					System.out.print("Enter file name: ");
					String fileName = sc.nextLine();

					System.out.print("Enter content: ");
					String content = sc.nextLine();

					System.out.print("Enter commit message: ");
					String msg = sc.nextLine();

					addFile(repository, fileName, content, msg);
					break;

				case 4:
					checkConnection();
					System.out.print("Enter folder name: ");
					String folder = sc.nextLine();
					addFolder(repository, folder);
					break;

				case 5:
					checkConnection();
					readHistory(repository);
					break;

				case 6:
//					checkConnection();
//					System.out.print("Enter file path: ");
//					String filePath = sc.nextLine();
//					readFile(repository, filePath);
//					break;
					checkConnection();
					System.out.print("Enter file path (e.g., test1.txt): ");
					String readPath = sc.nextLine();

					System.out.print("Enter Revision Number (-1 for Latest/HEAD): ");
					long readRev = sc.nextLong();
					sc.nextLine(); // consume newline

					readFile(repository, readPath, readRev);
					break;

				case 7:
//					checkConnection();
//					System.out.print("Enter file path: ");
//					String updatePath = sc.nextLine();
//					System.out.print("Enter old content: ");
//					String oldContent = sc.nextLine();
//					System.out.print("Enter new content: ");
//					String newContent = sc.nextLine();
//
//					System.out.print("Enter commit message: ");
//					String msg1 = sc.nextLine();
//
//					updateFile(repository, updatePath, oldContent, newContent, msg1);
					checkConnection();
					System.out.print("Enter file path (e.g., t2.txt): ");
					String updatePath = sc.nextLine();

					System.out.print("Enter new content: ");
					String newContent = sc.nextLine();

					System.out.print("Enter commit message: ");
					String msg1 = sc.nextLine();

					updateFile(repository, updatePath, newContent, msg1);
					break;
				// updateFile(repository, updatePath, oldContent, newContent);
				// break;

				case 8:
					checkConnection();
					System.out.print("Enter path to delete: ");
					String delPath = sc.nextLine();

					System.out.print("Enter commit message: ");
					String msg2 = sc.nextLine();

					// deleteEntry(repo, path, msg);
					deleteEntry(repository, delPath, msg2);
					break;

				case 9:
					System.out.println("Exiting...");
					return;
				case 10:
					checkConnection();
					List<FileChangeDetail> list = getFullHistory(repository);
					printReport(list);
					break;
				case 11:
					checkConnection();

					System.out.print("Enter FROM (dd-MM-yyyy): ");
					String fromStr = sc.nextLine();

					System.out.print("Enter TO (dd-MM-yyyy): ");
					String toStr = sc.nextLine();
					LocalDateTime from = parseFlexibleDate(fromStr);
					LocalDateTime to = parseFlexibleDate(toStr).withHour(23).withMinute(59).withSecond(59);

//					LocalDateTime from = LocalDateTime.parse(fromStr);
//					LocalDateTime to = LocalDateTime.parse(toStr);

					List<FileChangeDetail> res = getChangesBetweenDates(from, to);

					printReport(res);
					break;
				case 12:
					checkConnection();

					System.out.print("Enter revision: ");
					long rev = sc.nextLong();
					sc.nextLine();

					List<FileChangeDetail> res2 = getChangesAfterRevision(rev);

					printReport(res2);
					break;
				case 13:
					System.out.print("Enter Repo URL: ");
					String url = sc.nextLine();

					System.out.print("Enter local path: ");
					String path = sc.nextLine();

					checkout(url, currentUser, currentPassword, path);
					break;
				case 14:
					System.out.print("Enter working copy path: ");
					String wcPath = sc.nextLine();

					update(REPO_URL, currentUser, currentPassword, wcPath);
					break;
				case 15:
					System.out.print("Enter working copy path: ");
					String commitPath = sc.nextLine();

					System.out.print("Enter commit message: ");
					String msg3 = sc.nextLine();

					doCommit(currentUser, currentPassword, commitPath, msg3);
					break;
				case 16:
					checkConnection();

					System.out.print("Enter revision number: ");
					long rev1 = sc.nextLong();
					sc.nextLine();

					getRevisionInfo(repository, rev1);
					break;
				case 17:
					System.out.print("Enter property key: ");
					String key = sc.nextLine();

					System.out.print("Enter property value: ");
					String value = sc.nextLine();

					addProperties(key, value);
					break;
				case 18:
					getRepoProperties();
					break;
				case 19:
					checkConnection();
					System.out.println("Scanning repository for files and URLs...");

					printAllFileUrls(repository, "");
					break;
				case 21:
					System.out.println("Current User is: " + currentUser);

					System.out.print("Enter new user name (e.g. nitish): ");
					currentUser = sc.nextLine();

					System.out.print("Enter password (can be anything): ");
					currentPassword = sc.nextLine();

					repository = connect(REPO_URL, currentUser, currentPassword);

					System.out.println("\n SWITCH SUCCESSFUL: now all commits will be made by '" + currentUser + "'!");
					break;

				case 22:
					checkConnection();
					System.out.print("Enter file path (e.g., test1.txt): ");
					String histPath = sc.nextLine();
					getSingleFileHistory(repository, histPath);
					break;

				case 23:
					checkConnection();
					System.out.print("Enter file path (e.g., test1.txt): ");
					String propFile = sc.nextLine();

					System.out.print("Enter Property Key (e.g., custom-status): ");
					String propKey = sc.nextLine();

					System.out.print("Enter Property Value (e.g., Approved): ");
					String propVal = sc.nextLine();

					System.out.print("Enter Commit Message: ");
					String propMsg = sc.nextLine();

					setSingleFileProperty(repository, propFile, propKey, propVal, propMsg);
					break;

				case 24:
					checkConnection();
					System.out.print("Enter file path (e.g., test1.txt): ");
					String readPropFile = sc.nextLine();
					readSingleFileProperties(repository, readPropFile);
					break;
				default:
					System.out.println("Invalid option!");
				}
			}

		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	// ================= COMMON =================
	private static void checkConnection() {
		if (repository == null) {
			throw new RuntimeException("Please connect to repository first!");
		}
	}

	// ================= CREATE REPO =================
	public static void createRepository(String path) throws SVNException {
		SVNRepositoryFactory.createLocalRepository(new File(path), true, false);
		System.out.println("Repository created!");
	}

	// ================= CONNECT =================
	public static SVNRepository connect(String url, String user, String pass) throws SVNException {
		FSRepositoryFactory.setup();

		SVNRepository repo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url.replace(" ", "%20")));

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(user, pass);

		repo.setAuthenticationManager(auth);

		System.out.println("Connected! Latest Rev: " + repo.getLatestRevision());
		return repo;
	}

	// ================= ADD FILE =================
	public static void addFile(SVNRepository repo, String fileName, String content, String message)
			throws SVNException {

		ISVNEditor editor = repo.getCommitEditor(message, null);

		editor.openRoot(-1);

		editor.addFile(fileName, null, -1);
		editor.applyTextDelta(fileName, null);

		SVNDeltaGenerator delta = new SVNDeltaGenerator();

		String checksum = delta.sendDelta(fileName, new ByteArrayInputStream(content.getBytes()), editor, true);

		editor.closeFile(fileName, checksum);
		editor.closeDir();

		SVNCommitInfo info = editor.closeEdit();

		System.out.println("File added → Revision: " + info.getNewRevision());
	}

	// ================= ADD FOLDER =================
	public static void addFolder(SVNRepository repo, String folderName) throws SVNException {

		ISVNEditor editor = repo.getCommitEditor("Add folder " + folderName, null);
		editor.openRoot(-1);

		editor.addDir(folderName, null, -1);

		editor.closeDir();
		editor.closeDir();

		SVNCommitInfo info = editor.closeEdit();
		System.out.println("Folder added → Revision: " + info.getNewRevision());
	}

	// ================= READ HISTORY =================
	public static void readHistory(SVNRepository repo) throws SVNException {

		Collection logs = repo.log(new String[] { "" }, null, 0, -1, true, true);

		for (Object obj : logs) {
			SVNLogEntry entry = (SVNLogEntry) obj;

//			System.out.println("\nRevision: " + entry.getRevision());
//			System.out.println("Author: " + entry.getAuthor());
//			System.out.println("Date: " + entry.getDate());
			System.out.println("---------------------------------------------");
			System.out.println("Revision    : " + entry.getRevision());
			System.out.println("Author      : " + entry.getAuthor());
			System.out.println("Date        : " + entry.getDate());
			System.out.println("Log message : " + entry.getMessage());

			entry.getChangedPaths().forEach((path, val) -> {
				SVNLogEntryPath p = (SVNLogEntryPath) val;
				System.out.println(" [" + p.getType() + "] " + p.getPath());
			});
		}
		long latestRevision = repository.getLatestRevision();
		System.out.println("---------------------------------------------");
		System.out.println("Latest revision: " + latestRevision);
		System.out.println("--- READ DONE ---\n");
	}

	// ================= READ FILE =================
//	public static void readFile(SVNRepository repo, String path) throws Exception {
//
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		repo.getFile(path, -1, new SVNProperties(), out);
//
//		System.out.println("Content:\n" + out.toString());
//	}
	// ================= READ FILE (WITH PEG REVISION) =================
	public static void readFile(SVNRepository repo, String path, long revision) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			// Yahan -1 ki jagah ab 'revision' variable use ho raha hai
			repo.getFile(path, revision, new SVNProperties(), out);

			String revLabel = (revision == -1) ? "LATEST (HEAD)" : "Revision " + revision;
			System.out.println("\n--- Content at " + revLabel + " ---");
			System.out.println(out.toString());
			System.out.println("--------------------------------");

		} catch (Exception e) {
			System.err.println("\nERROR: File nahi mili ya read nahi ho payi.");
			System.err.println("SVN Reason: " + e.getMessage());
		}
	}

	// ================= UPDATE =================
//	public static void updateFile(SVNRepository repo, String path, String oldContent, String newContent, String message)
//			throws SVNException {
//
//		// ISVNEditor editor = repo.getCommitEditor("Update file " + path, null);
//		ISVNEditor editor = repo.getCommitEditor(message, null);
//		editor.openRoot(-1);
//		editor.openFile(path, -1);
//		editor.applyTextDelta(path, null);
//
//		SVNDeltaGenerator delta = new SVNDeltaGenerator();
//
//		String checksum = delta.sendDelta(path, new ByteArrayInputStream(oldContent.getBytes()), 0,
//				new ByteArrayInputStream(newContent.getBytes()), editor, true);
//
//		editor.closeFile(path, checksum);
//		editor.closeDir();
//
//		SVNCommitInfo info = editor.closeEdit();
//		System.out.println("Updated → Revision: " + info.getNewRevision());
//	}
	// ================= UPDATE (SMART AUTO-FETCH) =================
	public static void updateFile(SVNRepository repo, String path, String newContent, String message) throws Exception {

		String cleanPath = path;
		String repoBaseUrl = repo.getLocation().toString();
		if (path.startsWith(repoBaseUrl)) {
			cleanPath = path.substring(repoBaseUrl.length());
		}
		if (cleanPath.startsWith("/")) {
			cleanPath = cleanPath.substring(1);
		}

		ByteArrayOutputStream oldOut = new ByteArrayOutputStream();
		try {
			repo.getFile(cleanPath, -1, new SVNProperties(), oldOut);
		} catch (SVNException e) {
			System.err.println("ERROR: File server par nahi mili. Kripya path check karein: " + cleanPath);
			return;
		}
		byte[] oldData = oldOut.toByteArray();

		ISVNEditor editor = repo.getCommitEditor(message, null);
		editor.openRoot(-1);
		editor.openFile(cleanPath, -1);
		editor.applyTextDelta(cleanPath, null);

		SVNDeltaGenerator delta = new SVNDeltaGenerator();
		String checksum = delta.sendDelta(cleanPath, new ByteArrayInputStream(oldData), 0,
				new ByteArrayInputStream(newContent.getBytes()), editor, true);

		editor.closeFile(cleanPath, checksum);
		editor.closeDir();

		SVNCommitInfo info = editor.closeEdit();
		System.out.println("Updated Successfully → Revision: " + info.getNewRevision());
	}

	// ================= DELETE =================
	public static void deleteEntry(SVNRepository repo, String path, String msg2) throws SVNException {

		ISVNEditor editor = repo.getCommitEditor(msg2 + path, null);

		editor.openRoot(-1);
		editor.deleteEntry(path, -1);
		editor.closeDir();

		SVNCommitInfo info = editor.closeEdit();
		System.out.println("Deleted → Revision: " + info.getNewRevision());
	}

	private static SVNRepository createNewRepoInstance() throws SVNException {

		FSRepositoryFactory.setup();

		SVNRepository newRepo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(REPO_URL.replace(" ", "%20")));

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(currentUser, currentPassword);

		newRepo.setAuthenticationManager(auth);

		return newRepo;
	}

	public static List<FileChangeDetail> getFullHistory(SVNRepository repo) throws SVNException {

		List<FileChangeDetail> list = new ArrayList<>();

		repo.log(new String[] { "" }, 0, -1, true, false, 0, logEntry -> {

			long revision = logEntry.getRevision();
			String author = logEntry.getAuthor();
			Date date = (Date) logEntry.getDate();
			String msg = logEntry.getMessage();

			if (logEntry.getChangedPaths() == null)
				return;

			for (Object key : logEntry.getChangedPaths().keySet()) {

				SVNLogEntryPath pathObj = (SVNLogEntryPath) logEntry.getChangedPaths().get(key);

				String path = pathObj.getPath();
				char type = pathObj.getType();

				FileChangeDetail detail = new FileChangeDetail();
				detail.setFilePath(path);
				detail.setChangeType(type);
				detail.setRevision(revision);
				detail.setAuthor(author);
				detail.setCommitDate(date);
				detail.setCommitMessage(msg);

				// Fetch properties
				if (type != 'D') {
					enrichFile(detail, path, revision); // ✅ no repo passed
				}

				list.add(detail);
			}
		});

		return list;
	}

	private static void enrichFile(FileChangeDetail detail, String path, long revision) {

		try {
			SVNRepository repo = createNewRepoInstance(); // 🔥 NEW INSTANCE

			String cleanPath = path.startsWith("/") ? path.substring(1) : path;

			if (repo.checkPath(cleanPath, revision) != SVNNodeKind.FILE)
				return;

			SVNProperties props = new SVNProperties();

			repo.getFile(cleanPath, revision, props, null);

			Map<String, String> custom = new HashMap<>();

			for (Object k : props.nameSet()) {

				String key = (String) k;
				String val = props.getStringValue(key);

				switch (key) {
				case SVNProperty.WORKING_SIZE:
					try {
						detail.setFileSize(Long.parseLong(val));
					} catch (Exception e) {
					}
					break;

				case SVNProperty.CHECKSUM:
					detail.setChecksum(val);
					break;

				case SVNProperty.MIME_TYPE:
					detail.setMimeType(val);
					break;

				default:
					custom.put(key, val);
				}
			}

			detail.setCustomProperties(custom);

		} catch (Exception e) {
			System.out.println("Property fetch failed for " + path);
		}
	}

	public static void printReport(List<FileChangeDetail> list) {

		if (list.isEmpty()) {
			System.out.println("No changes found");
			return;
		}

		for (FileChangeDetail f : list) {

			System.out.println("------------------------------------");
			System.out.println(f.toString());

			if (f.getCustomProperties() != null) {
				f.getCustomProperties().forEach((k, v) -> System.out.println("   " + k + " = " + v));
			}
		}
	}

	public static List<FileChangeDetail> getChangesBetweenDates(LocalDateTime from, LocalDateTime to)
			throws SVNException {

		SVNRepository repo = createNewRepoInstance();

		long startRev = repo.getDatedRevision(Date.from(from.atZone(ZoneId.systemDefault()).toInstant()));
		long endRev = repo.getDatedRevision(Date.from(to.atZone(ZoneId.systemDefault()).toInstant()));

		System.out.println("Revisions: " + startRev + " → " + endRev);

		return collectChanges(startRev, endRev);
	}

	public static List<FileChangeDetail> getChangesAfterRevision(long rev) throws SVNException {

		SVNRepository repo = createNewRepoInstance();

		long latest = repo.getLatestRevision();

		if (rev >= latest) {
			System.out.println("No new changes");
			return Collections.emptyList();
		}

		return collectChanges(rev + 1, latest);
	}

	public static List<FileChangeDetail> collectChanges(long startRev, long endRev) throws SVNException {

		SVNRepository repo = createNewRepoInstance(); // 🔥 important

		List<FileChangeDetail> list = new ArrayList<>();

		repo.log(new String[] { "" }, startRev, endRev, true, false, 0, logEntry -> {

			for (Map.Entry<String, SVNLogEntryPath> entry : logEntry.getChangedPaths().entrySet()) {

				FileChangeDetail d = new FileChangeDetail();

				d.setFilePath(entry.getKey());
				d.setChangeType(entry.getValue().getType());
				d.setRevision(logEntry.getRevision());
				d.setAuthor(logEntry.getAuthor());
				d.setCommitDate(logEntry.getDate());
				d.setCommitMessage(logEntry.getMessage());

				if (d.getChangeType() != 'D') {
					enrichFile(d, d.getFilePath(), d.getRevision());
				}

				list.add(d);
			}
		});

		return list;
	}

	public static LocalDateTime parseDate(String input) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

		LocalDate date = LocalDate.parse(input, formatter);

		// default time = start of day
		return date.atStartOfDay();
	}

	public static LocalDateTime parseFlexibleDate(String input) {

		String[] patterns = { "dd-MM-yyyy", "dd/MM/yyyy", "ddMMyyyy" };

		for (String pattern : patterns) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

				LocalDate date = LocalDate.parse(input, formatter);
				return date.atStartOfDay();

			} catch (Exception ignored) {
			}
		}

		throw new RuntimeException("Invalid date format: " + input);
	}

	public static void checkout(String url, String user, String pass, String localPath) throws SVNException {

		SVNURL svnUrl = SVNURL.parseURIEncoded(url);
		File wcPath = new File(localPath);

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(user, pass);

		SVNClientManager clientManager = SVNClientManager.newInstance(null, auth);

		SVNUpdateClient updateClient = clientManager.getUpdateClient();

		long rev = updateClient.doCheckout(svnUrl, wcPath, SVNRevision.HEAD, SVNRevision.HEAD, true);

		System.out.println("Checkout done → Revision: " + rev);
	}

	public static void update(String url, String user, String pass, String wcPathStr) throws SVNException {

		File wcPath = new File(wcPathStr);

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(user, pass);

		SVNClientManager clientManager = SVNClientManager.newInstance(null, auth);

		SVNUpdateClient updateClient = clientManager.getUpdateClient();

		long rev = updateClient.doUpdate(wcPath, SVNRevision.HEAD, true);

		System.out.println("Updated → Revision: " + rev);
	}

	public static void doCommit(String user, String pass, String wcPath, String message) throws SVNException {

		File path = new File(wcPath);

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(user, pass);

		SVNClientManager clientManager = SVNClientManager.newInstance(null, auth);

		long rev = clientManager.getCommitClient()
				.doCommit(new File[] { path }, false, message, null, null, false, false, SVNDepth.INFINITY)
				.getNewRevision();

		System.out.println("Committed → Revision: " + rev);
	}

	public static void getRevisionInfo(SVNRepository repo, long revision) throws SVNException {

		repo.log(new String[] { "" }, revision, revision, true, false, 0, logEntry -> {

			System.out.println("\nRevision: " + logEntry.getRevision());
			System.out.println("Author: " + logEntry.getAuthor());
			System.out.println("Date: " + logEntry.getDate());
			System.out.println("Message: " + logEntry.getMessage());

			logEntry.getChangedPaths().forEach((path, val) -> {
				SVNLogEntryPath p = (SVNLogEntryPath) val;
				System.out.println(" [" + p.getType() + "] " + p.getPath());
			});
		});
	}

	public static void addProperties(String key, String value) throws SVNException {

		System.out.print("Enter working copy path: ");
		Scanner sc = new Scanner(System.in);
		String path = sc.nextLine();

		File file = new File(path);

		ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(currentUser, currentPassword);

		SVNClientManager clientManager = SVNClientManager.newInstance(null, auth);

		SVNWCClient wcClient = clientManager.getWCClient();

		SVNPropertyValue propValue = SVNPropertyValue.create(value);

		wcClient.doSetProperty(file, key, propValue, false, false, null);

		System.out.println("Property added: " + key + " = " + value);

		// IMPORTANT → Commit property
		clientManager.getCommitClient().doCommit(new File[] { file }, false, "Added property: " + key, null, null,
				false, false, SVNDepth.EMPTY);

		System.out.println("Property committed!");
	}

	public static void getRepoProperties() throws SVNException {

		System.out.println("Fetching latest revision properties...");

		SVNRepository repo = connect(REPO_URL, currentUser, currentPassword);

		long latestRev = repo.getLatestRevision();

		SVNProperties props = repo.getRevisionProperties(latestRev, null);

		System.out.println("Revision: " + latestRev);

		for (Object key : props.nameSet()) {
			String k = (String) key;
			System.out.println(k + " = " + props.getStringValue(k));
		}
	}

	// ================= GET ALL FILE URLs (Recursive) =================
	public static void printAllFileUrls(SVNRepository repo, String path) {
		try {
			// Repository base URL
			SVNURL repoUrl = repo.getLocation();

			if (path.equals("")) {
				System.out.println("\nRepository Base URL: " + repoUrl.toString());
				System.out.println("------------------------------------------------");
			}

			Collection entries = repo.getDir(path, -1, null, (Collection) null);

			for (Object obj : entries) {
				SVNDirEntry entry = (SVNDirEntry) obj;

				// (e.g., "A" + "/" + "test1.txt")
				String entryPath = (path.equals("") ? "" : path + "/") + entry.getName();

				// Exact URL generate
				SVNURL exactUrl = repoUrl.appendPath(entryPath, false);

				if (entry.getKind() == SVNNodeKind.DIR) {

					System.out.println("📁 [DIR]  " + exactUrl.toString());
					printAllFileUrls(repo, entryPath); // Calling itself for sub-folder

				} else if (entry.getKind() == SVNNodeKind.FILE) {

					System.out.println("📄 [FILE] " + exactUrl.toString());

					// Task 8: Unique Audit URL (Peg URL) with revision number
					System.out.println("          ↳ Audit URL : " + exactUrl.toString() + "@" + entry.getRevision());
				}
			}
		} catch (SVNException e) {
			System.err.println("ERROR reading path '" + path + "': " + e.getMessage());
		}
	}

	// ================= 1. PARTICULAR FILE HISTORY =================
	public static void getSingleFileHistory(SVNRepository repo, String filePath) {
		System.out.println("\n--- History for File: '" + filePath + "' ---");
		try {
			// new String[]{filePath} pass
			repo.log(new String[] { filePath }, 0, -1, true, true, 0, logEntry -> {
				System.out.println("Revision : " + logEntry.getRevision());
				System.out.println("Author   : " + logEntry.getAuthor());
				System.out.println("Date     : " + logEntry.getDate());
				System.out.println("Message  : " + logEntry.getMessage());
				System.out.println("---------------------------------------------");
			});
		} catch (SVNException e) {
			System.err.println("ERROR fetching history for file: " + e.getMessage());
		}
	}

	// ================= 2. SET PROPERTY ON A PARTICULAR FILE =================
	public static void setSingleFileProperty(SVNRepository repo, String filePath, String propKey, String propValue,
			String message) {
		try {

			ISVNEditor editor = repo.getCommitEditor(message, null);
			editor.openRoot(-1);

			editor.openFile(filePath, -1); // File open

			// Custom property add/update
			editor.changeFileProperty(filePath, propKey, SVNPropertyValue.create(propValue));

			editor.closeFile(filePath, null); // Checksum send null
			editor.closeDir();

			SVNCommitInfo info = editor.closeEdit();
			System.out.println(
					"✅ Property '" + propKey + "' set on '" + filePath + "' -> Revision: " + info.getNewRevision());

		} catch (SVNException e) {
			System.err.println("ERROR setting property: " + e.getMessage());
		}
	}

	// ================= 3. READ PROPERTIES OF A PARTICULAR FILE =================
	public static void readSingleFileProperties(SVNRepository repo, String filePath) {
		try {
			SVNProperties props = new SVNProperties();

			repo.getFile(filePath, -1, props, null);

			System.out.println("\n--- Properties for: '" + filePath + "' ---");
			if (props.isEmpty()) {
				System.out.println("No properties found.");
			} else {
				for (String key : props.nameSet()) {
					System.out.println("  " + key + " = " + props.getStringValue(key));
				}
			}
			System.out.println("----------------------------------------");

		} catch (SVNException e) {
			System.err.println("ERROR reading properties: " + e.getMessage());
		}
	}
}