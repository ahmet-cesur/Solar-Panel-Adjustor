package com.acesur.solarpvtracker

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launchesAndShowsWelcomeMessage() {
        // The HomeScreen shows "Welcome to Solar Panel Adjustor" (welcome_message)
        // Note: Strings might be localized, so we search for the default one
        val welcomeMessage = "Welcome to Solar Panel Adjustor"
        
        composeTestRule.onNodeWithText(welcomeMessage).assertIsDisplayed()
    }

    @Test
    fun app_showsTiltmeterOption() {
        composeTestRule.onNodeWithText("Tiltmeter").assertIsDisplayed()
    }
}
