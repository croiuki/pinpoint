package com.navercorp.pinpoint.plugin.apsaras.interceptor;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.apsaras.ApsarasConstants;

import java.util.Map;

/**
 * @author chenjunhua
 */
public class ApsarasNettyProviderInterceptor extends SpanSimpleAroundInterceptor {

    public ApsarasNettyProviderInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, ApsarasNettyProviderInterceptor.class);
    }

    @Override
    protected Trace createTrace(Object target, Object[] args) {
        Map<String, Object> map = (Map) args[1];
        Map<String, String> attachments = (Map<String, String>) args[2];
        if (attachments == null || attachments.size() == 0) {
            return null;
        }

        // If this transaction is not traceable, mark as disabled.
        if (attachments.get(ApsarasConstants.META_DO_NOT_TRACE) != null) {
            return traceContext.disableSampling();
        }

        String transactionId = attachments.get(ApsarasConstants.META_TRANSACTION_ID);

        // If there's no trasanction id, a new trasaction begins here.
        // FIXME There seems to be cases where the invoke method is called after a span is already created.
        // We'll have to check if a trace object already exists and create a span event instead of a span in that case.
        if (transactionId == null) {
            return traceContext.newTraceObject();
        }

        // otherwise, continue tracing with given data.
        long parentSpanID = NumberUtils.parseLong(attachments.get(ApsarasConstants.META_PARENT_SPAN_ID), SpanId.NULL);
        long spanID = NumberUtils.parseLong(attachments.get(ApsarasConstants.META_SPAN_ID), SpanId.NULL);
        short flags = NumberUtils.parseShort(attachments.get(ApsarasConstants.META_FLAGS), (short) 0);
        TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);

        return traceContext.continueTraceObject(traceId);
    }


    @Override
    protected void doInBeforeTrace(SpanRecorder recorder, Object target, Object[] args) {
        Map<String, Object> map = (Map) args[1];
        Map<String, String> attachments = (Map<String, String>) args[2];
        if (attachments == null || attachments.size() == 0) {
            return;
        }

        // You have to record a service type within Server range.
        recorder.recordServiceType(ApsarasConstants.APSARAS_PROVIDER_SERVICE_TYPE);

        String ifName = (String) map.get("ifName");
        String methodName = (String) map.get("method");
        String version = (String) map.get("version");
        String clientAddress = (String) map.get("clientNodeIp");
        String serverAddress = attachments.get(ApsarasConstants.SERVER_ADDRESS);

        recorder.recordRpcName(generateRpcName(ifName, methodName, version));
        recorder.recordEndPoint(serverAddress);
        recorder.recordRemoteAddress(clientAddress);

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            String parentApplicationName = attachments.get(ApsarasConstants.META_PARENT_APPLICATION_NAME);
            if (parentApplicationName != null) {
                short parentApplicationType = NumberUtils.parseShort(attachments.get(ApsarasConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);
                recorder.recordAcceptorHost(serverAddress);
            }
        }
    }

    private String generateRpcName(String ifName, String methodName, String version) {
        StringBuilder rpcName = new StringBuilder();
        int index = ifName.lastIndexOf(".");
        if (index >= 0) {
            rpcName.append(ifName.substring(index + 1));
        }
        rpcName.append(".").append(methodName).append("(").append(version).append(")");
        return rpcName.toString();

    }

    @Override
    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        Map<String, Object> map = (Map) args[1];
        Map<String, String> attachments = (Map<String, String>) args[2];
        if (attachments == null || attachments.size() == 0) {
            return;
        }
        recorder.recordApi(methodDescriptor);
        // TODO: 暂时不纪录入参（性能考虑）
        if (throwable == null) {
            // TODO: 暂时不记录结果（性能考虑）
            // recorder.recordAttribute(ApsarasConstants.APSARAS_RESULT_ANNOTATION_KEY, result);
        } else {
            recorder.recordException(throwable);
        }
    }


}
