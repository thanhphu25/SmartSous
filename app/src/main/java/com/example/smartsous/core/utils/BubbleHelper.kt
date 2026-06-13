package com.example.smartsous.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.smartsous.BubbleActivity
import com.example.smartsous.R

object BubbleHelper {
    private const val CHANNEL_ID = "bubble_chat_channel_v4"
    private const val NOTIFICATION_ID = 1004
    private const val SHORTCUT_ID = "chatbot_shortcut_v4"
    private const val LOCUS_ID = "chatbot_locus"

    fun showChatBubble(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val person = Person.Builder()
                .setName("SmartSous Assistant")
                .setImportant(true)
                .build()

            val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher_round)
            val targetIntent = Intent(context, BubbleActivity::class.java).apply {
                action = Intent.ACTION_VIEW
            }
            val locusId = LocusIdCompat(LOCUS_ID)

            createNotificationChannel(context)
            createShortcut(context, person, icon, targetIntent, locusId)
            showBubbleNotification(context, person, icon, targetIntent, locusId)

            // Đưa app về Home screen của hệ điều hành
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        }
    }

    fun dismissChatBubble(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trợ lý Nấu ăn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bong bóng chat SmartSous Assistant"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createShortcut(
        context: Context, 
        person: Person, 
        icon: IconCompat, 
        targetIntent: Intent, 
        locusId: LocusIdCompat
    ) {
        val shortcutInfo = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel("Trợ lý AI")
            .setLongLabel("Trợ lý SmartSous")
            .setIcon(icon)
            .setIntent(targetIntent)
            .setPerson(person) // Bắt buộc phải có Person cho Conversation Shortcut
            .setLocusId(locusId)
            .setLongLived(true)
            .build()
            
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)
    }

    private fun showBubbleNotification(
        context: Context, 
        person: Person, 
        icon: IconCompat, 
        targetIntent: Intent, 
        locusId: LocusIdCompat
    ) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val bubbleIntent = PendingIntent.getActivity(context, 0, targetIntent, flags)
        
        val bubbleData = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
            .setDesiredHeight(600)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()

        val locusId = LocusIdCompat(LOCUS_ID)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SmartSous")
            .setContentText("Trợ lý AI đang chờ...")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setBubbleMetadata(bubbleData)
            .setShortcutId(SHORTCUT_ID)
            .setLocusId(locusId)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addPerson(person)
            .setStyle(NotificationCompat.MessagingStyle(person)
                .addMessage("Trợ lý AI đang chờ bạn tương tác...", System.currentTimeMillis(), person)
            )
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
