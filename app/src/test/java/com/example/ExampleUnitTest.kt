package com.example

import com.example.model.TvRemoteKey
import com.example.viewmodel.GamepadControlMode
import com.example.viewmodel.RemoteTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGamepadTabLocalization() {
    assertEquals("دسته بازی", RemoteTab.GAMEPAD.getTitle(isFarsi = true))
    assertEquals("Gamepad", RemoteTab.GAMEPAD.getTitle(isFarsi = false))
  }

  @Test
  fun testGamepadControlModes() {
    val dpadMode = GamepadControlMode.DPAD
    val joystickMode = GamepadControlMode.JOYSTICK
    assertNotNull(dpadMode)
    assertNotNull(joystickMode)
    assertEquals(2, GamepadControlMode.values().size)
  }

  @Test
  fun testGamepadKeyCodes() {
    assertEquals(96, TvRemoteKey.BUTTON_A.keyCode)
    assertEquals(97, TvRemoteKey.BUTTON_B.keyCode)
    assertEquals(99, TvRemoteKey.BUTTON_X.keyCode)
    assertEquals(100, TvRemoteKey.BUTTON_Y.keyCode)
    assertEquals(102, TvRemoteKey.BUTTON_L1.keyCode)
    assertEquals(103, TvRemoteKey.BUTTON_R1.keyCode)
    assertEquals(104, TvRemoteKey.BUTTON_L2.keyCode)
    assertEquals(105, TvRemoteKey.BUTTON_R2.keyCode)
    assertEquals(108, TvRemoteKey.BUTTON_START.keyCode)
    assertEquals(109, TvRemoteKey.BUTTON_SELECT.keyCode)
  }
}
