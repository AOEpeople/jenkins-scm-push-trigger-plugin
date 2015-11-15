package com.aoe.scmpushtrigger

import spock.lang.Specification

/**
 * @author Carsten Lenz, AOE
 */
class ScmPushTriggerDslSpec extends Specification {

    def dsl = new ScmPushTriggerDsl()

    def "calling method without params should return default trigger"() {
        when:
        def trigger = dsl.scmPushTrigger()

        then:
        trigger.matchExpression == ''
        trigger.useScmTrigger
    }

    def "using the closure variant values can be set explicitly"() {
        when:
        def trigger = dsl.scmPushTrigger {
            useScmTrigger(false)
            customMatchExpression('hamsdi')
        }

        then:
        !trigger.useScmTrigger
        trigger.matchExpression == 'hamsdi'
    }
}
