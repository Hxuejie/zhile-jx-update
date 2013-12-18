package com.zhile.jx.update;

import com.zhile.jx.net.connect.ConnectHandler;
import com.zhile.jx.update.download.DownloadListener;
import com.zhile.jx.update.download.Downloader;

public class Updater {
	private UpdateChecker	updateChecker;
	private Downloader		downloader;
	private ConnectHandler connectHandler;

	public Updater() {
		updateChecker = new UpdateChecker();
	}

	public void checkUpdate() {
//		updateChecker.checkUpdate(new CheckResultCallback() {
//			@Override
//			public void onCheckResult() {
//				downloader.setConnectHandler(connectHandler);
//				downloader.setDownloadFileList(list);
//				downloader.setDownloadListener(new DownloadListener() {
//			}
//		});
	}
}
