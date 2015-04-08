package com.qmetric.penfold.client.app.support

import com.qmetric.penfold.client.app.commands.filter.EqualsFilter
import groovy.json.JsonSlurper
import spock.lang.Specification

class QuerySerializerTest extends Specification {
    final serializer = new QuerySerializer(ObjectMapperFactory.create())

    def "should serialize query"()
    {
        when:
        final json = serializer.serialize([EqualsFilter.of("policyId", "p12345678")])

        then:
        new JsonSlurper().parseText(json.get()) == new JsonSlurper().parseText("""[{"op":"EQ","key":"policyId","value":"p12345678"}]""")
    }
}
