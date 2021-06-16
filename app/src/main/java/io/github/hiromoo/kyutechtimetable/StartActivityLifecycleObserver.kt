package io.github.hiromoo.kyutechtimetable

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class StartActivityLifecycleObserver (
    private val registry: ActivityResultRegistry,
    private val key: String,
    private val callback: ActivityResultCallback<ActivityResult?>
) : DefaultLifecycleObserver {

    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(owner: LifecycleOwner) {
        startForResult =
            registry.register(
                key,
                owner,
                ActivityResultContracts.StartActivityForResult(),
                callback
            )
    }

    fun launch(intent: Intent) {
        startForResult.launch(intent)
    }

    fun launch(context: Context, activityClass: Class<out AppCompatActivity>) {
        launch(Intent(context, activityClass))
    }
}