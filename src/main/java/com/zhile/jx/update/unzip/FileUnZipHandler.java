package com.zhile.jx.update.unzip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

/** 解压程序
 * @author Hxuejie
 * @version 1.0.1 */
public class FileUnZipHandler {

	private final Logger	LOGGER	= Logger.getLogger(FileUnZipHandler.class);
	private ZipFile			zipFile;
	private UnzipListener	unzipListener;
	private String			unzipPath;

	/** 创建解压缩程序
	 * @param zipPath 压缩包路径
	 * @throws IOException */
	public FileUnZipHandler(String zipPath) throws IOException {
		this(zipPath, "");
	}

	/** 创建解压缩程序
	 * @param zipPath 压缩包路径
	 * @param unzipPath 解压路径
	 * @throws IOException */
	public FileUnZipHandler(String zipPath, String unzipPath) throws IOException {
		this(new ZipFile(zipPath), unzipPath);
	}

	/** 创建解压缩程序
	 * @param zipFile 压缩包
	 * @throws IllegalArgumentException 当 zipFile==null 时抛出 */
	public FileUnZipHandler(ZipFile zipFile) {
		this(zipFile, "");
	}

	/** 创建解压缩程序
	 * @param zipFile 压缩包
	 * @param unzipPath 解压路径
	 * @throws IllegalArgumentException 当 zipFile==null 时抛出 */
	public FileUnZipHandler(ZipFile zipFile, String unzipPath) {
		if (zipFile == null) {
			throw new IllegalArgumentException("压缩包文件为NULL");
		}
		this.zipFile = zipFile;
		this.unzipPath = unzipPath == null ? "" : unzipPath.trim();
	}

	/** 设置解压监听接口
	 * @param unzipListener */
	public void setUnZipListener(UnzipListener unzipListener) {
		this.unzipListener = unzipListener;
	}

	/** 执行解压
	 * @throws IOException */
	public void doUnzip() throws IOException {
		createUnzipPath();
		unZip();
	}

	/** 解压文件
	 * @throws IOException */
	private void unZip() throws IOException {
		Enumeration<? extends ZipEntry> files = zipFile.entries();
		final int total = zipFile.size();
		int count = 0;
		LOGGER.info(String.format("开始解压文件[%s],总条目数:[%s]", zipFile.getName(), total));
		while (files.hasMoreElements()) {
			count++;
			ZipEntry file = files.nextElement();
			if (!file.isDirectory()) {
				unzipEntry(file);
			}
			else {
				File dir = new File(unzipPath, file.getName());
				dir.mkdir();
			}
			float progress = count * 100F / total;
			LOGGER.info(String.format("解压进度:%-50s%f%%", file.getName(), progress));
			notifyProgress(file, progress);
		}
		zipFile.close();
		LOGGER.info(String.format("完成解压缩文件[%s]", zipFile.getName()));
	}

	/** 解压条目
	 * @param fileEntry 文件条目
	 * @throws IOException */
	private void unzipEntry(ZipEntry fileEntry) throws IOException {
		File file = new File(unzipPath, fileEntry.getName());
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
		InputStream is = zipFile.getInputStream(fileEntry);
		byte[] data = new byte[1024];
		int len = 0;
		while ((len = is.read(data)) != -1) {
			bos.write(data, 0, len);
			bos.flush();
		}
		is.close();
		bos.close();
	}

	/** 创建解压路径
	 * @return */
	private boolean createUnzipPath() {
		if (!"".equals(unzipPath)) {
			File dir = new File(unzipPath);
			if (!dir.exists()) {
				return dir.mkdirs();
			}
		}
		return true;
	}

	/** 通知解压进度更新
	 * @param unZipEntry 解压条目
	 * @param progress 更新进度 */
	private void notifyProgress(ZipEntry unZipEntry, float progress) {
		if (unzipListener != null) {
			unzipListener.onUnzipProgress(zipFile, unZipEntry, progress);
		}
	}
}
