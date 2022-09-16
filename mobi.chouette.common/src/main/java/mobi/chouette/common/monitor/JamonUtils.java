package mobi.chouette.common.monitor;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import mobi.chouette.common.Color;
import org.apache.log4j.Logger;

public final class JamonUtils {

    private  JamonUtils() {}

    public static void logYellow(Logger log, Monitor monitor) {
        logColor(log, monitor, Color.YELLOW);
    }

    public static void logMagenta(Logger log, Monitor monitor) {
        logColor(log, monitor, Color.MAGENTA);
    }

    public static void logCyan(Logger log, Monitor monitor) {
        logColor(log, monitor, Color.CYAN);
    }

    public static void logBlue(Logger log, Monitor monitor) {
        logColor(log, monitor, Color.BLUE);
    }

    private static void logColor(Logger log, Monitor monitor, String color) {
        if (MonitorFactory.isEnabled()) {
            log.info(color + monitor.stop() + Color.NORMAL);
        }
    }
}
