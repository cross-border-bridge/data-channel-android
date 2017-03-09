// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DataChannelWaitingResponseTable {
	private final Map<RequestTag, DataChannelResponseHandler> table = new ConcurrentHashMap<RequestTag, DataChannelResponseHandler>();

	DataChannelResponseHandler get(RequestTag tag) {
		Set<RequestTag> keys = table.keySet();
		for (RequestTag key : keys) {
			if (key.name.equals(tag.name)) {
				return table.remove(key);
			}
		}
		return null;
	}

	void put(RequestTag tag, DataChannelResponseHandler handler) {
		get(tag);
		table.put(tag, handler);
	}

	Map<RequestTag, DataChannelResponseHandler> popAllHandlers() {
		Map<RequestTag, DataChannelResponseHandler> returnTable = new HashMap<RequestTag, DataChannelResponseHandler>();
		for (RequestTag key : table.keySet()) {
			returnTable.put(key, table.remove(key));
		}
		return returnTable;
	}
}
