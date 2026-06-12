package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.TleViewModel
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
    assertEquals("TLE Finder", appName)
  }

  @Test
  fun `test favorite satellite saving and retrieval`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TleViewModel(application)

    // Initially, ISS (25544) should not be favorite
    assertFalse(viewModel.isFavorite("25544"))

    // Toggle favorite on ISS
    viewModel.toggleFavorite("25544", "ISS (ZARYA)")

    // Now it should be favorited
    assertTrue(viewModel.isFavorite("25544"))
    assertEquals(1, viewModel.favorites.value.size)
    assertEquals("25544" to "ISS (ZARYA)", viewModel.favorites.value[0])

    // Instantiate a new ViewModel to verify persistence via SharedPreferences is robust
    val persistentViewModel = TleViewModel(application)
    assertTrue(persistentViewModel.isFavorite("25544"))
    assertEquals(1, persistentViewModel.favorites.value.size)

    // Untoggle
    persistentViewModel.toggleFavorite("25544", "ISS (ZARYA)")
    assertFalse(persistentViewModel.isFavorite("25544"))
    assertEquals(0, persistentViewModel.favorites.value.size)
  }
}
