package com.zhile.jx.update.download;

public interface DownloadListener {
	void onDownloadProgress(Downloader downloader, DownloadTask task, float taskProgress, float totalProgress);

	void onDownloadFinish();

	void onDownloadBreak();
}
