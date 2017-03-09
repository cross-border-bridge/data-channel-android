// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

public class RequestTag {
	public String name;
	public int index;

	public RequestTag(String name, int index) {
		this.name = name;
		this.index = index;
	}

	public RequestTag(String token) {
		int colon = token.indexOf(':');
		if (-1 == colon || 0 == colon) {
			name = token;
			index = 0;
		} else {
			name = token.substring(0, colon);
			index = Integer.parseInt(token.substring(colon + 1));
		}
	}

	public String toString() {
		return name + ":" + index;
	}

	boolean equals(String name) {
		return this.name.equals(name);
	}
}
