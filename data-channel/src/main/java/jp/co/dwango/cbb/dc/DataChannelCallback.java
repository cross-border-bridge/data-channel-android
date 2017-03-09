// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

public interface DataChannelCallback {
	/**
	 * 応答を返信する
	 *
	 * @param packet 応答データ
	 */
	void send(Object packet);
}
