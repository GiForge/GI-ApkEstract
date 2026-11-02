package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GI-ApkExtractor", appName)
  }

  @Test
  fun `format file size works correctly`() {
    assertEquals("0 B", AppInfo.formatFileSize(0))
    assertEquals("500 B", AppInfo.formatFileSize(500))
    assertTrue(AppInfo.formatFileSize(1024 * 1024 * 15).contains("15"))
  }

  @Test
  fun `app info safe file name cleans special characters`() {
    val app = AppInfo(
        packageName = "com.test.app",
        appName = "Test App: Pro!",
        versionName = "1.0.2 beta",
        versionCode = 10,
        sourceDir = "/path/to/base.apk",
        apkSizeBytes = 1024,
        isSystemApp = false,
        firstInstallTime = 0,
        lastUpdateTime = 0,
        minSdkVersion = 24,
        targetSdkVersion = 34
    )
    assertEquals("Test_App__Pro__v1.0.2_beta.apk", app.safeFileName)
  }
}
