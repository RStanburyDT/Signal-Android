package org.thoughtcrime.securesms.ryan.contacts.paged

import android.app.Application
import androidx.core.os.bundleOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thoughtcrime.securesms.ryan.MockCursor
import org.thoughtcrime.securesms.ryan.database.RecipientTable
import org.thoughtcrime.securesms.ryan.database.model.DistributionListPrivacyMode
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies
import org.thoughtcrime.securesms.ryan.recipients.LiveRecipientCache
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ContactSearchPagedDataSourceTest {
  private val repository = mockk<ContactSearchPagedDataSourceRepository>(relaxed = true)
  private val cursor = mockk<MockCursor>(relaxed = true)
  private val groupStoryData = ContactSearchData.Story(Recipient.UNKNOWN, 0, DistributionListPrivacyMode.ALL)

  @Before
  fun setUp() {
    mockkStatic(AppDependencies::class)
    every { AppDependencies.recipientCache } returns mockk<LiveRecipientCache>(relaxed = true)

    every { repository.getRecipientFromGroupRecord(any()) } returns Recipient.UNKNOWN
    every { repository.getRecipientFromSearchCursor(any()) } returns Recipient.UNKNOWN
    every { repository.getRecipientFromThreadCursor(cursor) } returns Recipient.UNKNOWN
    every { repository.getRecipientFromDistributionListCursor(cursor) } returns Recipient.UNKNOWN
    every { repository.getPrivacyModeFromDistributionListCursor(cursor) } returns DistributionListPrivacyMode.ALL
    every { repository.getGroupStories() } returns emptySet()
    every { repository.getLatestStorySends(any()) } returns emptyList()
    every { cursor.getString(any()) } returns "A"
    every { cursor.moveToPosition(any()) } answers { callOriginal() }
    every { cursor.moveToNext() } answers { callOriginal() }
    every { cursor.position } answers { callOriginal() }
    every { cursor.isLast } answers { callOriginal() }
    every { cursor.isAfterLast } answers { callOriginal() }
  }

  @Test
  fun `Given recentsWHeader and individualsWHeaderWExpand, when I size, then I expect 15`() {
    val testSubject = createTestSubject()
    Assert.assertEquals(15, testSubject.size())
  }

  @Test
  fun `Given recentsWHeader and individualsWHeaderWExpand, when I load 12, then I expect properly structured output`() {
    val testSubject = createTestSubject()
    val result = testSubject.load(0, 12, 20) { false }

    val expected = listOf(
      ContactSearchKey.Header(ContactSearchConfiguration.SectionKey.RECENTS),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.Header(ContactSearchConfiguration.SectionKey.INDIVIDUALS)
    )

    val resultKeys = result.map { it.contactSearchKey }

    Assert.assertEquals(expected, resultKeys)
  }

  @Test
  fun `Given recentsWHeader and individualsWHeaderWExpand, when I load 10 with offset 5, then I expect properly structured output`() {
    val testSubject = createTestSubject()
    val result = testSubject.load(5, 10, 15) { false }

    val expected = listOf(
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.Header(ContactSearchConfiguration.SectionKey.INDIVIDUALS),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, false),
      ContactSearchKey.Expand(ContactSearchConfiguration.SectionKey.INDIVIDUALS)
    )

    val resultKeys = result.map { it.contactSearchKey }

    Assert.assertEquals(expected, resultKeys)
  }

  @Test
  fun `Given storiesWithHeaderAndExtras, when I load 11, then I expect properly structured output`() {
    val testSubject = createStoriesSubject()
    val result = testSubject.load(0, 12, 12) { false }

    val expected = listOf(
      ContactSearchKey.Header(ContactSearchConfiguration.SectionKey.STORIES),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true),
      ContactSearchKey.RecipientSearchKey(RecipientId.UNKNOWN, true)
    )

    val resultKeys = result.map { it.contactSearchKey }

    Assert.assertEquals(expected, resultKeys)
  }

  @Test
  fun `Given only arbitrary elements, when I size, then I expect 3`() {
    val testSubject = createArbitrarySubject()
    val expected = 3
    val actual = testSubject.size()

    Assert.assertEquals(expected, actual)
  }

  @Test
  fun `Given only arbitrary elements, when I load 1, then I expect 1`() {
    val testSubject = createArbitrarySubject()
    val expected = ContactSearchData.Arbitrary("two", bundleOf("n" to "two"))
    val actual = testSubject.load(1, 1, 1) { false }[0] as ContactSearchData.Arbitrary

    Assert.assertEquals(expected.data?.getString("n"), actual.data?.getString("n"))
  }

  private fun createArbitrarySubject(): ContactSearchPagedDataSource {
    val configuration = ContactSearchConfiguration.build {
      arbitrary(
        "one",
        "two",
        "three"
      )

      withEmptyState {
        arbitrary(
          "one",
          "two",
          "three"
        )
      }
    }

    return ContactSearchPagedDataSource(configuration, repository, ArbitraryRepoFake())
  }

  private fun createStoriesSubject(): ContactSearchPagedDataSource {
    val configuration = ContactSearchConfiguration.build {
      addSection(
        ContactSearchConfiguration.Section.Stories(
          groupStories = setOf(
            groupStoryData
          ),
          includeHeader = true,
          expandConfig = ContactSearchConfiguration.ExpandConfig(isExpanded = true)
        )
      )
    }

    every { repository.getStories(any()) } returns cursor
    every { repository.recipientNameContainsQuery(Recipient.UNKNOWN, null) } returns true
    every { cursor.count } returns 10

    return ContactSearchPagedDataSource(configuration, repository)
  }

  private fun createTestSubject(): ContactSearchPagedDataSource {
    val recents = ContactSearchConfiguration.Section.Recents(
      includeHeader = true
    )

    val configuration = ContactSearchConfiguration.build {
      addSection(recents)

      addSection(
        ContactSearchConfiguration.Section.Individuals(
          includeHeader = true,
          includeSelfMode = RecipientTable.IncludeSelfMode.Exclude,
          transportType = ContactSearchConfiguration.TransportType.ALL,
          expandConfig = ContactSearchConfiguration.ExpandConfig(isExpanded = false)
        )
      )
    }

    every { repository.getRecents(recents) } returns cursor
    every { repository.querySignalContacts(any()) } returns cursor
    every { cursor.count } returns 10

    return ContactSearchPagedDataSource(configuration, repository)
  }

  private class ArbitraryModel : MappingModel<ArbitraryModel> {
    override fun areItemsTheSame(newItem: ArbitraryModel): Boolean = true

    override fun areContentsTheSame(newItem: ArbitraryModel): Boolean = true
  }

  private class ArbitraryRepoFake : ArbitraryRepository {
    override fun getSize(section: ContactSearchConfiguration.Section.Arbitrary, query: String?): Int = section.types.size

    override fun getData(section: ContactSearchConfiguration.Section.Arbitrary, query: String?, startIndex: Int, endIndex: Int, totalSearchSize: Int): List<ContactSearchData.Arbitrary> {
      return section.types.toList().slice(startIndex..endIndex).map {
        ContactSearchData.Arbitrary(it, bundleOf("n" to it))
      }
    }

    override fun getMappingModel(arbitrary: ContactSearchData.Arbitrary): MappingModel<*> {
      return ArbitraryModel()
    }
  }
}
