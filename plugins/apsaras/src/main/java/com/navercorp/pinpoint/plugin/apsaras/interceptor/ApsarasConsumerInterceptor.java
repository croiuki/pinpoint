package com.navercorp.pinpoint.plugin.apsaras.interceptor;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor5;
import com.navercorp.pinpoint.plugin.apsaras.ApsarasConstants;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author chenjunhua
 */
public class ApsarasConsumerInterceptor implements AroundInterceptor5 {

    private final MethodDescriptor descriptor;
    private final TraceContext traceContext;

    public ApsarasConsumerInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        String serverIp = (String)arg0;
        String serverPort = (String)arg1;
        Map<String, String> attachments = (Map<String, String>)arg4;
        if (trace.canSampled()) {
            String serverAddress = serverIp + ":" + serverPort;
            SpanEventRecorder recorder = trace.traceBlockBegin();

            // RPC call trace have to be recorded with a service code in RPC client code range.
            recorder.recordServiceType(ApsarasConstants.APSARAS_CONSUMER_SERVICE_TYPE);

            // You have to issue a TraceId the receiver of this request will use.
            TraceId nextId = trace.getTraceId().getNextTraceId();

            // Then record it as next span id.
            recorder.recordNextSpanId(nextId.getSpanId());

            // Finally, pass some tracing data to the server.
            // How to put them in a message is protocol specific.
            // This example assumes that the target protocol message can include any metadata (like HTTP headers).
            attachments.put(ApsarasConstants.META_TRANSACTION_ID, nextId.getTransactionId());
            attachments.put(ApsarasConstants.META_SPAN_ID, Long.toString(nextId.getSpanId()));
            attachments.put(ApsarasConstants.META_PARENT_SPAN_ID, Long.toString(nextId.getParentSpanId()));
            attachments.put(ApsarasConstants.META_PARENT_APPLICATION_TYPE, Short.toString(traceContext.getServerTypeCode()));
            attachments.put(ApsarasConstants.META_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
            attachments.put(ApsarasConstants.META_FLAGS, Short.toString(nextId.getFlags()));

            attachments.put(ApsarasConstants.SERVER_ADDRESS, serverAddress);
        } else {
            // If sampling this transaction is disabled, pass only that infomation to the server.
            attachments.put(ApsarasConstants.META_DO_NOT_TRACE, "1");
        }
    }

    @Override
    public void after(Object target, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object result, Throwable throwable) {
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        String remoteIp = (String)arg0;
        String remotePort = (String)arg1;
        Method method = (Method)arg2;
        Object[] args = (Object[])arg3;
        Map<String, String> attachments = (Map<String, String>)arg4;

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();

            recorder.recordApi(descriptor);

            String endPoint = remoteIp + ":" + remotePort;
            // RPC client have to record end point (server address)
            recorder.recordEndPoint(endPoint);
            // Optionally, record the destination id (logical name of server. e.g. DB name)
            recorder.recordDestinationId(endPoint);

            if (throwable == null) {
                // recorder.recordAttribute(ApsarasConstants.APSARAS_ARGS_ANNOTATION_KEY, toJSONString(args));
                // recorder.recordAttribute(ApsarasConstants.APSARAS_RESULT_ANNOTATION_KEY, result);
            } else {
                recorder.recordException(throwable);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }

}
