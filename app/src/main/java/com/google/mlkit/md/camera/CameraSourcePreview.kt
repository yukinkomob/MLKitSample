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

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import com.example.mlkitsample.R
import com.google.android.gms.common.images.Size
import com.google.mlkit.md.Utils
import java.io.IOException

// 撮影成功時のコールバックは？
// カメラの撮影開始タイミングは？
// カメラの撮影領域の変更は可能？
// CameraSourcePreviewの利用準備は？
/** Preview the camera image in the screen.  */
// what: カメラソースの管理クラス
class CameraSourcePreview(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val surfaceView: SurfaceView = SurfaceView(context).apply {
        holder.addCallback(SurfaceCallback())
        addView(this)
    }
    private var graphicOverlay: GraphicOverlay? = null // GraphicOverlayはmlkit
    private var startRequested = false
    private var surfaceAvailable = false // コールバック（surface作成済み）時にtrue
    private var cameraSource: CameraSource? = null // start()時に設定
    private var cameraPreviewSize: Size? = null // onLayoutの時に引数を基に設定

    // when: レイアウトの設定が完了
    override fun onFinishInflate() {
        super.onFinishInflate()
        graphicOverlay = findViewById(R.id.camera_preview_graphic_overlay)
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource) { // CameraSource はどこから？（mlkit）
        this.cameraSource = cameraSource
        startRequested = true // ここでフラグ立ててるのは意味ある？
        startIfReady() // ここ以外から呼ばれる？ xxxIfyyyはどういう効果？ -> 内部でチェックされることが保証されている
    }

    fun stop() {
        cameraSource?.let {
            it.stop()
            cameraSource = null
            startRequested = false
        }
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) { // IfReady
            // リクエストあり＆画面利用可能
            cameraSource?.start(surfaceView.holder) // カメラ開始。holderは何？（SurfaceViewはこのビューの親で、描画用のビュー）
            requestLayout() // API
            graphicOverlay?.let { overlay ->
                cameraSource?.let {
                    overlay.setCameraInfo(it) // overlayは何？
                }
                overlay.clear()
            }
            startRequested = false
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val layoutWidth = right - left
        val layoutHeight = bottom - top

        cameraSource?.previewSize?.let { cameraPreviewSize = it }

        // what: 比率を設定
        val previewSizeRatio = cameraPreviewSize?.let { size ->
            if (Utils.isPortraitMode(context)) {
                // Camera's natural orientation is landscape, so need to swap width and height.
                size.height.toFloat() / size.width
            } else {
                size.width.toFloat() / size.height
            }
        } ?: layoutWidth.toFloat() / layoutHeight.toFloat()

        // Match the width of the child view to its parent.
        // what: 高さを設定
        val childHeight = (layoutWidth / previewSizeRatio).toInt()
        if (childHeight <= layoutHeight) {
            for (i in 0 until childCount) {
                getChildAt(i).layout(0, 0, layoutWidth, childHeight)
            }
        } else {
            // When the child view is too tall to be fitted in its parent: If the child view is
            // static overlay view container (contains views such as bottom prompt chip), we apply
            // the size of the parent view to it. Otherwise, we offset the top/bottom position
            // equally to position it in the center of the parent.
            // what: 画面を超過する時の調整
            val excessLenInHalf = (childHeight - layoutHeight) / 2
            for (i in 0 until childCount) {
                val childView = getChildAt(i)
                when (childView.id) {
                    R.id.static_overlay_container -> {
                        childView.layout(0, 0, layoutWidth, layoutHeight)
                    }
                    else -> {
                        childView.layout(
                            0, -excessLenInHalf, layoutWidth, layoutHeight + excessLenInHalf
                        )
                    }
                }
            }
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    // how to use: holder（SurfaceView）にインスタンスを設定（引数なし）
    private inner class SurfaceCallback : SurfaceHolder.Callback {
        // SurfaceViewが作成、破棄、変更時に通知
        // 作成を契機にカメラ開始

        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }
    }

    companion object {
        private const val TAG = "CameraSourcePreview"
    }
}
