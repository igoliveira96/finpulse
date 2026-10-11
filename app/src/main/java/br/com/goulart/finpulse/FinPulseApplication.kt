package br.com.goulart.finpulse

import android.app.Application
import android.util.Log
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinPulseApplication : Application() {
    // One instance per application process. Not injected into HomeViewModel yet.
    val database: FinPulseDatabase by lazy { FinPulseDatabase.create(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                // Room opens lazily. Trigger copying/validation off the UI thread at startup.
                database.openHelper.writableDatabase
            } catch (error: Exception) {
                Log.e("FinPulseDatabase", "Could not initialize the local database", error)
            }
        }
    }
}
