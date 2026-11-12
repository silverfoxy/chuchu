package com.jossephus.chuchu.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import com.jossephus.chuchu.ui.theme.ChuTheme
import com.jossephus.chuchu.ui.theme.GhosttyThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChuButtonTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `enter activates only the focused button once per press`() {
        GhosttyThemeRegistry.init(ApplicationProvider.getApplicationContext<Application>())
        val focus = FocusRequester()
        var connections = 0
        var cancellations = 0
        composeRule.setContent {
            ChuTheme(themeName = "Catppuccin Mocha") {
                Column {
                    ChuButton(onClick = { cancellations++ }) { ChuText("Cancel") }
                    ChuButton(onClick = { connections++ }, modifier = Modifier.focusRequester(focus)) {
                        ChuText("Connect")
                    }
                }
            }
        }
        composeRule.runOnIdle { focus.requestFocus() }
        for (key in listOf(Key.Enter, Key.NumPadEnter)) {
            composeRule.onNodeWithText("Connect").performKeyInput { keyDown(key) }
            composeRule.runOnIdle { assertEquals(if (key == Key.Enter) 0 else 1, connections) }
            composeRule.onNodeWithText("Connect").performKeyInput { keyUp(key) }
        }
        composeRule.runOnIdle {
            assertEquals(2, connections)
            assertEquals(0, cancellations)
        }
    }
    @Test
    fun `passphrase field submits with hardware enter and IME Done`() {
        GhosttyThemeRegistry.init(ApplicationProvider.getApplicationContext<Application>())
        var submissions = 0
        composeRule.setContent {
            ChuTheme(themeName = "Catppuccin Mocha") {
                ChuTextField(
                    value = "secret", onValueChange = {}, label = "Passphrase",
                    singleLine = true, onSubmit = { submissions++ },
                )
            }
        }
        composeRule.onNodeWithText("secret").performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        composeRule.runOnIdle { assertEquals(1, submissions) }
        composeRule.onNodeWithText("secret").performImeAction()
        composeRule.runOnIdle { assertEquals(2, submissions) }
    }

}
