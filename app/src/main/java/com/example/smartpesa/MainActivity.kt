package com.example.smartpesa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.smartpesa.data.preferences.ThemePreference
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import com.example.smartpesa.ui.navigation.NavGraph
import com.example.smartpesa.ui.theme.SmartPesaTheme
import com.example.smartpesa.util.CurrencyFormatter
import com.example.smartpesa.util.DateFormatFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by userPreferencesRepository.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
            val currencyCode by userPreferencesRepository.currencyPreference.collectAsState(initial = "KES")
            val datePattern by userPreferencesRepository.dateFormatPreference.collectAsState(initial = "dd MMM yyyy")

            LaunchedEffect(currencyCode) {
                CurrencyFormatter.setCurrencyCode(currencyCode)
            }
            LaunchedEffect(datePattern) {
                DateFormatFormatter.setPattern(datePattern)
            }

            SmartPesaTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(userPreferencesRepository = userPreferencesRepository)
                }
            }
        }
    }
}
