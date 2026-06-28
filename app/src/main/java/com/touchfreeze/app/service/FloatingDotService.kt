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
import kotlin.math.roundToInt

/**
 * Foreground service that displays a floating dot over other apps.
 * Tapping the dot toggles the screen lock.
 * Long-pressing shows a close button.
 */
class FloatingDotService : Service() {

    companion object {
        const val ACTION_SHOW = "action_show"
        const val ACTION_HIDE = "action_hide"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_dot_channel"
    }

    private var windowManager: WindowManager? = null

    // Floating dot (main UI)
    private var dotButton: ImageButton? = null
    private var dotParams: WindowManager.LayoutParams? = null
    private var closeButton: ImageButton? = null
    private var closeParams: WindowManager.LayoutParams? = null

    // Lock overlay
    private var lockOverlay: View? = null
    private var isLocked = false

    // Touch tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val dragThreshold = 20f
    private var longPressPending = false

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
        if (dotButton != null) return

        // Create the floating dot
        val params = WindowManager.LayoutParams(
            dpToPx(56),
            dpToPx(56),
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
            x = 20
            y = 400
        }
        dotParams = params

        dotButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            background = null
            alpha = 0.7f
            setOnTouchListener { _, event ->
                handleDotTouch(event, params)
            }
        }

        windowManager?.addView(dotButton, params)
    }

    private fun showCloseButton() {
        if (closeButton != null) {
            closeButton?.visibility = View.VISIBLE
            return
        }

        val params = WindowManager.LayoutParams(
            dpToPx(40),
            dpToPx(40),
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
            x = dotParams?.x ?: 20
            y = (dotParams?.y ?: 400) + dpToPx(60)
        }
        closeParams = params

        closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            background = null
            alpha = 0.9f
            setColorFilter(0xFFE91E63.toInt()) // Pink
            setOnClickListener {
                hideCloseButton()
                stopSelf()
            }
        }

        windowManager?.addView(closeButton, params)
    }

    private fun hideCloseButton() {
        closeButton?.visibility = View.GONE
    }

    private fun handleDotTouch(event: MotionEvent, params: WindowManager.LayoutParams): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                longPressPending = true

                // Schedule long-press detection
                dotButton?.postDelayed(longPressRunnable, 500)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (!isDragging && (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold)) {
                    isDragging = true
                    longPressPending = false
                    dotButton?.removeCallbacks(longPressRunnable)
                }

                if (isDragging) {
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    windowManager?.updateViewLayout(dotButton, params)

                    // Move close button too if visible
                    closeButton?.let { cb ->
                        closeParams?.x = params.x
                        closeParams?.y = params.y + dpToPx(60)
                        closeParams?.let { cp -> windowManager?.updateViewLayout(cb, cp) }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                dotButton?.removeCallbacks(longPressRunnable)

                if (!isDragging && longPressPending) {
                    // It was a tap, toggle lock
                    toggleLock()
                }

                isDragging = false
                longPressPending = false
                return true
            }
        }
        return false
    }

    private val longPressRunnable = Runnable {
        if (!isDragging) {
            longPressPending = false
            showCloseButton()
        }
    }

    private fun toggleLock() {
        if (isLocked) {
            hideLockOverlay()
        } else {
            showLockOverlay()
        }
    }

    private fun showLockOverlay() {
        if (lockOverlay != null) return

        // Create an INVISIBLE view that consumes all touch events.
        // The background is completely transparent so the video underneath
        // stays fully visible. The lock state is indicated by the dot icon change.
        lockOverlay = View(this).apply {
            setBackgroundColor(0x00000000) // 0% opacity = fully transparent
            setOnTouchListener { _, _ -> true }
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(lockOverlay, overlayParams)

        // Bring floating dot to front
        dotButton?.let { dot ->
            val dp = dot.layoutParams as WindowManager.LayoutParams
            windowManager?.removeView(dot)
            windowManager?.addView(dot, dp)
        }

        // Also bring close button if visible
        closeButton?.let { cb ->
            val cp = cb.layoutParams as WindowManager.LayoutParams
            windowManager?.removeView(cb)
            windowManager?.addView(cb, cp)
        }

        isLocked = true
        dotButton?.setImageResource(android.R.drawable.ic_secure)
        dotButton?.alpha = 1.0f
    }

    private fun hideLockOverlay() {
        lockOverlay?.let {
            windowManager?.removeView(it)
            lockOverlay = null
        }
        isLocked = false
        dotButton?.setImageResource(android.R.drawable.ic_lock_lock)
        dotButton?.alpha = 0.7f
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Lock for Kids",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating lock button visible"
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
            .setContentTitle("Screen Lock for Kids")
            .setContentText("Tap the floating dot to lock your screen")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        dotButton?.let { windowManager?.removeView(it) }
        closeButton?.let { windowManager?.removeView(it) }
        hideLockOverlay()
        dotButton = null
        closeButton = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
