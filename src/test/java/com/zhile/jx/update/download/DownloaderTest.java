package com.zhile.jx.update.download;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zhile.jx.net.connect.ConnectHandler;

import edu.hziee.common.tcp.client.ChannelListener;
import edu.hziee.nGame.fds.bto.FileLocationInfo;
import edu.hziee.nGame.fds.bto.FileVerInfo;

public class DownloaderTest {

	private Logger					LOGGER	= Logger.getLogger(DownloaderTest.class);
	private List<FileLocationInfo>	list;
	private ConnectHandler			connectHandler;

	public DownloaderTest() {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Before
	public void setUp() throws Exception {
		list = new ArrayList<FileLocationInfo>();
		list.add(create("wxclient.apk", 1913698));
		list.add(create("wxdownload.zip", 41637429));
		connectHandler = ConnectHandler.createConnect("172.23.1.253", 6666);
	}

	@After
	public void tearDown() throws Exception {
		connectHandler.closeConnect();
	}

	@Test
	public void testDownload() throws InterruptedException {
		Downloader downloader = new Downloader(3, "D:/333");
		final Thread t = Thread.currentThread();
		downloader.setConnectHandler(connectHandler);
		downloader.setDownloadFileList(list);
		downloader.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadProgress(Downloader downloader, DownloadTask task, float progress, float totalProgress) {
				LOGGER.info(String
						.format("进度:%s,%f%%", task.getDownloadFileInfo().getFileVerInfo().getName(), progress));
			}

			@Override
			public void onDownloadFinish() {
				LOGGER.info("下载完成");
				synchronized (t) {
					t.notifyAll();
				}
			}

			@Override
			public void onDownloadBreak() {
				LOGGER.info("下载中断");
				synchronized (t) {
					t.notifyAll();
				}
			}
		});
		if (downloader.ready()) {
			downloader.start();
		}
		connectHandler.setConnectListener(new ChannelListener() {
			@Override
			public void onChannelClosed(Channel channel) {
				synchronized (t) {
					t.notifyAll();
				}
			}
		});
		if (downloader.getState() == Downloader.STATE_DOWNLOADING) {
			synchronized (t) {
				t.wait();
			}
		}
	}

	private FileLocationInfo create(String name, int size) {
		FileLocationInfo fli = new FileLocationInfo();
		fli.setFileSize(size);
		fli.setFileUrl("");
		FileVerInfo fvi = new FileVerInfo();
		fvi.setName(name);
		fvi.setVer(1);
		fli.setFileVerInfo(fvi);
		return fli;
	}

}
