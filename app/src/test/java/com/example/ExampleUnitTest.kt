package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGoogleFormsUrlValidation() {
    assertTrue(com.example.formpilot.service.GoogleFormsBridge.isValidGoogleFormUrl("https://docs.google.com/forms/d/e/1FAIpQLSc/viewform"))
    assertTrue(com.example.formpilot.service.GoogleFormsBridge.isValidGoogleFormUrl("https://forms.gle/xyz123"))
    assertFalse(com.example.formpilot.service.GoogleFormsBridge.isValidGoogleFormUrl("https://example.com/not-a-form"))
  }

  @Test
  fun testFormPageStateCreation() {
    val state = com.example.formpilot.model.FormPageState(
      currentPage = 2,
      totalPages = 3,
      pageIndicatorText = "Page 2 of 3",
      hasNextPage = true,
      hasPrevPage = true,
      isSubmitPage = false
    )
    assertEquals(2, state.currentPage)
    assertEquals(3, state.totalPages)
    assertTrue(state.hasNextPage)
    assertTrue(state.hasPrevPage)
  }
}
