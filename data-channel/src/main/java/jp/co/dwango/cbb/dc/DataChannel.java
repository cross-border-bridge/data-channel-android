// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Map;

import jp.co.dwango.cbb.db.DataBus;
import jp.co.dwango.cbb.db.DataBusHandler;

public class DataChannel {
	private static final int DATA_TYPE_PUSH = 1;
	private static final int DATA_TYPE_REQUEST = 2;
	private static final int DATA_TYPE_RESPONSE = 3;
	private static final int DATA_TYPE_ERROR = 4;
	public final DataBus dataBus;
	private final DataChannelWaitingResponseTable waitingCallbacks = new DataChannelWaitingResponseTable();
	private DataBusHandler dataBusHandler;
	private ArrayList<DataChannelHandler> handlers = new ArrayList<DataChannelHandler>();
	private int latestTagNumber = 1;
	private boolean destroyed = false;

	/**
	 * チャネルを作成
	 *
	 * @param dataBus DataBusインスタンス
	 */
	public DataChannel(final DataBus dataBus) {
		this.dataBus = dataBus;
		this.dataBusHandler = new DataBusHandler() {
			@Override
			public void onReceive(JSONArray packet) {
				if (destroyed) return;
				try {
					// サイズチェック
					if (2 != packet.length()) {
						Logger.w("invalid packet received: " + packet.toString());
						return;
					}
					int dataType = packet.getInt(0);
					JSONArray data = packet.getJSONArray(1);
					switch (dataType) {
						case DATA_TYPE_PUSH: {
							Object channelPacket = data.get(0);
							if (0 == handlers.size()) {
								Logger.w("data lost: " + packet.toString());
							} else {
								for (DataChannelHandler handler : handlers) {
									handler.onPush(channelPacket);
								}
							}
							break;
						}
						case DATA_TYPE_REQUEST: {
							final RequestTag tag = new RequestTag(data.getString(0));
							Object channelPacket = data.get(1);
							if (0 == handlers.size()) {
								Logger.w("data lost: " + packet.toString());
							} else {
								for (DataChannelHandler handler : handlers) {
									handler.onRequest(channelPacket, new DataChannelCallback() {
										@Override
										public void send(Object packet) {
											sendResponse(tag.toString(), packet);
										}
									});
								}
							}
							break;
						}
						case DATA_TYPE_RESPONSE: {
							RequestTag tag = new RequestTag(data.getString(0));
							Object channelPacket = data.get(1);
							DataChannelResponseHandler callback = waitingCallbacks.get(tag);
							if (null == callback) {
								Logger.w("invalid response: " + packet.toString());
							} else {
								callback.onResponse(channelPacket);
							}
							break;
						}
						case DATA_TYPE_ERROR: {
							RequestTag tag = new RequestTag(data.getString(0));
							String error = data.getString(1);
							DataChannelResponseHandler callback = waitingCallbacks.get(tag);
							if (null == callback) {
								Logger.w("invalid response: " + packet.toString());
							} else {
								callback.onError(ErrorType.get(error));
							}
							break;
						}
						default:
							Logger.e("invalid packet: " + packet.toString());
					}
				} catch (JSONException e) {
					if (Logger.enabled) e.printStackTrace();
				}
			}
		};
		this.dataBus.addHandler(this.dataBusHandler);
	}

	/**
	 * ログ出力の有効化/無効化
	 *
	 * @param enabled true = 有効, false = 無効
	 */
	public static void logging(boolean enabled) {
		Logger.enabled = enabled;
	}

	/**
	 * リモート側 へ PUSH を 送信
	 *
	 * @param push PUSHデータ
	 */
	public void sendPush(Object push) {
		if (destroyed) return;
		JSONArray data = new JSONArray().put(push);
		JSONArray packet = new JSONArray();
		packet.put(DATA_TYPE_PUSH);
		packet.put(data);
		dataBus.send(packet);
	}

	/**
	 * リモート側 へ REQUEST を 送信
	 *
	 * @param request  REQUESTデータ
	 * @param callback 応答を受け取るハンドラ
	 */
	public void sendRequest(Object request, DataChannelResponseHandler callback) {
		if (destroyed) return;
		if (null == callback) {
			sendPush(request);
			return;
		}
		JSONArray data = new JSONArray();
		RequestTag tag = new RequestTag("A", latestTagNumber++);
		data.put(tag.toString());
		waitingCallbacks.put(tag, callback);
		data.put(request);
		JSONArray packet = new JSONArray();
		packet.put(DATA_TYPE_REQUEST);
		packet.put(data);
		dataBus.send(packet);
	}

	void sendResponse(String tag, Object response) {
		if (destroyed) return;
		JSONArray data = new JSONArray();
		data.put(tag);
		data.put(response);
		JSONArray packet = new JSONArray();
		packet.put(DATA_TYPE_RESPONSE);
		packet.put(data);
		dataBus.send(packet);
	}

	/**
	 * データ受信ハンドラを設定
	 *
	 * @param handler データ受信ハンドラ
	 */
	public void addHandler(DataChannelHandler handler) {
		if (destroyed || 0 <= handlers.indexOf(handler)) return;
		handlers.add(handler);
	}

	/**
	 * データ受信ハンドラを削除
	 *
	 * @param handler データ受信ハンドラ
	 */
	public void removeHandler(DataChannelHandler handler) {
		if (destroyed) return;
		handlers.remove(handler);
	}

	/**
	 * データ受信ハンドラを全て削除
	 */
	public void removeAllHandlers() {
		if (destroyed) return;
		handlers.clear();
	}

	/**
	 * 破棄
	 * responseが返って来ていないrequestがあったら、その全てにerrorを返す
	 */
	public void destroy() {
		if (destroyed) return;
		handlers.clear();
		dataBus.removeHandler(dataBusHandler);
		Map<RequestTag, DataChannelResponseHandler> waitingCallbacksMap = waitingCallbacks.popAllHandlers();
		for (RequestTag key : waitingCallbacksMap.keySet()) {
			waitingCallbacksMap.get(key).onError(ErrorType.Close);
		}
		destroyed = true;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			destroy();
		}
	}
}
