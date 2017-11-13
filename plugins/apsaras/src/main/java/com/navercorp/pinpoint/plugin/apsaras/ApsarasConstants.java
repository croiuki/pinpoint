package com.navercorp.pinpoint.plugin.apsaras;

import com.navercorp.pinpoint.common.trace.*;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;
import static com.navercorp.pinpoint.common.trace.AnnotationKeyProperty.*;

/**
 * @author chenjunhua
 */
public interface ApsarasConstants {

    ServiceType APSARAS_PROVIDER_SERVICE_TYPE = ServiceTypeFactory.of(1910, "APSARAS_PROVIDER", RECORD_STATISTICS);
    ServiceType APSARAS_CONSUMER_SERVICE_TYPE = ServiceTypeFactory.of(9910, "APSARAS_CONSUMER", RECORD_STATISTICS);
    AnnotationKey APSARAS_ARGS_ANNOTATION_KEY = AnnotationKeyFactory.of(990, "apsaras.args", VIEW_IN_RECORD_SET);
    AnnotationKey APSARAS_RESULT_ANNOTATION_KEY = AnnotationKeyFactory.of(991, "apsaras.result");

//    String META_DO_NOT_TRACE = "_APSARAS_DO_NOT_TRACE";
//    String META_TRANSACTION_ID = "_APSARAS_TRASACTION_ID";
//    String META_SPAN_ID = "_APSARAS_SPAN_ID";
//    String META_PARENT_SPAN_ID = "_APSARAS_PARENT_SPAN_ID";
//    String META_PARENT_APPLICATION_NAME = "_APSARAS_PARENT_APPLICATION_NAME";
//    String META_PARENT_APPLICATION_TYPE = "_APSARAS_PARENT_APPLICATION_TYPE";
//    String META_FLAGS = "_APSARAS_FLAGS";
//    String SERVER_ADDRESS = "_APSARAS_SERVER_ADDRESS";

    String META_DO_NOT_TRACE = "_APS_DO_NOT_TRACE";
    String META_TRANSACTION_ID = "_APS_TRANS_ID";
    String META_SPAN_ID = "_APS_SPAN_ID";
    String META_PARENT_SPAN_ID = "_APS_PARENT_SPAN_ID";
    String META_PARENT_APPLICATION_NAME = "_APS_PARENT_APP_NAME";
    String META_PARENT_APPLICATION_TYPE = "_APS_PARENT_APP_TYPE";
    String META_FLAGS = "_APS_FLAGS";
    String SERVER_ADDRESS = "_APS_SERVER_ADDR";

}
