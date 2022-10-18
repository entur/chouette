package mobi.chouette.common.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import java.util.concurrent.Callable;

public class CommandTimer {


    private final MetricRegistry applicationRegistry;
    private final String metricName;
    private final String metricDescription;

    public CommandTimer(MetricRegistry applicationRegistry, String metricName, String metricDescription) {
        this.applicationRegistry = applicationRegistry;
        this.metricName = metricName;
        this.metricDescription = metricDescription;
    }

    public boolean timed(Callable<Boolean> command, String referential) throws Exception {
        Tag tag =  new Tag("referential", referential);
        SimpleTimer timer = applicationRegistry.simpleTimer(Metadata.builder()
                .withName(metricName)
                .withDescription(metricDescription)
                .build(), tag);
        return timer.time(command);
    }
}
