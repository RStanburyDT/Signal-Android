package org.thoughtcrime.securesms.ryan.recipients;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.signal.core.util.ThreadUtil;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.database.DistributionListTables;
import org.thoughtcrime.securesms.ryan.database.GroupTable;
import org.thoughtcrime.securesms.ryan.database.RecipientTable;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.database.model.RecipientRecord;
import org.thoughtcrime.securesms.ryan.util.livedata.LiveDataUtil;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public final class LiveRecipient {

  private static final String TAG = Log.tag(LiveRecipient.class);

  private final Context                       context;
  private final MutableLiveData<Recipient>    liveData;
  private final LiveData<Recipient>           observableLiveData;
  private final LiveData<Recipient>           observableLiveDataResolved;
  private final Set<RecipientForeverObserver> observers;
  private final Observer<Recipient>           foreverObserver;
  private final AtomicReference<Recipient>    recipient;
  private final RecipientTable                recipientTable;
  private final GroupTable                    groupDatabase;
  private final DistributionListTables        distributionListTables;
  private final MutableLiveData<Object>       refreshForceNotify;
  private final BehaviorSubject<Recipient>    subject;

  LiveRecipient(@NonNull Context context, @NonNull Recipient defaultRecipient) {
    this.context                  = context.getApplicationContext();
    this.liveData                 = new MutableLiveData<>(defaultRecipient);
    this.subject                  = BehaviorSubject.createDefault(defaultRecipient);
    this.recipient                = new AtomicReference<>(defaultRecipient);
    this.recipientTable           = SignalDatabase.recipients();
    this.groupDatabase            = SignalDatabase.groups();
    this.distributionListTables   = SignalDatabase.distributionLists();
    this.observers                = new CopyOnWriteArraySet<>();
    this.foreverObserver          = recipient -> {
      ThreadUtil.postToMain(() -> {
        for (RecipientForeverObserver o : observers) {
          o.onRecipientChanged(recipient);
        }
      });
    };
    this.refreshForceNotify = new MutableLiveData<>(new Object());
    this.observableLiveData = LiveDataUtil.combineLatest(LiveDataUtil.distinctUntilChanged(liveData, Recipient::hasSameContent),
                                                         refreshForceNotify,
                                                         (recipient, force) -> recipient);
    this.observableLiveDataResolved = LiveDataUtil.filter(this.observableLiveData, r -> !r.isResolving());
  }

  public @NonNull RecipientId getId() {
    return recipient.get().getId();
  }

  /**
   * @return A recipient that may or may not be fully-resolved.
   */
  public @NonNull Recipient get() {
    return recipient.get();
  }

  /**
   * @return An rx-flavored {@link Observable}.
   */
  public @NonNull Observable<Recipient> observable() {
    return subject;
  }

  /**
   * Watch the recipient for changes. The callback will only be invoked if the provided lifecycle is
   * in a valid state. No need to remove the observer. If you do wish to remove the observer (if,
   * for instance, you wish to remove the listener before the end of the owner's lifecycle), you can
   * use {@link #removeObservers(LifecycleOwner)}.
   */
  public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Recipient> observer) {
    ThreadUtil.postToMain(() -> observableLiveData.observe(owner, observer));
  }

  /**
   * Removes observer of this data.
   */
  public void removeObserver(@NonNull Observer<Recipient> observer) {
    ThreadUtil.runOnMain(() -> observableLiveData.removeObserver(observer));
  }

  /**
   * Removes all observers of this data registered for the given LifecycleOwner.
   */
  public void removeObservers(@NonNull LifecycleOwner owner) {
    ThreadUtil.runOnMain(() -> observableLiveData.removeObservers(owner));
  }

  /**
   * Watch the recipient for changes. The callback could be invoked at any time. You MUST call
   * {@link #removeForeverObserver(RecipientForeverObserver)} when finished. You should use
   * {@link #observe(LifecycleOwner, Observer<Recipient>)} if possible, as it is lifecycle-safe.
   */
  public void observeForever(@NonNull RecipientForeverObserver observer) {
    ThreadUtil.postToMain(() -> {
      if (observers.isEmpty()) {
        observableLiveData.observeForever(foreverObserver);
      }
      observers.add(observer);
    });
  }

  /**
   * Unsubscribes the provided {@link RecipientForeverObserver} from future changes.
   */
  public void removeForeverObserver(@NonNull RecipientForeverObserver observer) {
    ThreadUtil.postToMain(() -> {
      observers.remove(observer);

      if (observers.isEmpty()) {
        observableLiveData.removeObserver(foreverObserver);
      }
    });
  }

  /**
   * @return A fully-resolved version of the recipient. May require reading from disk.
   */
  @WorkerThread
  public @NonNull Recipient resolve() {
    Recipient current = recipient.get();

    if (!current.isResolving() || current.getId().isUnknown()) {
      return current;
    }

    if (ThreadUtil.isMainThread()) {
      Log.w(TAG, "[Resolve][MAIN] " + getId(), new Throwable());
    }

    Recipient updated = fetchAndCacheRecipientFromDisk(getId());
    set(updated);
    return updated;
  }

  @WorkerThread
  public LiveRecipient refresh() {
    refresh(getId());
    return this;
  }

  /**
   * Forces a reload of the underlying recipient.
   */
  @WorkerThread
  public void refresh(@NonNull RecipientId id) {
    if (!getId().equals(id)) {
      Log.w(TAG, "Switching ID from " + getId() + " to " + id);
    }

    if (getId().isUnknown()) return;

    if (ThreadUtil.isMainThread()) {
      Log.w(TAG, "[Refresh][MAIN] " + id, new Throwable());
    }

    Recipient recipient = fetchAndCacheRecipientFromDisk(id);
    set(recipient);
    refreshForceNotify.postValue(new Object());
  }

  public @NonNull LiveData<Recipient> getLiveData() {
    return observableLiveData;
  }

  public @NonNull LiveData<Recipient> getLiveDataResolved() {
    return observableLiveDataResolved;
  }

  private @NonNull Recipient fetchAndCacheRecipientFromDisk(@NonNull RecipientId id) {
    RecipientRecord record;
    try {
      record = recipientTable.getRecord(id);
    } catch (RecipientTable.MissingRecipientException e) {
      Log.w(TAG, "Failed to find " + id + "! Returning UNKNOWN.");
      return Recipient.UNKNOWN;
    }

    Recipient recipient = RecipientCreator.forRecord(context, record);
    RecipientIdCache.INSTANCE.put(recipient);
    return recipient;
  }

  synchronized void set(@NonNull Recipient recipient) {
    this.recipient.set(recipient);
    this.liveData.postValue(recipient);
    this.subject.onNext(recipient);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LiveRecipient that = (LiveRecipient) o;
    return recipient.equals(that.recipient);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipient);
  }
}
