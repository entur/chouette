package mobi.chouette.common;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JAXBUtil {

    private JAXBUtil() {}

    private static final Map<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>();

    public static JAXBContext getJAXBContext(Class<?> type) {
        return jaxbContexts.computeIfAbsent(type, JAXBUtil::newJAXBContext);
    }

    private static JAXBContext newJAXBContext(Class<?> clazz) {
        try {
            return JAXBContext.newInstance(clazz);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }


}
