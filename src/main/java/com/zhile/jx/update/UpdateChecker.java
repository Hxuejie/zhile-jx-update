package com.zhile.jx.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.easycluster.easycluster.core.Closure;

import com.zhile.jx.net.connect.ConnectHandler;
import com.zhile.jx.update.common.TerminalUtil;

import edu.hziee.nGame.fds.bto.FileLocationInfo;
import edu.hziee.nGame.fds.bto.FileVerInfo;
import edu.hziee.nGame.fds.bto.GetUpdateResult;
import edu.hziee.nGame.fds.bto.TerminalInfo;
import edu.hziee.nGame.fds.bto.xip.GetUpdateReq;
import edu.hziee.nGame.fds.bto.xip.GetUpdateResp;

public class UpdateChecker {

	private final Logger			LOGGER			= Logger.getLogger(UpdateChecker.class);
	private List<FileLocationInfo>	downloadFileInfoList;
	private boolean					updateFlag;
	private ConnectHandler			connectHandler;
	private ArrayList<FileVerInfo>	updateFileList	= new ArrayList<FileVerInfo>();
	private TerminalInfo			terminalInfo;

	public UpdateChecker() {
		downloadFileInfoList = new ArrayList<FileLocationInfo>();
		terminalInfo = new TerminalUtil().getTerminalInfo();
	}

	public void setUpdateFileList(Collection<FileVerInfo> updateFileList) {
		if (updateFileList != null && !updateFileList.isEmpty()) {
			this.updateFileList.clear();
			this.updateFileList.addAll(updateFileList);
			LOGGER.info("设置更新文件列表:" + this.updateFileList);
		}
	}

	public void setConnectHandler(ConnectHandler connectHandler) {
		this.connectHandler = connectHandler;
		this.connectHandler.registerMsgType(GetUpdateResp.class);
	}

	public List<FileLocationInfo> getDownloadFileInfoList(List<FileLocationInfo> outList) {
		List<FileLocationInfo> result = outList;
		if (downloadFileInfoList.isEmpty()) {
			if (result == null) {
				result = Collections.emptyList();
			}
		}
		else {
			if (result == null) {
				result = new ArrayList<FileLocationInfo>();
			}
			result.addAll(downloadFileInfoList);
		}
		return result;
	}

	public void checkUpdate(final CheckResultCallback callback) {
		if (updateAble()) {
			LOGGER.info("检测更新...");
			connectHandler.sendWithReceiver(packRequest(), new Closure() {
				@Override
				public void execute(Object msg) {
					executeResp((GetUpdateResp) msg);
					if (callback != null) {
						callback.onCheckResult();
					}
				}
			});
		}
	}

	/** 同步检测更新
	 * @return true需要更新,false不需要更新
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException */
	public boolean checkUpdateSyn() throws InterruptedException, ExecutionException, TimeoutException {
		if (updateAble()) {
			LOGGER.info("同步检测更新...");
			executeResp((GetUpdateResp) connectHandler.sendAndWait(packRequest()));
			return updateFlag;
		}
		return false;
	}

	/** 游戏是否要更新<BR>
	 * 该方法必须在调用{@link #checkUpdate()}后调用有效<BR>
	 * 当调用{@link #checkUpdateSyn()}同步方法时可不调用该方法,从其返回值中就可获得是否要更新游戏
	 * @return true要更新,false不要更新 */
	public boolean needUpdate() {
		return updateFlag;
	}

	private GetUpdateReq packRequest() {
		GetUpdateReq req = new GetUpdateReq();
		req.setTermInfo(terminalInfo);
		req.setFileVerInfos(updateFileList);
		return req;
	}

	/** 检测是否可以进行更新
	 * @return */
	private boolean updateAble() {
		return connectHandler != null && !updateFileList.isEmpty();
	}

	/** 设置下载更新文件条目
	 * @param files */
	private void setUpdateFiles(List<FileLocationInfo> files) {
		if (files != null && files.size() > 0) {
			downloadFileInfoList.addAll(files);
		}
	}

	private void executeResp(GetUpdateResp resp) {
		LOGGER.info("更新检测结果:" + resp);
		GetUpdateResult result = null;
		for (GetUpdateResult val : GetUpdateResult.values()) {
			if (val.getCode() == resp.getErrorCode()) {
				result = val;
				break;
			}
		}
		switch (result) {
		case SUCCESS:// 成功
			setUpdateFiles(resp.getFileLocationInfos());
			updateFlag = true;
			break;
		case FILE_INFO_INVLAID:// 文件信息无效
			break;
		case TERMINAL_INFO_INVALID:// 终端信息无效
			break;
		case UP_TO_DATE:// 无更新
			break;
		}
	}

}
