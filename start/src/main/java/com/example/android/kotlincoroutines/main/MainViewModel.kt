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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.kotlincoroutines.util.singleArgViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainViewModel designed to store and manage UI-related data in a lifecycle conscious way. This
 * allows data to survive configuration changes such as screen rotations. In addition, background
 * work such as fetching network results can continue through configuration changes and deliver
 * results after the new Fragment or Activity is available.
 *
 * @param repository the data source this ViewModel will fetch results from.
 */
class MainViewModel(private val repository: TitleRepository) : ViewModel() {

    companion object {
        /**
         * Factory for creating [MainViewModel]
         */
        val FACTORY = singleArgViewModelFactory(::MainViewModel)
    }

    /**
     * Request a snackbar to display an error message.
     */
    private val _snackBar = MutableLiveData<String?>()
    val snackbar: LiveData<String?>
        get() = _snackBar

    /**
     * Update title text via this LiveData
     */
    val title = repository.title


    /**
     * Show a loading spinner if true
     */
    private val _spinner = MutableLiveData<Boolean>(false)
    val spinner: LiveData<Boolean>
        get() = _spinner

    /**
     * Count of taps on the screen
     */
    private var tapCount = 0

    /**
     * LiveData with formatted tap count.
     * Observed in the Fragment. Values of taps will be set to the TextView.
     */
    private val _taps = MutableLiveData<String>("$tapCount taps")
    val taps: LiveData<String>
        get() = _taps

    /**
     * Triggered when rootLayout is tapped.
     */
    fun onMainViewClicked() {
        refreshTitle()
        updateTaps()
    }

    /**
     * Wait one second then update the tap count.
     */
    private fun updateTaps() {
        viewModelScope.launch {
            tapCount++
            //suspend this coroutine for one second.
            //The function delay is a suspend function. This is shown by the  icon in the left gutter.
            delay(1_000)
            // resume in the main dispatcher
            _taps.postValue("$tapCount taps")
        }
    }

    /**
     * Called immediately after the UI shows the snackbar.
     */
    fun onSnackbarShown() {
        _snackBar.value = null
    }

    /**
     * Refresh the title from network via repository.
     * It will update the value of title LiveData.
     * The loading spinner will display until a result is returned.
     * Errors will trigger a snackbar.
     *
     * Even though refreshTitle will make a network request and database query it can
     * use coroutines to expose a main-safe interface. This means it'll be safe to
     * call it from the main thread.
     */
    fun refreshTitle() {
        launchDataLoad {
            repository.refreshTitle()
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _spinner.value = true
                block()
            } catch (error: TitleRefreshError) {
                _snackBar.value = error.message
            } finally {
                _spinner.value = false
            }
        }
    }
}
