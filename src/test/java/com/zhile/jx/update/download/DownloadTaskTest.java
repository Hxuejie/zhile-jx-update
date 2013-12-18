package com.zhile.jx.update.download;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zhile.jx.net.connect.ConnectHandler;
import com.zhile.jx.update.UpdateCheckerTest;

import edu.hziee.nGame.fds.bto.FileLocationInfo;
import edu.hziee.nGame.fds.bto.FileVerInfo;

public class DownloadTaskTest {

	private Logger				LOGGER	= Logger.getLogger(UpdateCheckerTest.class);
	private FileLocationInfo	fli;
	private ConnectHandler		connectHandler;

	public DownloadTaskTest() {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Before
	public void setup() throws Exception {
		LOGGER.info("建立连接...");
		connectHandler = ConnectHandler.createConnect("172.23.1.253", 6666);
		init();
	}

	private void init() {
		fli = new FileLocationInfo();
		fli.setFileSize(1913698);
		fli.setFileUrl("");
		FileVerInfo fvi = new FileVerInfo();
		fvi.setName("wxclient.apk");
		fvi.setVer(1);
		fli.setFileVerInfo(fvi);
	}

	 @Test
	public void testDownloadTaskSyn() throws DownloadException {
		String path = "D:/1/2/";
		DownloadTask downloadTask = new DownloadTask(fli, path);
		downloadTask.setConnectHandler(connectHandler);
		if (downloadTask.ready()) {
			downloadTask.doDownloadSyn();
		}
		File file = new File(path, fli.getFileVerInfo().getName());
		Assert.assertTrue(file.exists());
	}

	@Test
	public void testDownloadTask() throws DownloadException, InterruptedException {
		final String path = "D:/1/2/";
		DownloadTask downloadTask = new DownloadTask(fli, path);
		downloadTask.setConnectHandler(connectHandler);
		final Thread t = Thread.currentThread();
		if (downloadTask.ready()) {
			downloadTask.doDownload(new TaskCallback() {
				@Override
				public void onTaskFinish() {
					LOGGER.info("下载完成!");
					synchronized (t) {
						t.notifyAll();
					}
					File file = new File(path, fli.getFileVerInfo().getName());
					Assert.assertTrue(file.exists());
				}

				@Override
				public void onTaskBreak() {
					LOGGER.error("下载中断!");
					synchronized (t) {
						t.notifyAll();
					}
				}
			});
			if (downloadTask.isDownloading()) {
				synchronized (t) {
					t.wait();
				}
			}
		}
	}

	@After
	public void end() {
		LOGGER.info("关闭连接...");
		connectHandler.closeConnect();
	}
}
