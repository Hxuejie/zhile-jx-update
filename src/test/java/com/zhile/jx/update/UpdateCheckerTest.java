package com.zhile.jx.update;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zhile.jx.net.connect.ConnectHandler;

import edu.hziee.nGame.fds.bto.FileLocationInfo;
import edu.hziee.nGame.fds.bto.FileVerInfo;

public class UpdateCheckerTest {
	private ConnectHandler	connectHandler;
	private Logger			LOGGER	= Logger.getLogger(UpdateCheckerTest.class);

	public UpdateCheckerTest() {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Before
	public void setUp() throws Exception {
		LOGGER.info("建立连接...");
		connectHandler = ConnectHandler.createConnect("172.23.1.253", 6666);
		connectHandler.setDebugEnabled();
	}

	@Test
	public void testCheckUpdateSyn() throws InterruptedException, ExecutionException, TimeoutException {
		UpdateChecker uc = new UpdateChecker();
		uc.setConnectHandler(connectHandler);
		uc.setUpdateFileList(getFileInfoList());
		assertTrue(uc.checkUpdateSyn());
		List<FileLocationInfo> dList = uc.getDownloadFileInfoList(null);
		LOGGER.info("下载文件列表:" + dList);
		assertTrue(!dList.isEmpty());
	}

	@Test
	public void testCheckUpdate() throws InterruptedException {
		final UpdateChecker uc = new UpdateChecker();
		uc.setConnectHandler(connectHandler);
		uc.setUpdateFileList(getFileInfoList());
		uc.checkUpdate(new CheckResultCallback() {
			@Override
			public void onCheckResult() {
				assertTrue(uc.needUpdate());
			}
		});
		Thread.sleep(5000);
		List<FileLocationInfo> dList = uc.getDownloadFileInfoList(null);
		assertTrue(!dList.isEmpty());
	}

	private List<FileVerInfo> getFileInfoList() {
		List<FileVerInfo> list = new ArrayList<FileVerInfo>();
		FileVerInfo fvi = new FileVerInfo();
		fvi.setName("wxclient.apk");
		fvi.setVer(1);
		list.add(fvi);
		return list;
	}

	@After
	public void tearDown() throws Exception {
		LOGGER.info("关闭连接...");
		connectHandler.closeConnect();
	}

}
