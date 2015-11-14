package com.aoe.scmpushtrigger

import hudson.Extension
import org.jenkinsci.plugins.rabbitmqconsumer.extensions.MessageQueueListener

import java.util.concurrent.CopyOnWriteArraySet
import java.util.logging.Logger

/**
 * Integrates with the RabbitMQ Consumer Plugin. On message arrivals it tries to find all triggers
 * that match the content of the message and invokes them.
 *
 * @author Carsten Lenz, AOE
 */
@Extension
class RabbitMqListener extends MessageQueueListener implements PushNotificationProvider {

    private static final Logger LOGGER = Logger.getLogger(RabbitMqListener.class.getName())

    private final Set<ScmPushTriggerRef> triggerRefs = new CopyOnWriteArraySet<>()

    @Override
    String getName() { "SCM Push Trigger" }

    @Override
    String getAppId() { "scm-push-trigger" }

    void addTrigger(ScmPushTriggerRef triggerRef) {
        triggerRefs.remove(triggerRef)
        triggerRefs.add(triggerRef)
    }

    void removeTrigger(ScmPushTriggerRef triggerRef) {
        triggerRefs.remove(triggerRef)
    }

    @Override
    void onBind(String queueName) {
        LOGGER.info("Bind to: $queueName")
    }

    @Override
    void onUnbind(String queueName) {
        LOGGER.info("Unbind from: $queueName")
    }

    @Override
    void onReceive(String queueName, String contentType, Map<String, Object> headers, byte[] body) {
        def content = new String(body, 'UTF-8')
        LOGGER.warning("Received message $content")

        def matchingTriggerRefs = triggerRefs.findAll { it.matches(content) }
        def matchingTriggers = matchingTriggerRefs.collect { it.trigger }

        matchingTriggers*.scheduleBuild(queueName, content)
    }
}
