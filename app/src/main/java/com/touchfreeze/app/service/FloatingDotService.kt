package com.touchfreeze.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.touchfreeze.app.MainActivity
import kotlin.math.abs

class FloatingDotService : Service() {

    companion object {
        const val ACTION_SHOW = "action_show"
        const val ACTION_HIDE = "action_hide"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_dot_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var lockOverlay: View? = null
    private var isLocked = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val dragThreshold = 20f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showFloatingDot()
            }
        }
        return START_STICKY
    }

    private fun showFloatingDot() {
        if (floatingView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        floatingView = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            background = null
            alpha = 0.6f
            setOnTouchListener { view, event ->
                handleDotTouch(event, view, params)
            }
        }

        windowManager?.addView(floatingView, params)
    }

    private fun handleDotTouch(event: MotionEvent, view: View, params: WindowManager.LayoutParams): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (!isDragging && (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold)) {
                    isDragging = true
                }

                if (isDragging) {
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    windowManager?.updateViewLayout(view, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    toggleLock()
                }
                return true
            }
        }
        return false
    }

    private fun toggleLock() {
        if (isLocked) {
            showUnlockPrompt()
        } else {
            showLockOverlay()
        }
    }

    private fun showLockOverlay() {
        if (lockOverlay != null) return

        // 1. Add the lock overlay first (behind the dot)
        lockOverlay = View(this).apply {
            setBackgroundColor(0x80000000.toInt())
            setOnTouchListener { _, _ -> true } // Consume all touch events
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(lockOverlay, overlayParams)

        // 2. Bring the floating dot to the front by re-adding it
        // The last view added to WindowManager is on top
        floatingView?.let { fv ->
            val dotParams = fv.layoutParams as WindowManager.LayoutParams
            windowManager?.removeView(fv)
            windowManager?.addView(fv, dotParams)
        }

        isLocked = true
        // Change dot to unlock icon
        (floatingView as? ImageButton)?.setImageResource(android.R.drawable.ic_secure)
    }

    private fun showUnlockPrompt() {
        hideLockOverlay()
    }

    private fun hideLockOverlay() {
        lockOverlay?.let {
            windowManager?.removeView(it)
            lockOverlay = null
        }
        isLocked = false
        (floatingView as? ImageButton)?.setImageResource(android.R.drawable.ic_lock_lock)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Dot Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating dot visible for screen lock"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Touch Freeze Active")
            .setContentText("Tap the floating dot to lock/unlock screen")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
        hideLockOverlay()
        floatingView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
