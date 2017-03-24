// Copyright © 2017 DWANGO Co., Ltd.
package jp.co.dwango.cbb.dc;

import junit.framework.Assert;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import jp.co.dwango.cbb.db.MemoryQueue;
import jp.co.dwango.cbb.db.MemoryQueueDataBus;

public class DataChannelTest {
	private MemoryQueueDataBus senderDataBus;
	private MemoryQueueDataBus receiverDataBus;
	private DataChannel senderChannel;
	private DataChannel receiverChannel;
	private boolean isCalledHandler1;
	private boolean isCalledHandler2;
	private volatile int counter;

	@Before
	public void setUp() {
		DataChannel.logging(false);
	}

	private void before() {
		MemoryQueue queue1 = new MemoryQueue();
		MemoryQueue queue2 = new MemoryQueue();
		senderDataBus = new MemoryQueueDataBus(queue1, queue2);
		receiverDataBus = new MemoryQueueDataBus(queue2, queue1);
		senderChannel = new DataChannel(senderDataBus);
		receiverChannel = new DataChannel(receiverDataBus);
		isCalledHandler1 = false;
		isCalledHandler2 = false;
	}

	private void after() {
		receiverChannel.destroy();
		senderChannel.destroy();
		receiverDataBus.destroy();
		senderDataBus.destroy();
	}

	@Test
	public void 送信側がsendPushすると受信側はonPushで受け取れる() {
		before();
		final Object testPacket = new JSONArray().put("aaa").put("bbb").put(333);
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				isCalledHandler1 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				isCalledHandler2 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		senderChannel.sendPush(testPacket);
		Assert.assertTrue(isCalledHandler1);
		Assert.assertTrue(isCalledHandler2);
		after();
	}

	@Test
	public void 送信側がcallbackがnullでsendRequestすると受信側はonPushで受け取れる() {
		before();
		final Object testPacket = new JSONArray().put("aaa").put("bbb").put(333);
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				isCalledHandler1 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				isCalledHandler2 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		senderChannel.sendRequest(testPacket, null);
		Assert.assertTrue(isCalledHandler1);
		Assert.assertTrue(isCalledHandler2);
		after();
	}

	@Test
	public void 受信側でremoveHandlerされたhandlerは発火しない() {
		before();
		final Object testPacket = new JSONArray().put("aaa").put("bbb").put(333);
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				isCalledHandler1 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		DataChannelHandler testHandler = new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		};
		receiverChannel.addHandler(testHandler);
		receiverChannel.removeHandler(testHandler);
		senderChannel.sendPush(testPacket);
		Assert.assertTrue(isCalledHandler1);
		after();
	}

	@Test
	public void 受信側でremoveAllHandlersを呼ぶと全てのhandlerがremoveされる() {
		before();
		final Object testPacket = new JSONArray().put("aaa").put("bbb").put(333);
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});
		receiverChannel.removeAllHandlers();
		senderChannel.sendPush(testPacket);
		after();
	}

	@Test
	public void 送信側がsendRequestすると受信側はonRequestで受け取り送信側にResponseを返す() {
		before();
		final Object testPacket = new JSONArray().put("aaa").put("bbb").put(333);
		final Object resultPacket = new JSONArray().put("ccc").put(555);
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				isCalledHandler1 = true;
				Assert.assertEquals(packet.toString(), testPacket.toString());
				callback.send(resultPacket);
			}
		});
		senderChannel.sendRequest(testPacket, new DataChannelResponseHandler() {
			@Override
			public void onResponse(Object packet) {
				isCalledHandler2 = true;
				Assert.assertEquals(packet.toString(), resultPacket.toString());
			}

			@Override
			public void onError(ErrorType errorType) {
				Assert.fail();
			}
		});
		Assert.assertTrue(isCalledHandler1);
		Assert.assertTrue(isCalledHandler2);
		after();
	}

	@Test
	public void データロスト() {
		before();
		senderChannel.sendPush("toBeLost");
		senderChannel.sendRequest("toBeLost", new DataChannelResponseHandler() {
			@Override
			public void onResponse(Object packet) {
				Assert.fail();
			}

			@Override
			public void onError(ErrorType errorType) {
				// 応答が到達せずに after でクローズされる
				Assert.assertEquals(ErrorType.Close, errorType);
			}
		});
		after();
	}

	@Test
	public void マルチスレッド() {
		before();
		final int tryCount = 1000;
		final int threadCount = 10;
		counter = 0;

		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				counter++;
				callback.send("Okay");
			}
		});

		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threadCount; i++) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < tryCount; i++) {
						senderChannel.sendRequest("Is this Okay #" + i, new DataChannelResponseHandler() {
							@Override
							public void onResponse(Object packet) {
								counter++;
							}

							@Override
							public void onError(ErrorType errorType) {
								Assert.fail();
							}
						});
					}
				}
			});
			threads[i].start();
		}

		try {
			for (Thread t : threads) {
				t.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(tryCount * 2 * threads.length, counter);
		after();
	}

	@Test
	public void 不正なパケット() {
		before();
		receiverChannel.addHandler(new DataChannelHandler() {
			@Override
			public void onPush(Object packet) {
				Assert.fail();
			}

			@Override
			public void onRequest(Object packet, DataChannelCallback callback) {
				Assert.fail();
			}
		});

		// 要素2以外のパケット
		senderDataBus.send(new JSONArray());

		// 期待通りでないデータ形式
		senderDataBus.send(new JSONArray().put(3).put(new JSONArray()));

		// 応答待ちをしていないRESPONSE
		senderDataBus.send(new JSONArray().put(3).put(new JSONArray().put("tag:0").put("format").put("string")));

		// 応答待ちをしていないERROR RESPONSE
		senderDataBus.send(new JSONArray().put(4).put(new JSONArray().put("tag:0").put("errorTYpe")));

		// 存在しないDataType
		senderDataBus.send(new JSONArray().put(2525).put(new JSONArray()));

		after();
	}
}
