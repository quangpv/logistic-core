package com.support.core

import org.junit.Rule

abstract class BaseCoreTest {
    @get:Rule
    val rule = CoreTestRule()

}