package com.navercorp.pinpoint.plugin.apsaras;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * @author chenjunhua
 */
public class ApsarasPlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ApsarasConfiguration config = new ApsarasConfiguration(context.getConfig());
        if (!config.isApsarasEnabled()) {
            logger.info("ApsarasPlugin disabled");
            return;
        }

        this.addApplicationTypeDetector(context, config);
        this.addTransformers();
    }

    private void addTransformers() {
        transformTemplate.transform("com.dhgate.apsaras.access.BaseStackInvocation", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.getDeclaredMethod("invoke", "java.lang.String", "java.lang.String", "java.lang.reflect.Method",
                        "java.lang.Object[]", "java.util.Map")
                        .addInterceptor("com.navercorp.pinpoint.plugin.apsaras.interceptor.ApsarasConsumerInterceptor");
                return target.toBytecode();
            }
        });
        transformTemplate.transform("com.dhgate.apsaras.rpc.netty.ServerHandler", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.getDeclaredMethod("invokeService", "com.dhgate.apsaras.rpc.common.RpcInvoker", "java.util.Map", "java.util.Map")
                        .addInterceptor("com.navercorp.pinpoint.plugin.apsaras.interceptor.ApsarasNettyProviderInterceptor");
                return target.toBytecode();
            }
        });
        transformTemplate.transform("com.dhgate.apsaras.rpc.jbossremoting.JBossRemotingRPCServer", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.getDeclaredMethod("invokeService", "java.lang.reflect.Method", "java.lang.Object", "java.lang.Object[]", "java.util.Map", "java.util.Map")
                        .addInterceptor("com.navercorp.pinpoint.plugin.apsaras.interceptor.ApsarasJBossProviderInterceptor");
                return target.toBytecode();
            }
        });
    }

    /**
     * Pinpoint profiler agent uses this detector to find out the service type of current application.
     */
    private void addApplicationTypeDetector(ProfilerPluginSetupContext context, ApsarasConfiguration config) {
        context.addApplicationTypeDetector(new ApsarasProviderDetector());
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
