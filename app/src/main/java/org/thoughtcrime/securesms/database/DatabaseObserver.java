package org.thoughtcrime.securesms.ryan.database;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;
import org.signal.core.util.concurrent.SignalExecutors;
import org.thoughtcrime.securesms.ryan.database.model.MessageId;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;
import org.thoughtcrime.securesms.ryan.service.webrtc.links.CallLinkRoomId;
import org.thoughtcrime.securesms.ryan.util.concurrent.SerialExecutor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Allows listening to database changes to varying degrees of specificity.
 * <p>
 * A replacement for the observer system in {@link DatabaseTable}. We should move to this over time.
 */
public class DatabaseObserver {

  private static final String KEY_CONVERSATION          = "Conversation:";
  private static final String KEY_VERBOSE_CONVERSATION  = "VerboseConversation:";
  private static final String KEY_CONVERSATION_LIST     = "ConversationList";
  private static final String KEY_PAYMENT               = "Payment:";
  private static final String KEY_ALL_PAYMENTS          = "AllPayments";
  private static final String KEY_CHAT_COLORS           = "ChatColors";
  private static final String KEY_STICKERS              = "Stickers";
  private static final String KEY_STICKER_PACKS         = "StickerPacks";
  private static final String KEY_ATTACHMENTS           = "Attachments";
  private static final String KEY_MESSAGE_UPDATE        = "MessageUpdate:";
  private static final String KEY_MESSAGE_INSERT        = "MessageInsert:";
  private static final String KEY_NOTIFICATION_PROFILES = "NotificationProfiles";
  private static final String KEY_RECIPIENT             = "Recipient";
  private static final String KEY_STORY_OBSERVER        = "Story";
  private static final String KEY_SCHEDULED_MESSAGES    = "ScheduledMessages";
  private static final String KEY_CONVERSATION_DELETES  = "ConversationDeletes";

  private static final String KEY_CALL_UPDATES      = "CallUpdates";
  private static final String KEY_CALL_LINK_UPDATES = "CallLinkUpdates";
  private static final String KEY_IN_APP_PAYMENTS   = "InAppPayments";
  private static final String KEY_CHAT_FOLDER       = "ChatFolder";

  private final Executor    executor;

  private final Set<Observer>                      conversationListObservers;
  private final Map<Long, Set<Observer>>           conversationObservers;
  private final Map<Long, Set<Observer>>           verboseConversationObservers;
  private final Map<Long, Set<Observer>>           conversationDeleteObservers;
  private final Map<UUID, Set<Observer>>           paymentObservers;
  private final Map<Long, Set<Observer>>           scheduledMessageObservers;
  private final Set<Observer>                      allPaymentsObservers;
  private final Set<Observer>                      chatColorsObservers;
  private final Set<Observer>                      stickerObservers;
  private final Set<Observer>                      stickerPackObservers;
  private final Set<Observer>                      attachmentUpdatedObservers;
  private final Set<Observer>                      attachmentDeletedObservers;
  private final Set<MessageObserver>               messageUpdateObservers;
  private final Map<Long, Set<MessageObserver>>    messageInsertObservers;
  private final Set<Observer>                      notificationProfileObservers;
  private final Map<RecipientId, Set<Observer>>    storyObservers;
  private final Set<Observer>                      callUpdateObservers;
  private final Map<CallLinkRoomId, Set<Observer>> callLinkObservers;
  private final Set<InAppPaymentObserver>          inAppPaymentObservers;
  private final Set<Observer>                      chatFolderObservers;

  public DatabaseObserver() {
    this.executor                     = new SerialExecutor(SignalExecutors.BOUNDED);
    this.conversationListObservers    = new HashSet<>();
    this.conversationObservers        = new HashMap<>();
    this.verboseConversationObservers = new HashMap<>();
    this.conversationDeleteObservers  = new HashMap<>();
    this.paymentObservers             = new HashMap<>();
    this.allPaymentsObservers         = new HashSet<>();
    this.chatColorsObservers          = new HashSet<>();
    this.stickerObservers             = new HashSet<>();
    this.stickerPackObservers         = new HashSet<>();
    this.attachmentUpdatedObservers   = new HashSet<>();
    this.attachmentDeletedObservers   = new HashSet<>();
    this.messageUpdateObservers       = new HashSet<>();
    this.messageInsertObservers       = new HashMap<>();
    this.notificationProfileObservers = new HashSet<>();
    this.storyObservers               = new HashMap<>();
    this.scheduledMessageObservers    = new HashMap<>();
    this.callUpdateObservers          = new HashSet<>();
    this.callLinkObservers            = new HashMap<>();
    this.inAppPaymentObservers        = new HashSet<>();
    this.chatFolderObservers          = new HashSet<>();
  }

  public void registerConversationListObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      conversationListObservers.add(listener);
    });
  }

  public void registerConversationObserver(long threadId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(conversationObservers, threadId, listener);
    });
  }

  public void registerVerboseConversationObserver(long threadId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(verboseConversationObservers, threadId, listener);
    });
  }

  public void registerConversationDeleteObserver(long threadId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(conversationDeleteObservers, threadId, listener);
    });
  }

  public void registerPaymentObserver(@NonNull UUID paymentId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(paymentObservers, paymentId, listener);
    });
  }

  public void registerAllPaymentsObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      allPaymentsObservers.add(listener);
    });
  }

  public void registerChatColorsObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      chatColorsObservers.add(listener);
    });
  }

  public void registerStickerObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      stickerObservers.add(listener);
    });
  }

  public void registerStickerPackObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      stickerPackObservers.add(listener);
    });
  }

  public void registerAttachmentUpdatedObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      attachmentUpdatedObservers.add(listener);
    });
  }

  public void registerAttachmentDeletedObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      attachmentDeletedObservers.add(listener);
    });
  }

  public void registerMessageUpdateObserver(@NonNull MessageObserver listener) {
    executor.execute(() -> {
      messageUpdateObservers.add(listener);
    });
  }

  public void registerMessageInsertObserver(long threadId, @NonNull MessageObserver listener) {
    executor.execute(() -> {
      registerMapped(messageInsertObservers, threadId, listener);
    });
  }

  public void registerNotificationProfileObserver(@NotNull Observer listener) {
    executor.execute(() -> {
      notificationProfileObservers.add(listener);
    });
  }

  /**
   * Adds an observer which will be notified whenever a new Story message is inserted into the database.
   */
  public void registerStoryObserver(@NonNull RecipientId recipientId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(storyObservers, recipientId, listener);
    });
  }

  public void registerScheduledMessageObserver(long threadId, @NonNull Observer listener) {
    executor.execute(() -> {
      registerMapped(scheduledMessageObservers, threadId, listener);
    });
  }

  public void registerCallUpdateObserver(@NonNull Observer observer) {
    executor.execute(() -> callUpdateObservers.add(observer));
  }

  public void registerCallLinkObserver(@NonNull CallLinkRoomId callLinkRoomId, @NonNull Observer observer) {
    executor.execute(() -> {
      registerMapped(callLinkObservers, callLinkRoomId, observer);
    });
  }

  public void registerInAppPaymentObserver(@NonNull InAppPaymentObserver observer) {
    executor.execute(() -> inAppPaymentObservers.add(observer));
  }

  public void registerChatFolderObserver(@NonNull Observer observer) {
    executor.execute(() -> chatFolderObservers.add(observer));
  }

  public void unregisterObserver(@NonNull Observer listener) {
    executor.execute(() -> {
      conversationListObservers.remove(listener);
      unregisterMapped(conversationObservers, listener);
      unregisterMapped(verboseConversationObservers, listener);
      unregisterMapped(paymentObservers, listener);
      chatColorsObservers.remove(listener);
      stickerObservers.remove(listener);
      stickerPackObservers.remove(listener);
      attachmentUpdatedObservers.remove(listener);
      attachmentDeletedObservers.remove(listener);
      notificationProfileObservers.remove(listener);
      unregisterMapped(storyObservers, listener);
      unregisterMapped(scheduledMessageObservers, listener);
      unregisterMapped(conversationDeleteObservers, listener);
      callUpdateObservers.remove(listener);
      unregisterMapped(callLinkObservers, listener);
      chatFolderObservers.remove(listener);
    });
  }

  public void unregisterObserver(@NonNull MessageObserver listener) {
    executor.execute(() -> {
      messageUpdateObservers.remove(listener);
      unregisterMapped(messageInsertObservers, listener);
    });
  }

  public void unregisterObserver(@NonNull InAppPaymentObserver listener) {
    executor.execute(() -> {
      inAppPaymentObservers.remove(listener);
    });
  }

  public void notifyConversationListeners(Set<Long> threadIds) {
    for (long threadId : threadIds) {
      notifyConversationListeners(threadId);
    }
  }

  public void notifyConversationListeners(long threadId) {
    runPostSuccessfulTransaction(KEY_CONVERSATION + threadId, () -> {
      notifyMapped(conversationObservers, threadId);
      notifyMapped(verboseConversationObservers, threadId);
    });
  }

  public void notifyVerboseConversationListeners(Set<Long> threadIds) {
    for (long threadId : threadIds) {
      runPostSuccessfulTransaction(KEY_VERBOSE_CONVERSATION + threadId, () -> {
        notifyMapped(verboseConversationObservers, threadId);
      });
    }
  }

  public void notifyConversationDeleteListeners(Set<Long> threadIds) {
    for (long threadId : threadIds) {
      notifyConversationDeleteListeners(threadId);
    }
  }

  public void notifyConversationDeleteListeners(long threadId) {
    runPostSuccessfulTransaction(KEY_CONVERSATION_DELETES + threadId, () -> {
      notifyMapped(conversationDeleteObservers, threadId);
    });
  }

  public void notifyConversationListListeners() {
    runPostSuccessfulTransaction(KEY_CONVERSATION_LIST, () -> {
      for (Observer listener : conversationListObservers) {
        listener.onChanged();
      }
    });
  }

  public void notifyPaymentListeners(@NonNull UUID paymentId) {
    runPostSuccessfulTransaction(KEY_PAYMENT + paymentId.toString(), () -> {
      notifyMapped(paymentObservers, paymentId);
    });
  }

  public void notifyAllPaymentsListeners() {
    runPostSuccessfulTransaction(KEY_ALL_PAYMENTS, () -> {
      notifySet(allPaymentsObservers);
    });
  }

  public void notifyChatColorsListeners() {
    runPostSuccessfulTransaction(KEY_CHAT_COLORS, () -> {
      for (Observer chatColorsObserver : chatColorsObservers) {
        chatColorsObserver.onChanged();
      }
    });
  }

  public void notifyStickerObservers() {
    runPostSuccessfulTransaction(KEY_STICKERS, () -> {
      notifySet(stickerObservers);
    });
  }

  public void notifyStickerPackObservers() {
    runPostSuccessfulTransaction(KEY_STICKER_PACKS, () -> {
      notifySet(stickerPackObservers);
    });
  }

  public void notifyAttachmentUpdatedObservers() {
    runPostSuccessfulTransaction(KEY_ATTACHMENTS, () -> {
      notifySet(attachmentUpdatedObservers);
    });
  }

  public void notifyAttachmentDeletedObservers() {
    runPostSuccessfulTransaction(KEY_ATTACHMENTS, () -> {
      notifySet(attachmentDeletedObservers);
      notifySet(attachmentUpdatedObservers);
    });
  }

  public void notifyMessageUpdateObservers(@NonNull MessageId messageId) {
    runPostSuccessfulTransaction(KEY_MESSAGE_UPDATE + messageId.toString(), () -> {
      messageUpdateObservers.stream().forEach(l -> l.onMessageChanged(messageId));
    });
  }

  public void notifyMessageInsertObservers(long threadId, @NonNull MessageId messageId) {
    runPostSuccessfulTransaction(KEY_MESSAGE_INSERT + messageId, () -> {
      Set<MessageObserver> listeners = messageInsertObservers.get(threadId);

      if (listeners != null) {
        listeners.stream().forEach(l -> l.onMessageChanged(messageId));
      }
    });
  }

  public void notifyNotificationProfileObservers() {
    runPostSuccessfulTransaction(KEY_NOTIFICATION_PROFILES, () -> {
      notifySet(notificationProfileObservers);
    });
  }

  public void notifyRecipientChanged(@NonNull RecipientId recipientId) {
    SignalDatabase.runPostSuccessfulTransaction(KEY_RECIPIENT + recipientId.serialize(), () -> {
      Recipient.live(recipientId).refresh();
    });
  }

  public void notifyStoryObservers(@NonNull RecipientId recipientId) {
    runPostSuccessfulTransaction(KEY_STORY_OBSERVER, () -> {
      notifyMapped(storyObservers, recipientId);
    });
  }

  public void notifyStoryObservers(@NonNull Collection<RecipientId> recipientIds) {
    for (RecipientId recipientId : recipientIds) {
      runPostSuccessfulTransaction(KEY_STORY_OBSERVER, () -> {
        notifyMapped(storyObservers, recipientId);
      });
    }
  }

  public void notifyScheduledMessageObservers(long threadId) {
    runPostSuccessfulTransaction(KEY_SCHEDULED_MESSAGES + threadId, () -> {
      notifyMapped(scheduledMessageObservers, threadId);
    });
  }

  public void notifyCallUpdateObservers() {
    runPostSuccessfulTransaction(KEY_CALL_UPDATES, () -> notifySet(callUpdateObservers));
  }

  public void notifyCallLinkObservers(@NonNull CallLinkRoomId callLinkRoomId) {
    runPostSuccessfulTransaction(KEY_CALL_LINK_UPDATES, () -> notifyMapped(callLinkObservers, callLinkRoomId));
  }

  public void notifyInAppPaymentsObservers(@NonNull InAppPaymentTable.InAppPayment inAppPayment) {
    runPostSuccessfulTransaction(KEY_IN_APP_PAYMENTS, () -> {
      inAppPaymentObservers.forEach(item -> item.onInAppPaymentChanged(inAppPayment));
    });
  }

  public void notifyChatFolderObservers() {
    runPostSuccessfulTransaction(KEY_CHAT_FOLDER, () -> notifySet(chatFolderObservers));
  }

  private void runPostSuccessfulTransaction(@NonNull String dedupeKey, @NonNull Runnable runnable) {
    SignalDatabase.runPostSuccessfulTransaction(dedupeKey, () -> {
      executor.execute(runnable);
    });
  }

  private <K, V> void registerMapped(@NonNull Map<K, Set<V>> map, @NonNull K key, @NonNull V listener) {
    Set<V> listeners = map.get(key);

    if (listeners == null) {
      listeners = new HashSet<>();
    }

    listeners.add(listener);
    map.put(key, listeners);
  }

  private <K, V> void unregisterMapped(@NonNull Map<K, Set<V>> map, @NonNull V listener) {
    for (Map.Entry<K, Set<V>> entry : map.entrySet()) {
      entry.getValue().remove(listener);
    }
  }

  private static <K> void notifyMapped(@NonNull Map<K, Set<Observer>> map, @NonNull K key) {
    Set<Observer> listeners = map.get(key);

    if (listeners != null) {
      for (Observer listener : listeners) {
        listener.onChanged();
      }
    }
  }

  private static void notifySet(@NonNull Set<Observer> set) {
    for (final Observer observer : set) {
      observer.onChanged();
    }
  }

  /**
   * Blocks until the executor is empty. Only intended to be used for testing.
   */
  @VisibleForTesting
  void flush() {
    CountDownLatch latch = new CountDownLatch(1);
    executor.execute(latch::countDown);

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new AssertionError();
    }
  }

  public interface Observer {
    /**
     * Called when the relevant data changes. Executed on a serial executor, so don't do any
     * long-running tasks!
     */
    void onChanged();
  }

  public interface MessageObserver {
    void onMessageChanged(@NonNull MessageId messageId);
  }

  public interface InAppPaymentObserver {
    void onInAppPaymentChanged(@NonNull InAppPaymentTable.InAppPayment inAppPayment);
  }
}
