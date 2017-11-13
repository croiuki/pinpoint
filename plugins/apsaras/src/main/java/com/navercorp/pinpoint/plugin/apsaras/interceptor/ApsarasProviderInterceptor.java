package com.navercorp.pinpoint.plugin.apsaras.interceptor;

import com.dhgate.apsaras.protocol.PayloadMessage;
import com.dhgate.apsaras.protocol.ProtocolPack;
import com.dhgate.apsaras.rpc.context.RpcAttachment;
import com.dhgate.apsaras.serialize.SerializerContext;
import com.dhgate.apsaras.util.ApsarasProperties;
import com.dhgate.apsaras.util.RequestUtil;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.apsaras.ApsarasConstants;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author chenjunhua
 */
public class ApsarasProviderInterceptor extends SpanSimpleAroundInterceptor {

    public ApsarasProviderInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, ApsarasProviderInterceptor.class);
    }

    @Override
    protected Trace createTrace(Object target, Object[] args) {
        Map<String, Object> map = (Map) args[1];
        RpcAttachment rpcAttachment = (RpcAttachment) args[2];
        if (map == null || rpcAttachment == null) {
            return null;
        }
        Map<String, String> attachments = rpcAttachment.getAttachments();
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
        RpcAttachment rpcAttachment = (RpcAttachment) args[2];
        if (map == null || rpcAttachment == null) {
            return;
        }
        Map<String, String> attachments = rpcAttachment.getAttachments();
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
        String rpcName = "";
        int index = ifName.lastIndexOf(".");
        if (index >= 0) {
            rpcName += ifName.substring(index + 1);
        }
        rpcName += "." + methodName + "()";
        return rpcName;

    }

    @Override
    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        Map<String, Object> map = (Map) args[1];
        RpcAttachment rpcAttachment = (RpcAttachment) args[2];
        if (map == null || rpcAttachment == null) {
            return;
        }
        Map<String, String> attachments = rpcAttachment.getAttachments();
        if (attachments == null || attachments.size() == 0) {
            return;
        }

        recorder.recordApi(methodDescriptor);
        // TODO:
        // recorder.recordAttribute(ApsarasConstants.APSARAS_ARGS_ANNOTATION_KEY, "abc");

        if (throwable == null) {
            recorder.recordAttribute(ApsarasConstants.APSARAS_RESULT_ANNOTATION_KEY, result);
        } else {
            recorder.recordException(throwable);
        }
    }


//    @Override
//    protected Trace createTrace(Object target, Object[] args) {
//        ProtocolPack msg = (ProtocolPack) args[1];
//        if (msg == null) {
//            return null;
//        }
//        RpcAttachment rpcAttachment = msg.getRpcAttachment();
//        if (rpcAttachment == null) {
//            return null;
//        }
//        Map<String, String> attachments = rpcAttachment.getAttachments();
//        if (attachments == null || attachments.size() == 0) {
//            return null;
//        }
//
//        // If this transaction is not traceable, mark as disabled.
//        if (attachments.get(ApsarasConstants.META_DO_NOT_TRACE) != null) {
//            return traceContext.disableSampling();
//        }
//
//        String transactionId = attachments.get(ApsarasConstants.META_TRANSACTION_ID);
//
//        // If there's no trasanction id, a new trasaction begins here.
//        // FIXME There seems to be cases where the invoke method is called after a span is already created.
//        // We'll have to check if a trace object already exists and create a span event instead of a span in that case.
//        if (transactionId == null) {
//            return traceContext.newTraceObject();
//        }
//
//        // otherwise, continue tracing with given data.
//        long parentSpanID = NumberUtils.parseLong(attachments.get(ApsarasConstants.META_PARENT_SPAN_ID), SpanId.NULL);
//        long spanID = NumberUtils.parseLong(attachments.get(ApsarasConstants.META_SPAN_ID), SpanId.NULL);
//        short flags = NumberUtils.parseShort(attachments.get(ApsarasConstants.META_FLAGS), (short) 0);
//        TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);
//
//        return traceContext.continueTraceObject(traceId);
//    }
//
//
//    @Override
//    protected void doInBeforeTrace(SpanRecorder recorder, Object target, Object[] args) {
//        if (!(args[1] instanceof  ProtocolPack)) {
//            return;
//        }
//        ProtocolPack pack = (ProtocolPack) args[1];
//        RpcAttachment rpcAttachment = pack.getRpcAttachment();
//        if (rpcAttachment == null) {
//            return;
//        }
//        Map<String, String> attachments = rpcAttachment.getAttachments();
//        if (attachments == null || attachments.size() == 0) {
//            return;
//        }
//
//        // You have to record a service type within Server range.
//        recorder.recordServiceType(ApsarasConstants.APSARAS_PROVIDER_SERVICE_TYPE);
//
//        // Record rpc name, client address, server address.
//        List<ByteBuffer> datas = pack.getDatas();
//        PayloadMessage payloadMsg = new PayloadMessage();
//        SerializerContext context = new SerializerContext();
//        payloadMsg = payloadMsg.deserialize(datas.get(0), context);
//
//        String clientIP = payloadMsg.getIp();
//
//        String ifName = payloadMsg.getIfName();
//        String version = payloadMsg.getVersion();
//        String methodName = payloadMsg.getMethod();
//
//
//        // recorder.recordRpcName(appName + ":" + ifName + ":" + methodName + ":" + version);
//        recorder.recordRpcName(ifName + ":" + methodName + ":" + version);
//
//        String serverAddress = attachments.get(ApsarasConstants.SERVER_ADDRESS);
//        recorder.recordEndPoint(serverAddress);
//
//        String clientAddress = clientIP;
//        recorder.recordRemoteAddress(clientAddress);
//
//        // important: reset data for next reading
//        for (ByteBuffer data : datas) {
//            data.flip();
//        }
//
//        // If this transaction did not begin here, record parent(client who sent this request) information
//        if (!recorder.isRoot()) {
//            String parentApplicationName = attachments.get(ApsarasConstants.META_PARENT_APPLICATION_NAME);
//
//            if (parentApplicationName != null) {
//                short parentApplicationType = NumberUtils.parseShort(attachments.get(ApsarasConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
//                recorder.recordParentApplication(parentApplicationName, parentApplicationType);
//
//                // Pinpoint finds caller - callee relation by matching caller's end point and callee's acceptor host.
//                // https://github.com/naver/pinpoint/issues/1395
//                recorder.recordAcceptorHost(serverAddress);
//            }
//        }
//    }
//
//    @Override
//    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
//        if (!(args[1] instanceof  ProtocolPack)) {
//            return;
//        }
//        ProtocolPack pack = (ProtocolPack) args[1];
//        RpcAttachment rpcAttachment = pack.getRpcAttachment();
//        if (rpcAttachment == null) {
//            return;
//        }
//        Map<String, String> attachments = rpcAttachment.getAttachments();
//        if (attachments == null || attachments.size() == 0) {
//            return;
//        }
//
//        recorder.recordApi(methodDescriptor);
//        //recorder.recordAttribute(ApsarasConstants.APSARAS_ARGS_ANNOTATION_KEY, rpcAttachment.getArguments());
//
//        if (throwable == null) {
//            recorder.recordAttribute(ApsarasConstants.APSARAS_RESULT_ANNOTATION_KEY, result);
//        } else {
//            recorder.recordException(throwable);
//        }
//    }
}
