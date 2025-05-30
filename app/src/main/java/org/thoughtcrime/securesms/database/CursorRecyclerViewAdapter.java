/**
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.ryan.database;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.thoughtcrime.securesms.ryan.R;

import java.util.List;

/**
 * RecyclerView.Adapter that manages a Cursor, comparable to the CursorAdapter usable in ListView/GridView.
 */
public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private final @NonNull Context context;
  private final DataSetObserver observer = new AdapterDataSetObserver();

  @VisibleForTesting static final int  HEADER_TYPE = Integer.MIN_VALUE;
  @VisibleForTesting static final int  FOOTER_TYPE = Integer.MIN_VALUE + 1;
  @VisibleForTesting static final long HEADER_ID   = Long.MIN_VALUE;
  @VisibleForTesting static final long FOOTER_ID   = Long.MIN_VALUE + 1;

  private @Nullable Cursor  cursor;
  private           boolean valid;
  private @Nullable View    header;
  private @Nullable View    footer;

  private static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {

    private ViewGroup container;

    HeaderFooterViewHolder(@NonNull View itemView) {
      super(itemView);
      this.container = (ViewGroup) itemView;
    }

    void bind(@Nullable View view) {
      unbind();

      if (view != null) {
        container.addView(view);
      }
    }

    void unbind() {
      container.removeAllViews();
    }
  }

  protected CursorRecyclerViewAdapter(@NonNull Context context, @Nullable Cursor cursor) {
    this.context = context;
    this.cursor = cursor;
    if (cursor != null) {
      valid = true;
      cursor.registerDataSetObserver(observer);
    }
  }

  protected @NonNull Context getContext() {
    return context;
  }

  public @Nullable Cursor getCursor() {
    return cursor;
  }

  public void setHeaderView(@Nullable View header) {
    this.header = header;
  }

  public View getHeaderView() {
    return this.header;
  }

  public void setFooterView(@Nullable View footer) {
    this.footer = footer;
  }

  public boolean hasHeaderView() {
    return header != null;
  }

  public boolean hasFooterView() {
    return footer != null;
  }

  public void changeCursor(Cursor cursor) {
    Cursor old = swapCursor(cursor);
    if (old != null) {
      old.close();
    }
  }

  public Cursor swapCursor(Cursor newCursor) {
    if (newCursor == cursor) {
      return null;
    }

    final Cursor oldCursor = cursor;
    if (oldCursor != null) {
      oldCursor.unregisterDataSetObserver(observer);
    }

    cursor = newCursor;
    if (cursor != null) {
      cursor.registerDataSetObserver(observer);
    }

    valid = cursor != null;
    notifyDataSetChanged();
    return oldCursor;
  }

  @Override
  public int getItemCount() {
    if (!isActiveCursor()) return 0;

    return cursor.getCount()
           + getFastAccessSize()
           + (hasHeaderView() ? 1 : 0)
           + (hasFooterView() ? 1 : 0);
  }

  public int getCursorCount() {
    return cursor.getCount();
  }

  @SuppressWarnings("unchecked")
  @Override
  public final void onViewRecycled(@NonNull ViewHolder holder) {
    if (!(holder instanceof HeaderFooterViewHolder)) {
      onItemViewRecycled((VH)holder);
    } else {
      ((HeaderFooterViewHolder) holder).unbind();
    }
  }

  public void onItemViewRecycled(VH holder) {}

  @Override
  public @NonNull final ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    switch (viewType) {
    case HEADER_TYPE:
    case FOOTER_TYPE:
      return new HeaderFooterViewHolder(LayoutInflater.from(context).inflate(R.layout.cursor_adapter_header_footer_view, parent, false));
    default:
      return onCreateItemViewHolder(parent, viewType);
    }
  }

  public abstract VH onCreateItemViewHolder(ViewGroup parent, int viewType);

  @Override
  public final void onBindViewHolder(@NonNull ViewHolder viewHolder, int position, @NonNull List<Object> payloads) {
    if (arePayloadsValid(payloads) && !isHeaderPosition(position) && !isFooterPosition(position)) {
      if (isFastAccessPosition(position)) onBindFastAccessItemViewHolder((VH)viewHolder, position, payloads);
      else                                onBindItemViewHolder((VH)viewHolder, getCursorAtPositionOrThrow(position), payloads);
    } else {
      onBindViewHolder(viewHolder, position);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
    if (isHeaderPosition(position)) {
      ((HeaderFooterViewHolder) viewHolder).bind(header);
    } else if (isFooterPosition(position)) {
      ((HeaderFooterViewHolder) viewHolder).bind(footer);
    } else {
      if (isFastAccessPosition(position)) onBindFastAccessItemViewHolder((VH)viewHolder, position);
      else                                onBindItemViewHolder((VH)viewHolder, getCursorAtPositionOrThrow(position));
    }
  }

  public abstract void onBindItemViewHolder(VH viewHolder, @NonNull Cursor cursor);

  protected boolean arePayloadsValid(@NonNull List<Object> payloads) {
    return false;
  }

  protected void onBindItemViewHolder(VH viewHolder, @NonNull Cursor cursor, @NonNull List<Object> payloads) {
  }

  protected void onBindFastAccessItemViewHolder(VH viewHolder, int position) {
  }

  protected void onBindFastAccessItemViewHolder(VH viewHolder, int position, @NonNull List<Object> payloads) {
  }

  @Override
  public final int getItemViewType(int position) {
    if (isHeaderPosition(position))     return HEADER_TYPE;
    if (isFooterPosition(position))     return FOOTER_TYPE;
    if (isFastAccessPosition(position)) return getFastAccessItemViewType(position);
    return getItemViewType(getCursorAtPositionOrThrow(position));
  }

  public int getItemViewType(@NonNull Cursor cursor) {
    return 0;
  }

  @Override
  public final long getItemId(int position) {
    if (isHeaderPosition(position))          return HEADER_ID;
    else if (isFooterPosition(position))     return FOOTER_ID;
    else if (isFastAccessPosition(position)) return getFastAccessItemId(position);

    long itemId = getItemId(getCursorAtPositionOrThrow(position));
    return itemId <= Long.MIN_VALUE + 1 ? itemId + 2 : itemId;
  }

  public long getItemId(@NonNull Cursor cursor) {
    return cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
  }

  protected @NonNull Cursor getCursorAtPositionOrThrow(final int position) {
    if (!isActiveCursor()) {
      throw new IllegalStateException("this should only be called when the cursor is valid");
    }
    if (!cursor.moveToPosition(getCursorPosition(position))) {
      throw new IllegalStateException("couldn't move cursor to position " + position + " (actual cursor position " + getCursorPosition(position) + ")");
    }
    return cursor;
  }

  protected boolean isActiveCursor() {
    return valid && cursor != null;
  }

  protected boolean isFooterPosition(int position) {
    return hasFooterView() && position == getItemCount() - 1;
  }

  protected boolean isHeaderPosition(int position) {
    return hasHeaderView() && position == 0;
  }

  private int getCursorPosition(int position) {
    if (hasHeaderView()) {
      position -= 1;
    }

    return position - getFastAccessSize();
  }

  protected int getFastAccessItemViewType(int position) {
    return 0;
  }

  protected boolean isFastAccessPosition(int position) {
    return false;
  }

  protected long getFastAccessItemId(int position) {
    return 0;
  }

  protected int getFastAccessSize() {
    return 0;
  }

  private class AdapterDataSetObserver extends DataSetObserver {
    @Override
    public void onChanged() {
      super.onChanged();
      valid = true;
    }

    @Override
    public void onInvalidated() {
      super.onInvalidated();
      valid = false;
    }
  }
}
