package org.thoughtcrime.securesms.ryan.wallpaper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.signal.core.util.concurrent.SignalExecutors;
import org.thoughtcrime.securesms.ryan.conversation.colors.ChatColors;
import org.thoughtcrime.securesms.ryan.conversation.colors.ChatColorsPalette;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;
import org.thoughtcrime.securesms.ryan.util.concurrent.SerialExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

class ChatWallpaperRepository {

  private static final Executor EXECUTOR = new SerialExecutor(SignalExecutors.BOUNDED);

  @MainThread
  @Nullable ChatWallpaper getCurrentWallpaper(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getWallpaper();
    } else {
      return SignalStore.wallpaper().getWallpaper();
    }
  }

  @MainThread
  @NonNull ChatColors getCurrentChatColors(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getChatColors();
    } else if (SignalStore.chatColors().hasChatColors()) {
      return Objects.requireNonNull(SignalStore.chatColors().getChatColors());
    } else if (SignalStore.wallpaper().hasWallpaperSet()) {
      return Objects.requireNonNull(SignalStore.wallpaper().getWallpaper()).getAutoChatColors();
    } else {
      return ChatColorsPalette.Bubbles.getDefault().withId(ChatColors.Id.Auto.INSTANCE);
    }
  }

  void getAllWallpaper(@NonNull Consumer<List<ChatWallpaper>> consumer) {
    EXECUTOR.execute(() -> {
      List<ChatWallpaper> wallpapers = new ArrayList<>(ChatWallpaper.BuiltIns.INSTANCE.getAllBuiltIns());

      wallpapers.addAll(WallpaperStorage.getAll());
      consumer.accept(wallpapers);
    });
  }

  void saveWallpaper(@Nullable RecipientId recipientId, @Nullable ChatWallpaper chatWallpaper, @NonNull Runnable onWallpaperSaved) {
    EXECUTOR.execute(() -> {
      if (recipientId != null) {
        //noinspection CodeBlock2Expr
        SignalDatabase.recipients().setWallpaper(recipientId, chatWallpaper, true);
        onWallpaperSaved.run();
      } else {
        SignalStore.wallpaper().setWallpaper(chatWallpaper);
        onWallpaperSaved.run();
      }
    });
  }

  void resetAllWallpaper(@NonNull Runnable onWallpaperReset) {
    EXECUTOR.execute(() -> {
      SignalStore.wallpaper().setWallpaper(null);
      SignalDatabase.recipients().resetAllWallpaper();
      onWallpaperReset.run();
    });
  }

  void resetAllChatColors(@NonNull Runnable onColorsReset) {
    SignalStore.chatColors().setChatColors(null);
    EXECUTOR.execute(() -> {
      SignalDatabase.recipients().clearAllColors();
      onColorsReset.run();
    });
  }

  void setDimInDarkTheme(@Nullable RecipientId recipientId, boolean dimInDarkTheme) {
    if (recipientId != null) {
      EXECUTOR.execute(() -> {
        Recipient recipient = Recipient.resolved(recipientId);
        if (recipient.getHasOwnWallpaper()) {
          SignalDatabase.recipients().setDimWallpaperInDarkTheme(recipientId, dimInDarkTheme);
        } else if (recipient.getHasWallpaper()) {
          SignalDatabase.recipients()
                       .setWallpaper(recipientId,
                                     ChatWallpaperFactory.updateWithDimming(recipient.getWallpaper(),
                                                                            dimInDarkTheme ? ChatWallpaper.FIXED_DIM_LEVEL_FOR_DARK_THEME
                                                                                           : 0f),
                                     false);
        } else {
          throw new IllegalStateException("Unexpected call to setDimInDarkTheme, no wallpaper has been set on the given recipient or globally.");
        }
      });
    } else {
      SignalStore.wallpaper().setDimInDarkTheme(dimInDarkTheme);
    }
  }

  public void clearChatColor(@Nullable RecipientId recipientId, @NonNull Runnable onChatColorCleared) {
    if (recipientId == null) {
      SignalStore.chatColors().setChatColors(null);
      onChatColorCleared.run();
    } else {
      EXECUTOR.execute(() -> {
        SignalDatabase.recipients().clearColor(recipientId);
        onChatColorCleared.run();
      });
    }
  }
}
