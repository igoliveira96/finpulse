package br.com.goulart.finpulse

import android.app.Application
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.repository.HomeRepository
import br.com.goulart.finpulse.data.repository.RoomHomeRepository

class FinPulseApplication : Application() {
    // Shared dependencies for this process; Room opens on the first asynchronous query.
    val database: FinPulseDatabase by lazy { FinPulseDatabase.create(this) }
    val homeRepository: HomeRepository by lazy {
        RoomHomeRepository(database)
    }
}
