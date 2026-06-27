package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.repository.ManuscriptRepository
import com.example.ui.screens.EditorShellScreen
import com.example.ui.screens.RecoveryModeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.EditorViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DbState { LOADING, READY, ERROR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var dbState by mutableStateOf(DbState.LOADING)
        var repository: ManuscriptRepository? = null
        var errorMsg: String? = null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "novellum_db"
                ).build()
                
                // Force an open to verify it works safely (catches missing migrations, corruption, etc.)
                // This will crash if destructive migration fallback was needed but not allowed.
                database.openHelper.readableDatabase
                
                repository = ManuscriptRepository(database)
                withContext(Dispatchers.Main) {
                    dbState = DbState.READY
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = e.message
                    dbState = DbState.ERROR
                }
            }
        }

        setContent {
            MyApplicationTheme {
                when (dbState) {
                    DbState.LOADING -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Loading Manuscript Vault...")
                        }
                    }
                    DbState.READY -> {
                        val viewModel: EditorViewModel = viewModel(
                            factory = EditorViewModelFactory(repository!!)
                        )
                        EditorShellScreen(viewModel = viewModel)
                    }
                    DbState.ERROR -> {
                        RecoveryModeScreen(errorMsg = errorMsg)
                    }
                }
            }
        }
    }
}
