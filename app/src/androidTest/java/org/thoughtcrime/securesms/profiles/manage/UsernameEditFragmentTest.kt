package org.thoughtcrime.securesms.ryan.profiles.manage

import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.reactivex.rxjava3.schedulers.TestScheduler
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.dependencies.InstrumentationApplicationDependencyProvider
import org.thoughtcrime.securesms.ryan.testing.Put
import org.thoughtcrime.securesms.ryan.testing.RxTestSchedulerRule
import org.thoughtcrime.securesms.ryan.testing.SignalActivityRule
import org.thoughtcrime.securesms.ryan.testing.success
import org.whispersystems.signalservice.api.util.Usernames
import org.whispersystems.signalservice.internal.push.ReserveUsernameResponse
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class UsernameEditFragmentTest {

  @get:Rule
  val harness = SignalActivityRule(othersCount = 10)

  private val ioScheduler = TestScheduler()
  private val computationScheduler = TestScheduler()

  @get:Rule
  val testSchedulerRule = RxTestSchedulerRule(
    ioTestScheduler = ioScheduler,
    computationTestScheduler = computationScheduler
  )

  @After
  fun tearDown() {
    InstrumentationApplicationDependencyProvider.clearHandlers()
  }

  @Ignore("Flakey espresso test.")
  @Test
  fun testUsernameCreationOutsideOfRegistration() {
    val scenario = createScenario(UsernameEditMode.NORMAL)

    scenario.moveToState(Lifecycle.State.RESUMED)

    onView(withId(R.id.toolbar)).check { view, noViewFoundException ->
      assertThat(noViewFoundException).isNull()
      val toolbar = view as Toolbar
      assertThat(toolbar.navigationIcon).isNotNull()
    }

    onView(withText(R.string.UsernameEditFragment_username)).check(matches(isDisplayed()))
    onView(withContentDescription(R.string.load_more_header__loading)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
  }

  @Ignore("Flakey espresso test.")
  @Test
  fun testNicknameUpdateHappyPath() {
    val nickname = "Spiderman"
    val discriminator = "4578"
    val username = "$nickname${Usernames.DELIMITER}$discriminator"

    InstrumentationApplicationDependencyProvider.addMockWebRequestHandlers(
      Put("/v1/accounts/username/reserved") {
        MockResponse().success(ReserveUsernameResponse(username))
      },
      Put("/v1/accounts/username/confirm") {
        MockResponse().success()
      }
    )

    val scenario = createScenario(UsernameEditMode.NORMAL)
    scenario.moveToState(Lifecycle.State.RESUMED)

    onView(withId(R.id.username_text)).perform(typeText(nickname))

    computationScheduler.advanceTimeBy(501, TimeUnit.MILLISECONDS)
    computationScheduler.triggerActions()

    onView(withContentDescription(R.string.load_more_header__loading)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    ioScheduler.triggerActions()
    computationScheduler.triggerActions()

    onView(withId(R.id.username_text)).perform(closeSoftKeyboard())
    onView(withId(R.id.username_done_button)).check(matches(isDisplayed()))
    onView(withId(R.id.username_done_button)).check(matches(isEnabled()))
    onView(withText(username)).check(matches(isDisplayed()))

    onView(withId(R.id.username_done_button)).perform(click())

    computationScheduler.triggerActions()
    onView(withId(R.id.username_done_button)).check(matches(isNotEnabled()))
  }

  private fun createScenario(mode: UsernameEditMode = UsernameEditMode.NORMAL): FragmentScenario<UsernameEditFragment> {
    val fragmentArgs = UsernameEditFragmentArgs.Builder().setMode(mode).build().toBundle()
    return launchFragmentInContainer(
      fragmentArgs = fragmentArgs,
      themeResId = R.style.Signal_DayNight_NoActionBar
    )
  }
}
