// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

public interface DataChannelHandler {
	/**
	 * コンテンツからパケットを受信（応答返信不要）
	 *
	 * @param packet パケット
	 */
	void onPush(Object packet);

	/**
	 * コンテンツからタグ付きのパケットを受信（DataChannel#sendResponse で応答を返信すること）
	 *
	 * @param packet   パケット
	 * @param callback 応答を返信するインタフェース
	 */
	void onRequest(Object packet, DataChannelCallback callback);
}

