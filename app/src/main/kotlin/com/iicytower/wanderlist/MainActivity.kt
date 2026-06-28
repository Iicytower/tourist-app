package com.iicytower.wanderlist

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.iicytower.wanderlist.navigation.WanderListNavGraph
import com.iicytower.wanderlist.ui.theme.WanderListTheme

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently — UI shows guidance when needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        setContent {
            WanderListTheme {
                WanderListNavGraph()
            }
        }
    }
}
