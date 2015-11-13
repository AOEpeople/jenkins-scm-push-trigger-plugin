package com.aoe.jenkinstrigger

import org.jenkinsci.plugins.rabbitmqconsumer.extensions.MessageQueueListener

class PushNotificationProviderAccess {
    private static PushNotificationProvider INSTANCE
    static getInstance() {
        if (INSTANCE == null) {
            INSTANCE = MessageQueueListener.all().get(RabbitMqListener)
        }
        INSTANCE
    }
}
