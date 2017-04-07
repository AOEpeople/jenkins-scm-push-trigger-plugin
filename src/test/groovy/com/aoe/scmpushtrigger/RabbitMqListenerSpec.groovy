package com.aoe.scmpushtrigger

import spock.lang.Specification

/**
 * @author Sebastian Rose, AOE on 05.01.17.
 */
class RabbitMqListenerSpec extends Specification {

    def mqListener = new RabbitMqListener()

    def createTriggerRef(String content, boolean matching) {
        ScmPushTrigger exampleTrigger = Mock(ScmPushTrigger)
        exampleTrigger.matches(content) >> matching
        new ScmPushTriggerRef(content,exampleTrigger)
    }

    def "a listeners matchingTriggerRefs should contain matching triggers added before"() {
        when:
        ScmPushTriggerRef exampleTriggerRef1 = createTriggerRef("testUrl1", true)
        mqListener.addTrigger(exampleTriggerRef1)

        ScmPushTriggerRef exampleTriggerRef2 = createTriggerRef("testUrl2", false)
        mqListener.addTrigger(exampleTriggerRef2)

        then:
        mqListener.matchingTriggerRefs("testUrl1").contains(exampleTriggerRef1)
        !mqListener.matchingTriggerRefs("testUrl2").contains(exampleTriggerRef2)

    }

    def "a listeners triggersFromTriggerRefs should contain all triggers from given triggerRefs"() {

    }

    def "a listeners onReceive method should execute shedule build on each matching trigger"() {

    }

}
