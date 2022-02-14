package com.example.mlkitsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.md.LiveBarcodeScanningActivity

class MainActivity : AppCompatActivity() {

    private val RequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.goToButton).apply {
            setOnClickListener {
                goToBarcodeActivity()
            }
        }

        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            requestCameraPermission()
            return
        }
        goToBarcodeActivity()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RequestCode -> {
                if (isPermissionGranted(grantResults)) {
                    goToBarcodeActivity()
                } else {
                    Toast.makeText(this, "パーミッションが付与されませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isPermissionGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun goToBarcodeActivity() {
        val intent = Intent(this, LiveBarcodeScanningActivity::class.java)
        startActivity(intent)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RequestCode)
    }
}