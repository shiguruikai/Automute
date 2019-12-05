package com.github.shiguruikai.automuteapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main_with_nav.*

class MainActivity : AppCompatActivity() {

    private val appBarConfiguration = AppBarConfiguration.Builder(TOP_LEVEL_DESTINATION_IDS).build()

    private val navController by lazy { findNavController(R.id.nav_host_fragment) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_with_nav)

        // Toolbar, BottomNavigationView をセットアップ
        // https://developer.android.com/guide/navigation/navigation-ui#action_bar
        // https://developer.android.com/guide/navigation/navigation-ui#bottom_navigation
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigationView.setupWithNavController(navController)

        // androidx.navigation 2.1.0 現在、
        // ボトムメニューの再選択によって、フラグメントを再生成してしまう。
        // 対策として、再選択時に呼ばれるコールバックに何もしないリスナーをセットする。
        bottomNavigationView.setOnNavigationItemReselectedListener { }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)

        val privacyPolicyMenuItem = menu.findItem(R.id.privacy_policy_menu_item)

        privacyPolicyMenuItem.setOnMenuItemClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
            startActivity(intent)

            true
        }

        return true
    }

    /**
     * [https://developer.android.com/guide/navigation/navigation-ui#action_bar]
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val TOP_LEVEL_DESTINATION_IDS = setOf(
            R.id.nav_mute_settings,
            R.id.nav_notification_settings,
            R.id.nav_select_app,
            R.id.nav_select_activity
        )

        private const val PRIVACY_POLICY_URL = "https://sites.google.com/view/automute/privacy-policy"
    }
}
