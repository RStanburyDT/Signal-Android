package org.thoughtcrime.securesms.ryan.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.RequestManager;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.mms.Slide;
import org.thoughtcrime.securesms.ryan.mms.SlideClickListener;
import org.thoughtcrime.securesms.ryan.mms.SlidesClickedListener;

public class BorderlessImageView extends FrameLayout {

  private ThumbnailView image;
  private View          missingShade;

  public BorderlessImageView(@NonNull Context context) {
    super(context);
    init();
  }

  public BorderlessImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.sticker_view, this);

    this.image        = findViewById(R.id.sticker_thumbnail);
    this.missingShade = findViewById(R.id.sticker_missing_shade);
  }

  @Override
  public void setFocusable(boolean focusable) {
    image.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    image.setClickable(clickable);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener l) {
    image.setOnLongClickListener(l);
  }

  public void setSlide(@NonNull RequestManager requestManager, @NonNull Slide slide, @ColorInt int missingBackgroundColor) {
    boolean showControls = slide.asAttachment().getUri() == null;

    if (slide.hasSticker()) {
      image.setScaleType(ImageView.ScaleType.FIT_CENTER);
      image.setImageResource(requestManager, slide, showControls, false, 0, 0, missingBackgroundColor);
    } else {
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setImageResource(requestManager, slide, showControls, false, slide.asAttachment().width, slide.asAttachment().height, missingBackgroundColor);
    }

    missingShade.setVisibility(showControls ? View.VISIBLE : View.GONE);
  }

  public void setThumbnailClickListener(@NonNull SlideClickListener listener) {
    image.setThumbnailClickListener(listener);
  }

  public void setDownloadClickListener(@NonNull SlidesClickedListener listener) {
    image.setStartTransferClickListener(listener);
  }
}
