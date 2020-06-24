/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkCompletableFake
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TitleRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    @Test
    fun whenRefreshTitleSuccess_insertsRows() = runBlockingTest {
        val subject = TitleRepository(
                MainNetworkFake("OK"),
                TitleDaoFake("title")
        )

        subject.refreshTitle()
    }

    /**
     * This test uses the provided fake MainNetworkCompletableFake, which is a network fake that's
     * designed to suspend callers until the test continues them. When refreshTitle tries to make
     * a network request, it'll hang forever because we want to test timeouts.
     * Then, it launches a separate coroutine to call refreshTitle. This is a key part of testing
     * timeouts, the timeout should happen in a different coroutine than the one runBlockingTest
     * creates. By doing so, we can call the next line, advanceTimeBy(5_000) which will advance
     * time by 5 seconds and cause the other coroutine to timeout.
     */
    @Test(expected = TitleRefreshError::class)
    fun whenRefreshTitleTimeout_throws() = runBlockingTest{
        val network = MainNetworkCompletableFake()
        val subject = TitleRepository(
                network,
                TitleDaoFake("title")
        )

        launch{
            subject.refreshTitle()
        }
        advanceTimeBy(5_000L)
    }
}