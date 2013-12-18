package com.zhile.jx.update.common;

import edu.hziee.nGame.fds.bto.TerminalInfo;

public class TerminalUtil {
	/** 获得设备信息
	 * @return */
	public TerminalInfo getTerminalInfo() {
		TerminalInfo terminalInfo = new TerminalInfo();
		// terminalInfo.setHsman(android.os.Build.MANUFACTURER);// 厂商名称
		terminalInfo.setHsman("厂商名称");
		// terminalInfo.setHstype(android.os.Build.MODEL);// 机型类型
		terminalInfo.setHstype("机型类型");
		// terminalInfo.setOsVer(android.os.Build.VERSION.RELEASE);// 操作系统版本
		terminalInfo.setOsVer("操作系统版本");// 操作系统版本
		terminalInfo.setScreenWidth((short) 1024);// 屏幕宽
		terminalInfo.setScreenHeight((short) 768);// 屏幕高
		terminalInfo.setRamSize((short) 256);// RAM大小
		terminalInfo.setImsi("国际移动用户识别码");// 国际移动用户识别码imsi
		terminalInfo.setImei("国际移动设备身份码");// 国际移动设备身份码imei
		terminalInfo.setAppId("");
		terminalInfo.setSmsCenter("");
		// terminalInfo.setNetworkType(networkType);
		// terminalInfo.setLac(lac);
		terminalInfo.setIp("ip");
		terminalInfo.setChannelId("MAIN");
		return terminalInfo;
	}
}
