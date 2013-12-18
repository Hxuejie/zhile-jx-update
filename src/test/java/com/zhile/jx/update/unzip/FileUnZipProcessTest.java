package com.zhile.jx.update.unzip;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zhile.jx.update.unzip.FileUnZipHandler;

public class FileUnZipProcessTest {

	public FileUnZipProcessTest() {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Before
	public void setUp() throws Exception {}

	@After
	public void tearDown() throws Exception {}

	@Test
	public void testUnZip() throws IOException {
		FileUnZipHandler unZipHandler = new FileUnZipHandler("D:/jxres.zip", "D:/123");
		unZipHandler.doUnzip();
	}

}
