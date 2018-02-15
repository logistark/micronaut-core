/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.scheduling

import org.particleframework.context.ApplicationContext
import org.particleframework.context.annotation.Requires
import org.particleframework.scheduling.annotation.Scheduled
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Singleton

/**
 * @author graemerocher
 * @since 1.0
 */
class ScheduledFixedRateSpec extends Specification {


    void 'test schedule task at fixed delay or rate '() {
        given:
        ApplicationContext beanContext = ApplicationContext.run(
                'some.configuration':'10ms',
                'scheduled-test.task.enabled':true
        )

        PollingConditions conditions = new PollingConditions()

        when:
        MyTask myTask = beanContext.getBean(MyTask)

        then:
        conditions.eventually {
            myTask.wasRun
            myTask.fixedDelayWasRun
            !myTask.wasDelayedRun
            beanContext.getBean(MyJavaTask).wasRun
        }

        and:
        conditions.eventually {
            myTask.wasRun
            myTask.wasDelayedRun
            beanContext.getBean(MyJavaTask).wasRun
        }
    }

    @Singleton
    @Requires(property = 'scheduled-test.task.enabled', value = 'true')
    static class MyTask {
        boolean wasRun = false
        boolean wasDelayedRun = false
        boolean fixedDelayWasRun = false
        boolean configuredWasRun = false

        @Scheduled(fixedRate = '10ms')
        void runSomething() {
            wasRun = true
        }

        @Scheduled(fixedRate = '${some.configuration}')
        void runScheduleConfigured() {
            configuredWasRun = true
        }
        @Scheduled(fixedDelay = '10ms')
        void runFixedDelay() {
            fixedDelayWasRun = true
        }

        @Scheduled(fixedRate = '10ms', initialDelay = '500ms')
        void runSomethingElse() {
            wasDelayedRun = true
        }
    }
}