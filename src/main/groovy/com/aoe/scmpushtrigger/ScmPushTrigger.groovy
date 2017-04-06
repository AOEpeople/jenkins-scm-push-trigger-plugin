package com.aoe.scmpushtrigger

import hudson.Extension
import hudson.model.AbstractProject
import hudson.model.Cause
import hudson.model.Item
import hudson.model.Job
import hudson.model.JobProperty
import hudson.model.Project
import hudson.model.listeners.ItemListener
import hudson.scm.SCM
import hudson.triggers.SCMTrigger
import hudson.triggers.Trigger
import hudson.triggers.TriggerDescriptor
import hudson.util.FormValidation
import javaposse.jobdsl.dsl.jobs.WorkflowJob
import jenkins.model.Jenkins
import jenkins.triggers.SCMTriggerItem
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import java.util.logging.Logger

/**
 * A trigger that is based on push-notifications based instead of regular schedules and polling.
 *
 * @author Carsten Lenz, AOE
 */
class ScmPushTrigger extends Trigger<Job> {

    private static Logger LOGGER = Logger.getLogger(ScmPushTrigger.class.getName())

    final String matchExpression
    final boolean useScmTrigger

    private volatile transient ScmPushTriggerRef triggerRef

    @DataBoundConstructor
    ScmPushTrigger(String matchExpression, boolean useScmTrigger) {
        this.matchExpression = matchExpression
        this.useScmTrigger = useScmTrigger
    }

    ScmPushTriggerRef getTriggerRef() {
        if (!triggerRef) {
            if (!job) {
                throw new IllegalStateException("Creating triggerRef before start()")
            }
            triggerRef = new ScmPushTriggerRef(job.fullName, this)
        }
        triggerRef
    }

    public List<String> getScmUrls() {
        if(job.hasProperty('SCMs')) {
            LOGGER.warning("Found SCMs ${job.SCMs}")
            def scms = job.SCMs.findAll { isGitSCM(it) }
            LOGGER.warning("Found GIT SCMs $scms")
            def collectNested = scms.collectNested { it.userRemoteConfigs*.url }
            LOGGER.warning("Found GIT SCMs nestsed ${collectNested.flatten()}")
            collectNested.flatten()
        } else {
            LOGGER.warning("Currently only Git (optionally with MultiSCM) as SCM is supported")
            []
        }
    }

    private boolean isGitSCM(SCM scm) {
        scm.getClass().name == 'hudson.plugins.git.GitSCM'
    }

    private boolean isMultiSCM(SCM scm) {
        scm.getClass().name == 'org.jenkinsci.plugins.multiplescms.MultiSCM'
    }

    @Override
    void start(Job job, boolean newInstance) {
        super.start(job, newInstance)

        PushNotificationProviderAccess.instance.addTrigger(getTriggerRef())
    }

    @Override
    void stop() {
        //while stop is called, we don't need creation of a new trigger ref
        if (triggerRef) {
            PushNotificationProviderAccess.instance.removeTrigger(getTriggerRef())
        }
        super.stop()
    }

    boolean matches(String content) {
        if (matchExpression) {
            matchExpression == content
        } else {
            getScmUrls().contains(content)
        }
    }

    void scheduleBuild(String queueName, String content) {
        if (useScmTrigger) {
            def trigger = job.getSCMTrigger()
            if (trigger) {
                trigger.run()
                LOGGER.info("Trigger for job ${trigger.job.fullName} was started.")
                return
            } else {
                LOGGER.warning("No SCM Trigger found although 'Use SCM Trigger' configured. Falling back to simply scheduling a build")
            }
        }

        job.scheduleBuild2(0, new ScmPushTriggerBuildCause(queueName, content))
    }

    /**
     * Jenkins will associate this Descriptor with the enclosing class automatically.
     */
    @Extension
    static class PushTriggerDescriptor extends TriggerDescriptor {

        final String displayName = "RabbitMQ SCM Push Trigger"

        @Override
        boolean isApplicable(Item item) {
            true
        }

        FormValidation doCheckUseScmTrigger(
                @QueryParameter boolean value, @AncestorInPath AbstractProject project) {

            if (value && project) {
                def trigger = project.getTrigger(SCMTrigger) as SCMTrigger
                if (!trigger) {
                    return FormValidation.warning("SCM Trigger is not activated! SCM Trigger must be " +
                            "activated for this trigger to delegate to SCM Trigger")
                }
                if (trigger.getSpec()) {
                    return FormValidation.warning("You have a Cron expression in your SCM Trigger -" +
                            "This may be intentional but be aware that your build is still actively " +
                            "polling Git.")
                }
            }
            FormValidation.ok()
        }
    }
}

/**
 * Used for subscribing to the queue listener
 */
class ScmPushTriggerRef {
    final String projectName
    final ScmPushTrigger trigger

    ScmPushTriggerRef(String projectName, ScmPushTrigger trigger) {
        this.projectName = projectName
        this.trigger = trigger
    }

    boolean matches(String content) {
        trigger.matches(content)
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ScmPushTriggerRef that = (ScmPushTriggerRef) o

        if (projectName != that.projectName) return false

        return true
    }

    int hashCode() {
        return projectName.hashCode()
    }

    String toString() {
        "Project: ${projectName}; SCMUrls: ${trigger.scmUrls}"
    }
}


class ScmPushTriggerBuildCause extends Cause {

    final String queueName
    final String content

    ScmPushTriggerBuildCause(String queueName, String content) {
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
class ScmPushTriggerItemListenerImpl extends ItemListener {

    @Override
    void onLoaded() {
        def provider = PushNotificationProviderAccess.instance
        def triggers = Jenkins.getInstance().getAllItems(Project)*.getTrigger(ScmPushTrigger)
        triggers.removeAll { !it }
        triggers.each { trigger ->
            provider.addTrigger(trigger.triggerRef)
        }
    }

}