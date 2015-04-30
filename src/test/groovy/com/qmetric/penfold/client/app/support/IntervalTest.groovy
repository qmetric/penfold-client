package com.qmetric.penfold.client.app.support

import spock.lang.Specification

import java.util.concurrent.TimeUnit

class IntervalTest extends Specification {

    def "should convert to seconds"()
    {
        expect:
        new Interval(1, TimeUnit.MINUTES).seconds() == 60
        new Interval(2, TimeUnit.MINUTES).seconds() == 120
        new Interval(1, TimeUnit.HOURS).seconds() == 3600
    }
}
