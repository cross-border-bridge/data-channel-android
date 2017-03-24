// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DataChannelWaitingResponseTable {
	private final Map<String, DataChannelResponseHandler> table = new ConcurrentHashMap<String, DataChannelResponseHandler>();
	private final Object locker = new Object();

	DataChannelResponseHandler get(RequestTag tag) {
		synchronized (locker) {
			Set<String> keys = table.keySet();
			String tagString = tag.toString();
			for (String key : keys) {
				if (key.equals(tagString)) {
					return table.remove(key);
				}
			}
		}
		return null;
	}

	void put(RequestTag tag, DataChannelResponseHandler handler) {
		synchronized (locker) {
			table.put(tag.toString(), handler);
		}
	}

	Map<RequestTag, DataChannelResponseHandler> popAllHandlers() {
		Map<RequestTag, DataChannelResponseHandler> returnTable = new HashMap<RequestTag, DataChannelResponseHandler>();
		synchronized (locker) {
			for (String key : table.keySet()) {
				returnTable.put(new RequestTag(key), table.remove(key));
			}
		}
		return returnTable;
	}
}
