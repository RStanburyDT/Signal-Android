package org.thoughtcrime.securesms.ryan.conversation;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.signal.core.util.ThreadUtil;
import org.thoughtcrime.securesms.ryan.search.MessageResult;
import org.thoughtcrime.securesms.ryan.search.SearchRepository;
import org.thoughtcrime.securesms.ryan.util.Debouncer;

import java.util.Collections;
import java.util.List;

public class ConversationSearchViewModel extends ViewModel {

  private final SearchRepository              searchRepository;
  private final MutableLiveData<SearchResult> result;
  private final Debouncer                     debouncer;

  private boolean firstSearch;
  private boolean searchOpen;
  private String  activeQuery;
  private long    activeThreadId;

  public ConversationSearchViewModel(@NonNull String noteToSelfTitle) {
    result           = new MutableLiveData<>();
    debouncer        = new Debouncer(500);
    searchRepository = new SearchRepository(noteToSelfTitle);
  }

  public @NonNull LiveData<SearchResult> getSearchResults() {
    return result;
  }

  public void onQueryUpdated(@NonNull String query, long threadId, boolean forced) {
    if (firstSearch && queryTooShort(query)) {
      result.postValue(new SearchResult(Collections.emptyList(), 0));
      return;
    }

    if (query.equals(activeQuery) && !forced) {
      return;
    }

    updateQuery(query, threadId);
  }

  public void onMissingResult() {
    if (activeQuery != null) {
      updateQuery(activeQuery, activeThreadId);
    }
  }

  public void onMoveUp() {
    if (result.getValue() == null) {
      return;
    }

    debouncer.clear();

    List<MessageResult> messages = result.getValue().getResults();
    int                 position = Math.min(result.getValue().getPosition() + 1, messages.size() - 1);

    result.setValue(new SearchResult(messages, position));
  }

  public void onMoveDown() {
    if (result.getValue() == null) {
      return;
    }

    debouncer.clear();

    List<MessageResult> messages = result.getValue().getResults();
    int                 position = Math.max(result.getValue().getPosition() - 1, 0);

    result.setValue(new SearchResult(messages, position));
  }


  public void onSearchOpened() {
    searchOpen  = true;
    firstSearch = true;
  }

  public void onSearchClosed() {
    searchOpen = false;
    debouncer.clear();
  }

  private void updateQuery(@NonNull String query, long threadId) {
    activeQuery    = query;
    activeThreadId = threadId;

    debouncer.publish(() -> {
      firstSearch = false;

      searchRepository.query(query, threadId, messages -> {
        ThreadUtil.runOnMain(() -> {
          if (searchOpen && query.equals(activeQuery)) {
            result.setValue(new SearchResult(messages, 0));
          }
        });
      });
    });
  }

  private static boolean queryTooShort(String query) {
    if (query.isEmpty()) {
      return true;
    }

    if (query.length() >= 2) {
      return false;
    }

    char    onlyCharacter = query.charAt(0);
    boolean alphaNumeric  = Character.isAlphabetic(onlyCharacter) || Character.isDigit(onlyCharacter);

    return alphaNumeric;
  }

  public static class SearchResult {

    private final List<MessageResult> results;
    private final int                 position;

    SearchResult(@NonNull List<MessageResult> results, int position) {
      this.results  = results;
      this.position = position;
    }

    public List<MessageResult> getResults() {
      return results;
    }

    public int getPosition() {
      return position;
    }
  }

  public static class Factory extends ViewModelProvider.NewInstanceFactory {

    private final String noteToSelfTitle;

    public Factory(@NonNull String noteToSelfTitle) {
      this.noteToSelfTitle = noteToSelfTitle;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection ConstantConditions
      return modelClass.cast(new ConversationSearchViewModel(noteToSelfTitle));
    }
  }
}
