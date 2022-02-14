/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.md.camera

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.md.objectdetection.DetectedObjectInfo
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.HashSet

/** View model for handling application workflow based on camera preview.  */
class WorkflowModel(application: Application) : AndroidViewModel(application) {

    val workflowState = MutableLiveData<WorkflowState>()
    val detectedBarcode = MutableLiveData<Barcode>() // ML Kit

    private val objectIdsToSearch = HashSet<Int>()

    var isCameraLive = false
        private set

    private var confirmedObject: DetectedObjectInfo? = null

    /**
     * State set of the application workflow.
     */
    // 初期 > 検出中 > 検出済み > 確認中 > 確認済み > 検索中 > 検索済み
    enum class WorkflowState {
        NOT_STARTED, // onResume, onPause
        DETECTING, // onResumeでsetFrameProcessor直後
        DETECTED, // barcodeProcessorでonSuccess時
        CONFIRMING, // barcodeProcessorでonSuccess時（サイズが小さい時）
        CONFIRMED, // confirmingObject()で自動サーチがOFFの時
        SEARCHING, // confirmingObject()で自動サーチがONの時
        SEARCHED // WorkflowModelにてonSearchCompleted()
    }

    @MainThread
    fun setWorkflowState(workflowState: WorkflowState) {
        if (workflowState != WorkflowState.CONFIRMED &&
            workflowState != WorkflowState.SEARCHING &&
            workflowState != WorkflowState.SEARCHED
        ) {
            confirmedObject = null
        }
        this.workflowState.value = workflowState
    }

    fun markCameraLive() {
        isCameraLive = true
        objectIdsToSearch.clear()
    }

    fun markCameraFrozen() {
        isCameraLive = false
    }
}
