package com.aoe.jenkinstrigger

/**
 * @author Carsten Lenz, AOE
 */
interface PushNotificationProvider {

    void addTrigger(ScmPushTriggerRef triggerRef)

    void removeTrigger(ScmPushTriggerRef triggerRef)
}
