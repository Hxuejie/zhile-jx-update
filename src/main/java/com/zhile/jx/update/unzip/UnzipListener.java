package com.zhile.jx.update.unzip;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 解压监听接口
 * @author Hxuejie
 * @version 1.0.1 */
public interface UnzipListener {
	/** 当解压进度更新时回调
	 * @param zipFile 解压文件
	 * @param unZipEntry 解压条目
	 * @param progress 解压进度 [0,100]<BR>
	 *            0代表开始,100代表结束 */
	void onUnzipProgress(ZipFile zipFile, ZipEntry unZipEntry, float progress);
}