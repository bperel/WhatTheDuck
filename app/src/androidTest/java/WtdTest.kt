
import android.app.Activity
import android.view.View
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Checks
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.github.clans.fab.FloatingActionButton
import net.ducksmanager.persistence.AppDatabase
import net.ducksmanager.whattheduck.*
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import java.util.*

open class WtdTest : AndroidJUnitRunner {
    abstract class LocaleWithDefaultPublication(country: String?, language: String?) {
        val locale: Locale = Locale(language, country)
        val defaultCountry: String
            get() = locale.country

        abstract val defaultPublication: String

    }

    companion object {
        fun parameterData(): Iterable<Array<Any>> {
            return listOf<Array<Any>>(
                arrayOf(object : LocaleWithDefaultPublication("se", "sv") {
                    override val defaultPublication: String
                        get() = "se/KAP"
                }),
                arrayOf(object : LocaleWithDefaultPublication("us", "en") {
                    override val defaultPublication: String
                        get() = "us/WDC"
                }),
                arrayOf(object : LocaleWithDefaultPublication("fr", "fr") {
                    override val defaultPublication: String
                        get() = "fr/DDD"
            }))
        }

        var currentLocale: LocaleWithDefaultPublication? = null
        var mockServer: MockWebServer? = null

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            initMockServer()
        }

        private fun initMockServer() {
            mockServer = MockWebServer()
            mockServer!!.dispatcher = DownloadHandlerMock.dispatcher

            WhatTheDuck.config = Properties()
            WhatTheDuck.config.setProperty(WhatTheDuck.CONFIG_KEY_ROLE_NAME, "test")
            WhatTheDuck.config.setProperty(WhatTheDuck.CONFIG_KEY_ROLE_PASSWORD, "test")

            WhatTheDuck.config.setProperty(
                WhatTheDuck.CONFIG_KEY_DM_URL,
                mockServer!!.url("/dm/").toString()
            )
            WhatTheDuck.config.setProperty(
                WhatTheDuck.CONFIG_KEY_API_ENDPOINT_URL,
                mockServer!!.url("/dm-server/").toString()
            )
            WhatTheDuck.config.setProperty(
                WhatTheDuck.CONFIG_KEY_EDGES_URL,
                mockServer!!.url("/edges/").toString()
            )
        }

        fun login(user: String?, password: String?) {
            onView(ViewMatchers.withId(R.id.username))
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeText(user), closeSoftKeyboard())

            onView(ViewMatchers.withId(R.id.password))
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeText(password), closeSoftKeyboard())

            onView(ViewMatchers.withId(R.id.login)).perform(ViewActions.click())
        }

        val activityInstance: Activity?
            get() {
                val currentActivity = arrayOfNulls<Activity>(1)
                getInstrumentation().runOnMainSync {
                    val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    currentActivity[0] = resumedActivities.iterator().next()
                }
                return currentActivity[0]
            }

        fun forceFloatingActionButtonsVisible(value: Boolean): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isAssignableFrom(FloatingActionButton::class.java)
                }

                override fun perform(uiController: UiController, view: View) {
                    view.visibility = if (value) View.VISIBLE else View.GONE
                }

                override fun getDescription(): String {
                    return "Force floating action buttons to be visible"
                }
            }
        }

        fun forceFloatingActionButtonsVisible(): Matcher<Any> {
            return object : BoundedMatcher<Any, FloatingActionButton>(FloatingActionButton::class.java) {
                public override fun matchesSafely(item: FloatingActionButton): Boolean {
                    item.visibility = View.VISIBLE
                    return true
                }

                override fun describeTo(description: Description) {
                    description.appendText("Force floating action buttons to be visible")
                }
            }
        }
    }

    init {
        currentLocale = object : LocaleWithDefaultPublication("us", "en") {
            override val defaultPublication: String
                get() = "us/WDC"
        }
        WhatTheDuck.DB_NAME = "appDB_test"
        WhatTheDuck.isTestContext = true
    }

    val screenshotPath: String
        get() = ScreenshotTestRule.SCREENSHOTS_PATH_SHOWCASE + "/" + currentLocale!!.locale.language

    @get:Rule
    var screenshotTestRule = ScreenshotTestRule()

    @get:Rule
    val loginActivityRule = ActivityTestRule(
        Login::class.java,
        true,
        false
    )

    @Before
    fun resetDownloadMockState() {
        DownloadHandlerMock.state.remove("server_offline")
    }

    @Before
    fun resetDb() {
        WhatTheDuck.appDB = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @Before
    fun resetConfigs() {
        CountryList.hasFullList = false
        IssueList.viewType = IssueList.ViewType.LIST_VIEW
    }

    fun switchLocale() {
        if (currentLocale != null) {
            val resources = WhatTheDuck.applicationContext!!.resources
            val configuration = resources.configuration
            configuration.setLocale(currentLocale!!.locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }

    constructor()

    constructor(currentLocale: LocaleWithDefaultPublication?) {
        Companion.currentLocale = currentLocale
    }

    fun assertToastShown(textId: Int) {
        onView(withText(textId))
            .inRoot(RootMatchers.withDecorView(CoreMatchers.not(CoreMatchers.`is`(loginActivityRule.activity.window.decorView))))
            .check(matches(ViewMatchers.isDisplayed()))
    }

    fun assertCurrentActivityIsInstanceOf(activityClass: Class<*>, assertTrue: Boolean) {
        Checks.checkNotNull(activityInstance)
        Checks.checkNotNull(activityClass)
        val isInstance = activityInstance!!::class.java.name == activityClass.name
        if (assertTrue) {
            Assert.assertTrue(isInstance)
        } else {
            Assert.assertFalse(isInstance)
        }
    }

    protected fun clickOnActionButton(buttonId: Int) {
        val viewMatcher = onView(Matchers.allOf(ViewMatchers.withId(buttonId), forceFloatingActionButtonsVisible()))

        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        viewMatcher.perform(ViewActions.click())
    }
}