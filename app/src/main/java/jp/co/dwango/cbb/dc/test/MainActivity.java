// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc.test;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jp.co.dwango.cbb.db.DataBus;
import jp.co.dwango.cbb.db.WebViewDataBus;
import jp.co.dwango.cbb.dc.DataChannel;
import jp.co.dwango.cbb.dc.DataChannelHandler;
import jp.co.dwango.cbb.dc.DataChannelCallback;
import jp.co.dwango.cbb.dc.DataChannelResponseHandler;
import jp.co.dwango.cbb.dc.ErrorType;

public class MainActivity extends AppCompatActivity {
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// KITKAT以上の場合は Chrome でのデバッグを有効にする
		if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
			WebView.setWebContentsDebuggingEnabled(true);
		}
		setContentView(R.layout.activity_main);
		WebView webView = (WebView) findViewById(R.id.web_view);
		assert webView != null;

		// デバッグログ出力を有効化
		DataBus.logging(true);
		DataChannel.logging(true);

		// DataBusを用いるWebViewを指定してインスタンス化
		final DataBus dataBus = new WebViewDataBus(this, webView);

		// DataChannelを作成
		final DataChannel dataChannel = new DataChannel(dataBus);

		// DataChannelへの要求受け口を準備
		dataChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				String text = "received PUSH from js: packet=" + packet;
				Log.d("data-dataChannel-java", text);
				Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				String text = "received request from js: packet=" + packet;
				Log.d("data-dataChannel-java", text);
				callback.send("Thank you for your request!!!");
			}
		});

		// PUSH送信ボタンを準備
		View v = findViewById(R.id.send_push);
		assert v != null;
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dataChannel.sendPush("push from Java to JS");
			}
		});

		// REQUEST送信ボタンを準備
		v = findViewById(R.id.send_request);
		assert v != null;
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dataChannel.sendRequest("request from Java to JS", new DataChannelResponseHandler() {
					@Override
					public void onResponse(Object packet) {
						String text = "received result from js: packet=" + packet;
						Log.d("data-dataChannel-java", text);
						Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onError(ErrorType errorType) {
						Log.d("data-dataChannel-java", "received error-result from js: errorType=" + errorType);
					}
				});
			}
		});

		// DESTROYボタンを準備
		v = findViewById(R.id.destroy);
		assert v != null;
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dataChannel.destroy();
				dataBus.destroy();
			}
		});

		// WebView を 準備
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.d("CrossBorderBridge-js", consoleMessage.message() + " (src=" + consoleMessage.sourceId() + ", line=" + consoleMessage.lineNumber() + ")");
				return super.onConsoleMessage(consoleMessage);
			}
		});

		// WebView へコンテンツをロード
		webView.loadDataWithBaseURL("", loadTextFromAsset("html/index.html"), "text/html", "UTF-8", null);
	}

	private String loadTextFromAsset(String path) {
		InputStream is = null;
		BufferedReader br = null;
		StringBuilder result = new StringBuilder(16384);
		try {
			is = getAssets().open(path);
			br = new BufferedReader(new InputStreamReader(is));
			String str;
			while ((str = br.readLine()) != null) {
				result.append(str).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != is) try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (null != br) try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result.toString();
	}
}
