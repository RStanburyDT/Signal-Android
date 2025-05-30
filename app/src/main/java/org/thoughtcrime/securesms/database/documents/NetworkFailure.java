package org.thoughtcrime.securesms.ryan.database.documents;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;

import java.util.Objects;

public class NetworkFailure {

  /** DEPRECATED */
  @JsonProperty(value = "a")
  private String address;

  @JsonProperty(value = "r")
  private String recipientId;

  public NetworkFailure(@NonNull RecipientId recipientId) {
    this.recipientId = recipientId.serialize();
    this.address = "";
  }

  public NetworkFailure() {}

  @JsonIgnore
  public RecipientId getRecipientId() {
    if (!TextUtils.isEmpty(recipientId)) {
      return RecipientId.from(recipientId);
    } else {
      Recipient recipient = Recipient.external(address);
      if (recipient != null) {
        return recipient.getId();
      } else {
        return RecipientId.UNKNOWN;
      }
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NetworkFailure that = (NetworkFailure) o;
    return Objects.equals(address, that.address) &&
        Objects.equals(recipientId, that.recipientId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, recipientId);
  }
}
