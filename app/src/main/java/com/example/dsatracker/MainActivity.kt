package com.example.dsatracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dsatracker.ui.theme.DSATrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.dsatracker.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DSATrackerTheme {
                val user = FirebaseAuth.getInstance().currentUser
                val startDest = if (user != null) "dashboard" else "login"
                NavGraph(startDest)
            }
        }
    }
}

