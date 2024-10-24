package mobi.chouette.exchange.stopplace;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JAXBUtil;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.parser.StopPlaceParser;
import mobi.chouette.model.util.Referential;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;

import static mobi.chouette.exchange.netexprofile.Constant.NETEX_LINE_DATA_CONTEXT;

@Log4j
public class PublicationDeliveryStopPlaceParser {
    private static final String IMPORT_ID_KEY = "imported-id";
    private static final String MERGED_ID_KEY = "merged-id";
    private static final String ID_VALUE_SEPARATOR = ",";
    private InputStream inputStream;
    private Instant now;

    @Getter
    private StopAreaUpdateContext updateContext;

    public PublicationDeliveryStopPlaceParser(InputStream inputStream) {
        this.inputStream = inputStream;
        now = Instant.now();
        updateContext = new StopAreaUpdateContext();
        parseStopPlaces();
    }


    public void parseStopPlaces() {
        try {
            PublicationDeliveryStructure incomingPublicationDelivery = unmarshal(inputStream);

            convertToStopAreas(incomingPublicationDelivery);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshall delivery publication structure: " + e.getMessage(), e);
        }
    }

    private void convertToStopAreas(PublicationDeliveryStructure incomingPublicationDelivery) throws Exception {

        Context context = new Context();
        Referential referential = new Referential();
        context.put(Constant.REFERENTIAL, referential);
        StopPlaceParser stopPlaceParser = (StopPlaceParser) ParserFactory.create(StopPlaceParser.class.getName());

        for (JAXBElement<? extends Common_VersionFrameStructure> frameStructureElmt : incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame()) {
            Common_VersionFrameStructure frameStructure = frameStructureElmt.getValue();

            if (frameStructure instanceof Site_VersionFrameStructure) {
                Site_VersionFrameStructure siteFrame = (Site_VersionFrameStructure) frameStructure;

                if (siteFrame.getStopPlaces() != null) {

                    if (siteFrame.getTariffZones() != null) {
                        context.put(NETEX_LINE_DATA_CONTEXT, siteFrame.getTariffZones());
                        stopPlaceParser.parse(context);
                    }

                    context.put(NETEX_LINE_DATA_CONTEXT, siteFrame.getStopPlaces());
                    stopPlaceParser.parse(context);


                    for (JAXBElement<? extends Site_VersionStructure> jaxbStopPlace : siteFrame.getStopPlaces().getStopPlace_()) {

                        StopPlace stopPlace = (StopPlace) jaxbStopPlace.getValue();

                        if (!isActive(stopPlace, now)) {
                            updateContext.getInactiveStopAreaIds().add(stopPlace.getId());
                            referential.getStopAreas().remove(stopPlace.getId());
                        } 
                    }

                }
            }
        }

        updateContext.getActiveStopAreas().addAll(referential.getStopAreas().values().stream().filter(sa -> sa.getParent() == null).collect(Collectors.toSet()));
    }



    private boolean isActive(StopPlace stopPlace, Instant atTime) {
        if (CollectionUtils.isEmpty(stopPlace.getValidBetween()) || stopPlace.getValidBetween().get(0) == null) {
            return true;
        }
        LocalDateTime validTo = stopPlace.getValidBetween().get(0).getToDate();

        return validTo == null || validTo.atZone(ZoneId.systemDefault()).toInstant().isAfter(atTime);
    }

    private PublicationDeliveryStructure unmarshal(InputStream inputStream) throws JAXBException {
        Unmarshaller jaxbUnmarshaller = JAXBUtil.getJAXBContext(PublicationDeliveryStructure.class).createUnmarshaller();

        JAXBElement<PublicationDeliveryStructure> jaxbElement = jaxbUnmarshaller.unmarshal(new StreamSource(inputStream), PublicationDeliveryStructure.class);
        PublicationDeliveryStructure publicationDeliveryStructure = jaxbElement.getValue();

        return publicationDeliveryStructure;

    }

}
