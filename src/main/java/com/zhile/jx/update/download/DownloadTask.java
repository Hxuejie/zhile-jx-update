package com.zhile.jx.update.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.easycluster.easycluster.core.Closure;

import com.zhile.jx.net.connect.ConnectHandler;

import edu.hziee.nGame.fds.bto.FileDownloadData;
import edu.hziee.nGame.fds.bto.FileDownloadInfo;
import edu.hziee.nGame.fds.bto.FileLocationInfo;
import edu.hziee.nGame.fds.bto.xip.DownloadReq;
import edu.hziee.nGame.fds.bto.xip.DownloadResp;

/** 下载任务
 * @author Hxuejie
 * @version 1.0.1 */
public class DownloadTask {

	private final Logger				LOGGER				= Logger.getLogger(DownloadTask.class);
	private ConnectHandler				connectHandler;
	private ArrayList<FileDownloadInfo>	downloadInfoList	= new ArrayList<FileDownloadInfo>();
	private DownloadReq					downloadRequest;
	private FileLocationInfo			downloadFileInfo;
	private File						file;
	private String						dstPath;
	private BufferedOutputStream		fileWriteStream;
	private long						dataLength;
	private long						fileSize;
	private boolean						readied;
	private boolean						downloading;
	private TaskCallback				taskCallback;
	private TaskProgressListener		progressListener;

	public DownloadTask(FileLocationInfo downloadFileInfo, String dstPath) {
		if (downloadFileInfo == null) {
			throw new IllegalArgumentException("info is null!");
		}
		if (dstPath == null) {
			throw new IllegalArgumentException("dstPath == null!");
		}
		this.downloadFileInfo = downloadFileInfo;
		this.dstPath = dstPath;
		fileSize = downloadFileInfo.getFileSize();
	}

	public FileLocationInfo getDownloadFileInfo() {
		return downloadFileInfo;
	}

	public TaskProgressListener getProgressListener() {
		return progressListener;
	}

	public void setProgressListener(TaskProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public boolean isDownloading() {
		return downloading;
	}

	public boolean ready() {
		try {
			loadFile();
			readied = true;
		} catch (IOException e) {
			e.printStackTrace();
			readied = false;
		}
		return readied;
	}

	public void doDownload(TaskCallback callback) {
		if (readied) {
			if (!checkFinish()) {
				LOGGER.debug(String.format("开始下载[%s]...", file.getName()));
				taskCallback = callback;
				getFileWriteStream();
				download();
				downloading = true;
			}
			else {
				LOGGER.debug("文件已存在,无需下载:" + file.getName());
				if (callback != null) {
					callback.onTaskFinish();
				}
			}
		}
	}

	public void doDownloadSyn() throws DownloadException {
		if (readied) {
			if (!checkFinish()) {
				LOGGER.debug(String.format("开始下载[%s]...(同步)", file.getName()));
				getFileWriteStream();
				downloading = true;
				downloadSyn();
			}
			else {
				LOGGER.debug("文件已存在,无需下载:" + file.getName());
			}
		}
	}

	/** 中断下载 */
	public void doBreak() {
		if (downloading) {
			finish();
		}
	}

	public void setConnectHandler(ConnectHandler connectHandler) {
		this.connectHandler = connectHandler;
		this.connectHandler.registerMsgType(DownloadResp.class);
	}

	private void loadFile() throws IOException {
		file = new File(dstPath, downloadFileInfo.getFileVerInfo().getName());
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
			dataLength = 0;
		}
		else {
			dataLength = file.length();
			LOGGER.debug(String.format("载入历史下载进度[%s]:%s/%s,%f%%", file.getName(), dataLength, fileSize,
					getCurDownloadProgress()));
		}
	}

	private void getFileWriteStream() {
		try {
			fileWriteStream = new BufferedOutputStream(new FileOutputStream(file, true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void closeFileWriteStream() throws IOException {
		fileWriteStream.close();
	}

	private void download() {
		prepare();
		reqData(new Closure() {
			@Override
			public void execute(Object msg) {
				try {
					executeResp((DownloadResp) msg);
					if (!checkFinish()) {
						download();
					}
					else {
						finish();
						LOGGER.debug(String.format("下载完成:%s", file.getName()));
						if (taskCallback != null) {
							taskCallback.onTaskFinish();
						}
					}
				} catch (IOException e) {
					finish();
					LOGGER.error(String.format("下载失败:%s[%f%%]", file.getName(), getCurDownloadProgress()));
					e.printStackTrace();
					if (taskCallback != null) {
						taskCallback.onTaskBreak();
					}
				}
			}
		});
	}

	private void downloadSyn() throws DownloadException {
		prepare();
		try {
			executeResp(reqDataSyn());
			if (!checkFinish()) {
				downloadSyn();
			}
			else {
				LOGGER.debug("下载完成!");
			}
		} catch (Exception e) {
			LOGGER.error(String.format("下载失败:%s[%s]", file.getName(), getCurDownloadProgress()));
			throw new DownloadException();
		} finally {
			finish();
		}
	}

	private void prepare() {
		FileDownloadInfo info;
		if (downloadInfoList.isEmpty()) {
			info = new FileDownloadInfo();
			info.setFileVerInfo(downloadFileInfo.getFileVerInfo());
			downloadInfoList.add(info);
		}
		else {
			info = downloadInfoList.get(0);
		}
		info.setOffset((int) dataLength);

		if (downloadRequest == null) {
			downloadRequest = new DownloadReq();
			downloadRequest.setFileDownloadInfos(downloadInfoList);
		}
	}

	private void writeData(byte[] data) throws IOException {
		fileWriteStream.write(data);
	}

	private boolean checkFinish() {
		return dataLength == fileSize;
	}

	private float getCurDownloadProgress() {
		return (dataLength * 1.0F / fileSize) * 100;
	}

	private void finish() {
		try {
			fileWriteStream.flush();
			closeFileWriteStream();
			fileWriteStream = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		downloading = false;
	}

	private void reqData(Closure callback) {
		connectHandler.sendWithReceiver(downloadRequest, callback);
	}

	private DownloadResp reqDataSyn() throws InterruptedException, ExecutionException, TimeoutException {
		return connectHandler.sendAndWait(downloadRequest);
	}

	private void executeResp(DownloadResp resp) throws IOException {
		FileDownloadData[] dataArray = resp.getFileDownloadDatas();
		byte[] db = null;
		if (dataArray != null && dataArray.length > 0 && (db = dataArray[0].getData()) != null && db.length > 0) {
			writeData(db);
			dataLength += db.length;
			float progress = getCurDownloadProgress();
			LOGGER.debug(String.format("下载进度:%s/%s,%f%%", dataLength, fileSize, progress));
			notifyProgress(progress);
		}
		else {
			throw new RuntimeException("dataResp=" + resp);
		}
	}

	private void notifyProgress(float progress) {
		if (progressListener != null) {
			progressListener.onTaskProgress(this, progress);
		}
	}

	@Override
	public String toString() {
		return "DownloadTask [fileName=" + downloadFileInfo.getFileVerInfo().getName() + ", dstPath=" + dstPath + "]";
	}

}
