package com.zhile.jx.update.download;

/** 下载回调接口
 * @author Hxuejie
 * @version 1.0.1 */
interface TaskCallback {
	/** 下载任务完成回调 */
	void onTaskFinish();

	/** 下载任务中断(出错)回调 */
	void onTaskBreak();
}
