package com.svn.crud;

import java.util.Date;
import java.util.Map;

public class FileChangeDetail {

	private String filePath;
	private char changeType;
	private long revision;
	private String author;
	private Date commitDate;
	private String commitMessage;
	private long fileSize;
	private String checksum;
	private String mimeType;
	private Map<String, String> customProperties;

	// Getters & Setters
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String v) {
		this.filePath = v;
	}

	public char getChangeType() {
		return changeType;
	}

	public void setChangeType(char v) {
		this.changeType = v;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long v) {
		this.revision = v;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String v) {
		this.author = v;
	}

	public Date getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(Date v) {
		this.commitDate = v;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String v) {
		this.commitMessage = v;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long v) {
		this.fileSize = v;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String v) {
		this.checksum = v;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String v) {
		this.mimeType = v;
	}

	public Map<String, String> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(Map<String, String> v) {
		this.customProperties = v;
	}

	@Override
	public String toString() {
		return String.format("[r%d] [%s] %s | Author: %s | Date: %s | Size: %d | MD5: %s | MIME: %s | Msg: %s",
				revision, changeType, filePath, author, commitDate, fileSize, checksum, mimeType, commitMessage);
	}
}