package com.aoe.jenkinstrigger

import hudson.Extension
import hudson.model.AbstractProject
import hudson.model.Cause
import hudson.model.Item
import hudson.model.Project
import hudson.model.listeners.ItemListener
import hudson.triggers.SCMTrigger
import hudson.triggers.Trigger
import hudson.triggers.TriggerDescriptor
import jenkins.model.Jenkins
import org.apache.log4j.Logger
import org.jenkinsci.plugins.rabbitmqconsumer.extensions.MessageQueueListener
import org.kohsuke.stapler.DataBoundConstructor

/**
 * A trigger that is based on push-notifications based instead of regular schedules and polling.
 *
 * @author Carsten Lenz, AOE
 */
class PushTrigger extends Trigger<AbstractProject<?, ?>> {

    private static Logger LOGGER = Logger.getLogger(PushTrigger.class.getName())

    final String matchExpression
    final boolean useScmTrigger

    @DataBoundConstructor
    PushTrigger(String matchExpression, boolean useScmTrigger) {
        this.matchExpression = matchExpression
        this.useScmTrigger = useScmTrigger
    }

    PushTriggerRef getTriggerRef() {
        new PushTriggerRef(job.name, matchExpression)
    }

    @Override
    void start(AbstractProject<?, ?> project, boolean newInstance) {
        super.start(project, newInstance)
        MessageQueueListener.all().get(RabbitMqListener)?.addTrigger(triggerRef)
    }

    @Override
    void stop() {
        MessageQueueListener.all().get(RabbitMqListener)?.removeTrigger(triggerRef)
        super.stop()
    }

    void scheduleBuild(String queueName, String content) {
        if (useScmTrigger) {
            def trigger = job.getTrigger(SCMTrigger)
            if (trigger) {
                trigger.run()
                return
            }
            else {
                LOGGER.warn("No SCM Trigger found although 'Use SCM Trigger' configured. Falling back to simply scheduling a build")
            }
        }

        job.scheduleBuild2(0, new PushTriggerBuildCause(queueName, content))
    }

    /**
     * Jenkins will associate this Descriptor with the enclosing class automatically.
     */
    @Extension
    static class PushTriggerDescriptor extends TriggerDescriptor {

        final String displayName = "Push Trigger"

        @Override
        boolean isApplicable(Item item) {
            true
        }
    }
}

class PushTriggerRef {
    final String projectName
    final String matchExpression

    PushTriggerRef(String projectName, String matchExpression) {
        this.projectName = projectName
        this.matchExpression = matchExpression
    }

    boolean matches(String content)  {
        matchExpression == content
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        PushTriggerRef that = (PushTriggerRef) o

        if (projectName != that.projectName) return false

        return true
    }

    int hashCode() {
        return projectName.hashCode()
    }
}


class PushTriggerBuildCause extends Cause {

    final String queueName
    final String content

    PushTriggerBuildCause(String queueName, String content) {
        this.queueName = queueName
        this.content = content
    }

    @Override
    String getShortDescription() {
        "Triggered by push-trigger message to queue '$queueName' with content '$content'"
    }
}

/**
 * Will register all triggers with the listener on Jenkins startup
 */
@Extension
class ItemListenerImpl extends ItemListener {
    @Override
    void onLoaded() {
        def listener = MessageQueueListener.all().get(RabbitMqListener)
        def triggers = Jenkins.getInstance().getAllItems(Project)*.getTrigger(PushTrigger)
        triggers.removeAll { !it }
        triggers.each { trigger ->
            listener?.addTrigger(trigger.triggerRef)
        }
    }
}