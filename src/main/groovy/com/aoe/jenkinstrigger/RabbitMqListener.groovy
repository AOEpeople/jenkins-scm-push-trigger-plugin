package com.aoe.jenkinstrigger

import hudson.Extension
import hudson.model.Project
import jenkins.model.Jenkins
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
class RabbitMqListener extends MessageQueueListener {

    private static final Logger LOGGER = Logger.getLogger(RabbitMqListener.class.getName())

    private final Set<PushTriggerRef> triggerRefs = new CopyOnWriteArraySet<>()

    @Override
    String getName() { "Push Trigger" }

    @Override
    String getAppId() { "push-trigger" }

    void addTrigger(PushTriggerRef triggerRef) {
        triggerRefs.remove(triggerRef)
        triggerRefs.add(triggerRef)
    }

    void removeTrigger(PushTriggerRef triggerRef) {
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

        def matchingTriggers = triggerRefs.findAll { it.matches(content) }

        def allProjects = Jenkins.getInstance().getAllItems(Project)

        matchingTriggers.each { triggerRef ->
            def job = allProjects.find { it.name == triggerRef.projectName }
            def trigger = job?.getTrigger(PushTrigger) as PushTrigger
            if (!trigger) {
                throw new IllegalStateException("Trigger registered but none found on referenced Project??")
            }
            trigger.scheduleBuild(queueName, content)
        }
    }
}
