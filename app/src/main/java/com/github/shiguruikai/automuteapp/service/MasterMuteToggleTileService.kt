package com.github.shiguruikai.automuteapp.service

import android.annotation.TargetApi
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import com.github.shiguruikai.automuteapp.R
import com.github.shiguruikai.automuteapp.util.isMasterMute
import com.github.shiguruikai.automuteapp.util.setMasterMute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@TargetApi(Build.VERSION_CODES.N)
class MasterMuteToggleTileService : TileService(), CoroutineScope by MainScope() {

    private val audioManager by lazy { getSystemService<AudioManager>()!! }

    override fun onStartListening() {
        launch {
            while (isActive) {
                val isMuted = audioManager.isMasterMute()
                updateTile(isMuted)
                delay(UPDATE_STATE_INTERVAL)
            }
        }
    }

    override fun onStopListening() {
        coroutineContext.cancelChildren()
    }

    override fun onDestroy() {
        cancel()
    }

    override fun onClick() {
        val isMuted = audioManager.isMasterMute()
        audioManager.setMasterMute(!isMuted)
        updateTile(!isMuted)
    }

    private fun updateTile(active: Boolean) {
        val tile = qsTile

        if (active && tile.state == Tile.STATE_ACTIVE) {
            return
        }

        val newIcon: Int
        val newState: Int

        if (active) {
            newState = Tile.STATE_ACTIVE
            newIcon = R.drawable.ic_volume_off_black_24dp
        } else {
            newState = Tile.STATE_INACTIVE
            newIcon = R.drawable.ic_volume_up_black_24dp
        }

        tile.state = newState
        tile.icon = IconCompat.createWithResource(applicationContext, newIcon).toIcon()
        tile.label = getString(R.string.mute)

        tile.updateTile()
    }

    companion object {
        private val TAG = MasterMuteToggleTileService::class.java.simpleName

        private const val UPDATE_STATE_INTERVAL = 500L
    }
}
