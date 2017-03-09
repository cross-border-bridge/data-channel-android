// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

public enum ErrorType {
	Timeout,
	Close,
	Unknown;

	static ErrorType get(String errorType) {
		if (errorType.equals("Timeout")) {
			return Timeout;
		} else if (errorType.equals("Close")) {
			return Close;
		} else {
			return Unknown;
		}
	}
}
