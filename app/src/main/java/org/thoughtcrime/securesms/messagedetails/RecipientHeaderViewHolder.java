package org.thoughtcrime.securesms.ryan.messagedetails;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.components.DeliveryStatusView;

final class RecipientHeaderViewHolder extends RecyclerView.ViewHolder {
  private final TextView           header;
  private final DeliveryStatusView deliveryStatus;

  RecipientHeaderViewHolder(View itemView) {
    super(itemView);

    header         = itemView.findViewById(R.id.recipient_header_text);
    deliveryStatus = itemView.findViewById(R.id.recipient_header_delivery_status);
  }

  void bind(RecipientHeader recipientHeader) {
    header.setText(recipientHeader.getHeaderText());
    switch (recipientHeader) {
      case PENDING:
        deliveryStatus.setPending();
        break;
      case SENT_TO:
        deliveryStatus.setSent();
        break;
      case DELIVERED:
        deliveryStatus.setDelivered();
        break;
      case READ:
        deliveryStatus.setRead();
        break;
      default:
        deliveryStatus.setNone();
        break;
    }
  }
}
