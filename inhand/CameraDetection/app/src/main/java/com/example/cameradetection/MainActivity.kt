package com.example.cameradetection

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate: checkPermission : ${checkPermission(this)}")
        if (!checkPermission(this)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, 100)
        }

        findViewById<Button>(R.id.move_button).setOnClickListener {
            if (checkPermission(this)) {
                Log.d(TAG, "onCreate: 권한 있음")
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, 100)
            }
        }
    }


    private fun checkPermission(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val TAG = "MainActivity"
    }
}
