package mobi.chouette.ws.startup;

import com.jamonapi.MonitorFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ChouetteJamonServletContextListener implements ServletContextListener {


    public ChouetteJamonServletContextListener() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MonitorFactory.setEnabled(isJamonEnabled());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private boolean isJamonEnabled() {
        String prop = System.getenv("IEV_JAMON_ENABLED");
        return Boolean.parseBoolean(prop);
    }

}
