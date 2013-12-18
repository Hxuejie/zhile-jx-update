package com.zhile.jx.update.download;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.zhile.jx.net.connect.ConnectHandler;

import edu.hziee.nGame.fds.bto.FileLocationInfo;

public class Downloader implements TaskProgressListener {
	public static final byte		STATE_NONE			= 0;
	public static final byte		STATE_READIED		= 1;
	public static final byte		STATE_DOWNLOADING	= 2;
	public static final byte		STATE_STOPED		= 3;
	public static final byte		STATE_FINISH		= 4;

	private final byte				DEF_MAX_TRY_COUNT	= 1;
	private final Logger			LOGGER				= Logger.getLogger(Downloader.class);

	private byte					state;
	private List<FileLocationInfo>	downloadFileList;
	private Stack<DownloadTask>		downloadTaskStack;
	private Set<DownloadTask>		downloadTaskSet;
	private final int				MAX_TASK_NUM;
	private int						maxTryCount			= DEF_MAX_TRY_COUNT;
	private String					dstPath;
	private ConnectHandler			connectHandler;
	private float					totalProgress;
	private DownloadListener		downloadListener;

	public Downloader(String dstPath) {
		this(3, dstPath);
	}

	public Downloader(int maxTaskNum, String dstPath) {
		if (maxTaskNum <= 0) {
			throw new IllegalArgumentException("maxTaskNum=" + maxTaskNum);
		}
		this.MAX_TASK_NUM = maxTaskNum;
		this.dstPath = dstPath;
		downloadFileList = new ArrayList<FileLocationInfo>();
		downloadTaskSet = new HashSet<DownloadTask>(MAX_TASK_NUM);
		downloadTaskStack = new Stack<DownloadTask>();
	}

	public void setConnectHandler(ConnectHandler connectHandler) {
		this.connectHandler = connectHandler;
	}

	public DownloadListener getDownloadListener() {
		return downloadListener;
	}

	public void setDownloadListener(DownloadListener downloadListener) {
		this.downloadListener = downloadListener;
	}

	public List<FileLocationInfo> getDownloadFileList() {
		return downloadFileList;
	}

	public void setDownloadFileList(List<FileLocationInfo> downloadFileList) {
		if (downloadFileList != null && downloadFileList.size() > 0) {
			this.downloadFileList.addAll(downloadFileList);
		}
	}

	public byte getState() {
		return state;
	}

	/** step:1
	 * @return */
	public synchronized boolean ready() {
		if (state == STATE_READIED) {
			return true;
		}
		pushAllDownloadTask();
		if (state != STATE_NONE || downloadTaskStack.isEmpty()) {
			return false;
		}
		state = STATE_READIED;
		LOGGER.debug("下载准备");
		return true;
	}

	/** step:2
	 * @throws IllegalStateException */
	public synchronized void start() {
		if (state != STATE_READIED) {
			throw new IllegalStateException("state=" + state);
		}
		state = STATE_DOWNLOADING;
		LOGGER.debug("启动下载");
		for (int i = 0; i < MAX_TASK_NUM && hasMoreDownloadTask(); ++i) {
			startOneDownloadTask();
		}
	}

	public synchronized void stop() {
		switch (state) {
		case STATE_NONE:
			throw new IllegalStateException("state=" + state);
		case STATE_READIED:
			state = STATE_STOPED;
			break;
		case STATE_DOWNLOADING:
			breakAllDownloadTask();
			if (downloadListener != null) {
				downloadListener.onDownloadBreak();
			}
			state = STATE_STOPED;
			break;
		case STATE_STOPED:
			break;
		case STATE_FINISH:
			break;
		}
	}

	public int getMaxTryCount() {
		return maxTryCount;
	}

	public void setMaxTryCount(int maxTryCount) {
		if (maxTryCount < 0) {
			throw new IllegalArgumentException("maxTryCount=" + maxTryCount);
		}
		this.maxTryCount = maxTryCount;
	}

	private void breakAllDownloadTask() {
		synchronized (downloadTaskStack) {
			for (DownloadTask task : downloadTaskSet) {
				task.doBreak();
				downloadTaskStack.push(task);
			}
			downloadTaskSet.clear();
		}
	}

	private void pushAllDownloadTask() {
		if (downloadTaskStack.isEmpty()) {
			for (FileLocationInfo info : downloadFileList) {
				DownloadTask task = new DownloadTask(info, dstPath);
				task.setConnectHandler(connectHandler);
				task.setProgressListener(this);
				downloadTaskStack.push(task);
				LOGGER.debug("新建下载任务:" + task);
			}
		}
	}

	private boolean hasMoreDownloadTask() {
		return !downloadTaskStack.isEmpty();
	}

	private void startOneDownloadTask() {
		synchronized (downloadTaskStack) {
			final DownloadTask dTask = downloadTaskStack.pop();
			LOGGER.debug("启动下载任务:" + dTask);
			if (dTask.ready()) {
				downloadTaskSet.add(dTask);
				dTask.doDownload(new TaskCallback() {
					private int	tryCount;

					@Override
					public void onTaskFinish() {
						LOGGER.debug("完成下载任务:" + dTask);
						downloadTaskSet.remove(dTask);
						if (hasMoreDownloadTask()) {
							startOneDownloadTask();
						}
						else {
							if (downloadTaskSet.isEmpty()) {
								state = STATE_FINISH;
								LOGGER.debug("完成所有下载任务");
								if (downloadListener != null) {
									downloadListener.onDownloadFinish();
								}
							}
						}
					}

					@Override
					public void onTaskBreak() {
						if (tryCount < maxTryCount) {
							LOGGER.debug("尝试重新启动下载任务:" + dTask);
							if (dTask.ready()) {
								dTask.doDownload(this);
								tryCount++;
							}
						}
						else {
							stop();
						}
					}
				});
			}
		}
	}

	@Override
	public void onTaskProgress(DownloadTask task, float progress) {
		if (downloadListener != null) {
			downloadListener.onDownloadProgress(this, task, progress, totalProgress);
		}
	}

}
