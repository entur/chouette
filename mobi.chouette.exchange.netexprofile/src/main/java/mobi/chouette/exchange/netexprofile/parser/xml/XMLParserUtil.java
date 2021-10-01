package mobi.chouette.exchange.netexprofile.parser.xml;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;

public final class XMLParserUtil {

    private XMLParserUtil() {
    }

    public static XMLInputFactory getXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

}
