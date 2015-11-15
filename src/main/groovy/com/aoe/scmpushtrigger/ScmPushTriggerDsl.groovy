package com.aoe.scmpushtrigger

import hudson.Extension
import javaposse.jobdsl.dsl.Context
import javaposse.jobdsl.dsl.helpers.triggers.TriggerContext
import javaposse.jobdsl.plugin.ContextExtensionPoint
import javaposse.jobdsl.plugin.DslExtensionMethod

/**
 * Optional support for Job DSL
 *
 * @author Carsten Lenz, AOE
 */
@Extension(optional = true)
class ScmPushTriggerDsl extends ContextExtensionPoint {

    @DslExtensionMethod(context = TriggerContext)
    def scmPushTrigger() {
        new ScmPushTrigger('', true)
    }

    @DslExtensionMethod(context = TriggerContext)
    def scmPushTrigger(Closure cls) {
        def context = new ScmPushTriggerDslContext()
        executeInContext(cls, context)
        new ScmPushTrigger(context.customMatchExpression, context.useScmTrigger)
    }
}

class ScmPushTriggerDslContext implements Context {
    String customMatchExpression = ''
    boolean useScmTrigger = true

    void customMatchExpression(String expression = '') {
        customMatchExpression = expression
    }

    void useScmTrigger(boolean useScmTrigger = true) {
        this.useScmTrigger = useScmTrigger
    }
}
