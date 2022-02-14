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

package com.google.mlkit.md

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.mlkitsample.R
import com.google.common.base.Objects
import com.google.mlkit.md.barcodedetection.BarcodeProcessor
import com.google.mlkit.md.camera.CameraSource
import com.google.mlkit.md.camera.CameraSourcePreview
import com.google.mlkit.md.camera.GraphicOverlay
import com.google.mlkit.md.camera.WorkflowModel
import com.google.mlkit.md.camera.WorkflowModel.WorkflowState
import java.io.IOException

/** Demonstrates the barcode scanning workflow using camera preview.  */
// what is this: 枠のアニメーション、カメラのミラーリング、スキャン時のアニメーション、ステータス文字列の表示、結果ビューの表示
class LiveBarcodeScanningActivity : AppCompatActivity() {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null // MLKit
    private var workflowModel: WorkflowModel? = null // MLKit
    private var currentWorkflowState: WorkflowState? = null // MLKit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_live_barcode)
        preview = findViewById(R.id.camera_preview)
        // オーバーレイ表示（スケーリングとミラーリングを担当）
        graphicOverlay = findViewById<GraphicOverlay>(R.id.camera_preview_graphic_overlay).apply {
            cameraSource = CameraSource(this) // カメラ設定
        }

        // ワークフローの設定
        setUpWorkflowModel()
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen() // カメラのライブを停止
        currentWorkflowState = WorkflowState.NOT_STARTED // 状態遷移
        cameraSource?.setFrameProcessor(BarcodeProcessor(graphicOverlay!!, workflowModel!!)) // デコードを開始？
        workflowModel?.setWorkflowState(WorkflowState.DETECTING) // 状態遷移
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    private fun startCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        val cameraSource = this.cameraSource ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            preview?.stop()
        }
    }

    // what: ViewModel
    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java)

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel!!.workflowState.observe(this, Observer { workflowState ->
            if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                return@Observer
            }

            currentWorkflowState = workflowState
            Log.d(TAG, "Current workflow state: ${currentWorkflowState!!.name}")

            when (workflowState) {
                WorkflowState.DETECTING -> {
                    startCameraPreview()
                }
                WorkflowState.CONFIRMING -> {
                    startCameraPreview()
                }
                WorkflowState.DETECTED -> {
                    stopCameraPreview()
                }
                else -> {}
            }
        })

        // バーコード検出を監視
        workflowModel?.detectedBarcode?.observe(this, Observer { barcode ->
            if (barcode != null) {
                Toast.makeText(this, barcode.rawValue, Toast.LENGTH_SHORT).show()
            }
        })
    }

    companion object {
        private const val TAG = "LiveBarcodeActivity"
    }
}
