package org.example.function;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.function.adapter.aws.FunctionInvoker;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.tracing.Tracing;

public class PowertoolsFunctionInvoker extends FunctionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(PowertoolsFunctionInvoker.class);
    private static final Metrics METRICS = MetricsFactory.getMetricsInstance();

    @Override
    @Logging(clearState = true)
    @Tracing
    @FlushMetrics(captureColdStart = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        METRICS.addMetric("Invocation", 1, MetricUnit.COUNT);

        try {
            super.handleRequest(input, output, context);
            METRICS.addMetric("InvocationSuccess", 1, MetricUnit.COUNT);
        } catch (IOException e) {
            METRICS.addMetric("InvocationError", 1, MetricUnit.COUNT);
            LOG.error("Invocation failed", e);
            throw e;
        } catch (RuntimeException e) {
            METRICS.addMetric("InvocationError", 1, MetricUnit.COUNT);
            LOG.error("Invocation failed", e);
            throw e;
        } catch (Exception e) {
            METRICS.addMetric("InvocationError", 1, MetricUnit.COUNT);
            LOG.error("Invocation failed", e);
            throw new RuntimeException(e);
        }
    }
}
