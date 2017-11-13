package com.navercorp.pinpoint.plugin.apsaras;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * @author chenjunhua
 */
public class ApsarasTraceMetadataProvider implements TraceMetadataProvider {
    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(ApsarasConstants.APSARAS_PROVIDER_SERVICE_TYPE);
        context.addServiceType(ApsarasConstants.APSARAS_CONSUMER_SERVICE_TYPE);
        context.addAnnotationKey(ApsarasConstants.APSARAS_ARGS_ANNOTATION_KEY);
        context.addAnnotationKey(ApsarasConstants.APSARAS_RESULT_ANNOTATION_KEY);
    }
}
