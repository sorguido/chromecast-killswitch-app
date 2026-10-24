package it.sorguido.chromecastkillswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.sorguido.chromecastkillswitch.ui.KillSwitchApp
import it.sorguido.chromecastkillswitch.ui.UiActions

class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KillSwitchApp(model.state.collectAsStateWithLifecycle().value, UiActions(
                onRefresh = model::refresh, onDiscover = model::discover,
                onPair = model::pair, onConnect = model::connect, onOperate = model::operate,
            ))
        }
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { model.background(); super.onStop() }
}
