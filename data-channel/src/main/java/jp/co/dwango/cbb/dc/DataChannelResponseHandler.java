// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

public interface DataChannelResponseHandler {
	/**
	 * コンテンツにsendした要求に対する応答を受信
	 *
	 * @param packet パケット
	 */
	void onResponse(Object packet);


	/**
	 * エラーを受信
	 *
	 * @param errorType エラー種別情報
	 */
	void onError(ErrorType errorType);
}
