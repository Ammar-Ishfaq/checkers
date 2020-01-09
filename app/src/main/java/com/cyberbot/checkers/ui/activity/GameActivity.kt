package com.cyberbot.checkers.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cyberbot.checkers.R
import com.cyberbot.checkers.fx.getRandomAiThinkSoundRes
import com.cyberbot.checkers.fx.getRandomMoveSoundRes
import com.cyberbot.checkers.fx.play
import com.cyberbot.checkers.game.Grid
import com.cyberbot.checkers.game.GridEntry
import com.cyberbot.checkers.game.PlayerNum
import com.cyberbot.checkers.preferences.Preferences
import com.cyberbot.checkers.ui.view.MoveAttemptListener
import kotlinx.android.synthetic.main.activity_game.*


class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        move_player2.text = getString(R.string.game_player_turn_info)

        val pref = Preferences.fromContext(this)
        val gridData = Grid(pref.gridSize, pref.playerRows)
        settingsGridPreview.gridData = gridData

        settingsGridPreview.moveAttemptListener = object : MoveAttemptListener {
            override fun onForcedMoveStart(grid: Grid, srcEntry: GridEntry, dstEntry: GridEntry) {
                play(this@GameActivity, getRandomMoveSoundRes())
                move_player2.text = "Busy"
            }

            override fun onForcedMoveEnd(grid: Grid, srcEntry: GridEntry, dstEntry: GridEntry) {
                grid.attemptMove(srcEntry, dstEntry)
                move_player2.text = getString(R.string.game_player_turn_info)
            }

            override fun onUserMoveStart(grid: Grid, srcEntry: GridEntry) {

            }

            override fun onUserMoveEnd(grid: Grid, srcEntry: GridEntry, dstEntry: GridEntry) {
                if (srcEntry == dstEntry) {
                    return
                }

                grid.attemptMove(srcEntry, dstEntry)
                if (dstEntry.player == PlayerNum.SECOND) {
                    val src: GridEntry = gridData.filter {
                        it.player == PlayerNum.FIRST
                    }.random()

                    val dst: GridEntry = gridData.filter {
                        it != src && gridData.moveAllowed(src, it)
                    }.random()


                    Thread {
                        play(this@GameActivity, getRandomAiThinkSoundRes())
                        Thread.sleep(1000)
                        runOnUiThread {
                            settingsGridPreview.attemptMove(src, dst)
                        }
                    }.start()
                }
            }
        }
    }
}