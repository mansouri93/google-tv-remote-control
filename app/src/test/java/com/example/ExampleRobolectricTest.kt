package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TvPreferences
import com.example.util.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Google TV Remote", appName)
  }

  @Test
  fun `test preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs1 = TvPreferences(context)
    prefs1.language = AppLanguage.EN
    prefs1.isLiteMode = false
    prefs1.lastConnectedIp = "192.168.1.55"
    prefs1.lastConnectedName = "Sony Android TV"

    // Create a fresh instance reading from SharedPreferences
    val prefs2 = TvPreferences(context)
    assertEquals(AppLanguage.EN, prefs2.language)
    assertFalse(prefs2.isLiteMode)
    assertEquals("192.168.1.55", prefs2.lastConnectedIp)
    assertEquals("Sony Android TV", prefs2.lastConnectedName)
  }
}
