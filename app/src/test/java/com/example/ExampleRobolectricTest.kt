package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ImprimeDoc", appName)
  }

  @Test
  fun `verify localization strings exist`() {
    val baseContext = ApplicationProvider.getApplicationContext<Context>()

    // Test French (default)
    val frConfig = Configuration(baseContext.resources.configuration).apply { setLocale(Locale.FRENCH) }
    val frContext = baseContext.createConfigurationContext(frConfig)
    assertEquals("Imprimer des Photos", frContext.getString(R.string.photos_title))

    // Test English
    val enConfig = Configuration(baseContext.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    val enContext = baseContext.createConfigurationContext(enConfig)
    assertEquals("Print Photos", enContext.getString(R.string.photos_title))

    // Test Arabic
    val arConfig = Configuration(baseContext.resources.configuration).apply { setLocale(Locale("ar")) }
    val arContext = baseContext.createConfigurationContext(arConfig)
    assertEquals("طباعة الصور", arContext.getString(R.string.photos_title))
  }
}
