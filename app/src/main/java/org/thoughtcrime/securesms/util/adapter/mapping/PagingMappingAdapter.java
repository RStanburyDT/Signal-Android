package org.thoughtcrime.securesms.ryan.util.adapter.mapping;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.paging.PagingController;
import org.thoughtcrime.securesms.ryan.util.ViewUtil;

/**
 * A specialized {@link MappingAdapter} backed by a {@link PagingController}.
 */
public class PagingMappingAdapter<Key> extends MappingAdapter {

  private PagingController<Key> pagingController;

  public PagingMappingAdapter() {
    this(1, ViewUtil.dpToPx(100));
  }

  public PagingMappingAdapter(int placeHolderWidth, int placeHolderHeight) {
    registerFactory(Placeholder.class, parent -> {
      View view = new FrameLayout(parent.getContext());
      view.setLayoutParams(new FrameLayout.LayoutParams(placeHolderWidth, placeHolderHeight));
      return new MappingViewHolder.SimpleViewHolder<>(view);
    });
  }

  public void setPagingController(@Nullable PagingController<Key> pagingController) {
    this.pagingController = pagingController;
  }

  @Override
  public @Nullable MappingModel<?> getItem(int position) {
    if (pagingController != null) {
      pagingController.onDataNeededAroundIndex(position);
    }

    if (position >= 0 && position < super.getCurrentList().size()) {
      return super.getItem(position);
    } else {
      return null;
    }
  }

  public boolean isAvailableAround(int position) {
    int start = position - 10;
    int end   = position + 10;

    if (isRangeAvailable(start, end)) {
      return true;
    } else {
      getItem(position);
      return false;
    }
  }

  protected final boolean isRangeAvailable(int start, int end) {
    if (end <= start || start >= getItemCount() || end <= 0) {
      return false;
    }

    int clampedStart = Math.max(0, start);
    int clampedEnd   = Math.min(getItemCount(), end);

    for (int i = clampedStart; i < clampedEnd; i++) {
      if (super.getItem(i) == null) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int getItemViewType(int position) {
    MappingModel<?> item = getItem(position);
    if (item == null) {
      //noinspection ConstantConditions
      return itemTypes.get(Placeholder.class);
    }

    Integer type = itemTypes.get(item.getClass());
    if (type != null) {
      return type;
    }
    throw new AssertionError("No view holder factory for type: " + item.getClass());
  }

  public boolean hasItem(int position) {
    return getItem(position) != null;
  }

  protected static class Placeholder implements MappingModel<Placeholder> {
    @Override
    public boolean areItemsTheSame(@NonNull Placeholder newItem) {
      return false;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Placeholder newItem) {
      return false;
    }
  }
}
