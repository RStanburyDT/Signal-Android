package org.thoughtcrime.securesms.ryan.stories.viewer

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.stories.viewer.page.StoryViewerPageArgs
import org.thoughtcrime.securesms.ryan.stories.viewer.page.StoryViewerPageFragment

class StoryViewerPagerAdapter(
  fragment: Fragment,
  private val arguments: StoryViewerPageArgs
) : FragmentStateAdapter(fragment) {

  private val pages: MutableList<RecipientId> = mutableListOf()

  fun setPages(newPages: List<RecipientId>) {
    val oldPages = ArrayList(pages)
    pages.clear()
    pages.addAll(newPages)

    val callback = Callback(oldPages, pages)
    DiffUtil.calculateDiff(callback).dispatchUpdatesTo(this)
  }

  fun getRecipientId(position: Int): RecipientId {
    return pages[position]
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
  }

  override fun getItemCount(): Int = pages.size

  override fun getItemId(position: Int): Long {
    return pages[position].toLong()
  }

  override fun containsItem(itemId: Long): Boolean {
    return pages.find { it.toLong() == itemId } != null
  }

  override fun createFragment(position: Int): Fragment {
    return StoryViewerPageFragment.create(arguments.copy(recipientId = pages[position]))
  }

  private class Callback(
    private val oldList: List<RecipientId>,
    private val newList: List<RecipientId>
  ) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      return oldList[oldItemPosition] == newList[newItemPosition]
    }
  }
}
