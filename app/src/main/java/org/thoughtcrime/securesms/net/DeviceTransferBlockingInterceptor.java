package org.thoughtcrime.securesms.ryan.net;

import androidx.annotation.NonNull;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Provide a way to block network access while performing a device transfer.
 */
public final class DeviceTransferBlockingInterceptor implements Interceptor {

  private static final String TAG = Log.tag(DeviceTransferBlockingInterceptor.class);

  private static final DeviceTransferBlockingInterceptor INSTANCE = new DeviceTransferBlockingInterceptor();

  private volatile boolean blockNetworking;

  public static DeviceTransferBlockingInterceptor getInstance() {
    return INSTANCE;
  }

  public DeviceTransferBlockingInterceptor() {
    this.blockNetworking = SignalStore.misc().isOldDeviceTransferLocked();
  }

  @Override
  public @NonNull Response intercept(@NonNull Chain chain) throws IOException {
    if (!blockNetworking) {
      return chain.proceed(chain.request());
    }

    Log.w(TAG, "Preventing request because in transfer mode.");
    return new Response.Builder().request(chain.request())
                                 .protocol(Protocol.HTTP_1_1)
                                 .receivedResponseAtMillis(System.currentTimeMillis())
                                 .message("")
                                 .body(ResponseBody.create(null, ""))
                                 .code(500)
                                 .build();
  }

  public boolean isBlockingNetwork() {
    return blockNetworking;
  }

  public void blockNetwork() {
    blockNetworking = true;
    AppDependencies.resetNetwork();
  }

  public void unblockNetwork() {
    blockNetworking = false;
    SignalExecutors.UNBOUNDED.execute(AppDependencies::startNetwork);
  }
}
