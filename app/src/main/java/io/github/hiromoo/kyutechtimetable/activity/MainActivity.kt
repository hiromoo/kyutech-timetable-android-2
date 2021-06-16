package io.github.hiromoo.kyutechtimetable.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.databinding.ActivityMainBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.load

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        val navController = findNavController(R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.navView, navController)

        supportActionBar?.hide()

        load(this)
    }
}