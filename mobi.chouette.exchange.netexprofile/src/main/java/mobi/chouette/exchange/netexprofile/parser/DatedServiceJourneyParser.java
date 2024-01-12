package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.List;

@Log4j
public class DatedServiceJourneyParser extends NetexParser implements Parser, Constant {

    @Override
    @SuppressWarnings("unchecked")
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        JourneysInFrame_RelStructure journeyStructs = (JourneysInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
        List<Journey_VersionStructure> serviceJourneys = journeyStructs.getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney();

        for (Journey_VersionStructure journeyStruct : serviceJourneys) {
            if (journeyStruct instanceof DatedServiceJourney) {
                parseDatedServiceJourney(context, referential, (DatedServiceJourney) journeyStruct);
            } else {
                if(log.isTraceEnabled()) {
                    log.trace("Ignoring non-DatedServiceJourney with id: " + journeyStruct.getId());
                }
            }
        }
    }

    private void parseDatedServiceJourney(Context context, Referential referential, DatedServiceJourney netexDatedServiceJourney) {
        String datedServiceJourneyId = netexDatedServiceJourney.getId();
        if(log.isTraceEnabled()) {
            log.trace("Parsing DatedServiceJourney with id: " + datedServiceJourneyId);
        }
        mobi.chouette.model.DatedServiceJourney datedServiceJourney = ObjectFactory.getDatedServiceJourney(referential, datedServiceJourneyId);

        // operating day
        NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);
        String operatingDayRefId = netexDatedServiceJourney.getOperatingDayRef().getRef();
        OperatingDay operatingDay = NetexObjectUtil.getOperatingDay(netexReferential, operatingDayRefId);
        datedServiceJourney.setOperatingDay(TimeUtil.toLocalDateIgnoreTime(operatingDay.getCalendarDate()));

        // service journey
        JAXBElement<? extends JourneyRefStructure> journeyRef = netexDatedServiceJourney.getJourneyRef();
        VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, journeyRef.getValue().getRef());
        datedServiceJourney.setVehicleJourney(vehicleJourney);

        // dated service journeys replaced by this dated service journey
        if(netexDatedServiceJourney.getReplacedJourneys() != null) {
            for (JAXBElement<VehicleJourneyRefStructure> jaxbJourneyRefStructure : netexDatedServiceJourney.getReplacedJourneys().getDatedVehicleJourneyRefOrNormalDatedVehicleJourneyRef()) {
                VehicleJourneyRefStructure vehicleJourneyRefStructure = jaxbJourneyRefStructure.getValue();
                 mobi.chouette.model.DatedServiceJourney originalDatedServiceJourney = ObjectFactory.getDatedServiceJourney(referential, vehicleJourneyRefStructure.getRef());
                datedServiceJourney.addOriginalDatedServiceJourney(originalDatedServiceJourney);
            }
        }

        // service alteration
        if (netexDatedServiceJourney.getServiceAlteration() != null) {
            datedServiceJourney.setServiceAlteration(NetexParserUtils.toServiceAlterationEum(netexDatedServiceJourney.getServiceAlteration()));
        }
    }

    static {
        ParserFactory.register(DatedServiceJourneyParser.class.getName(), new ParserFactory() {
            private DatedServiceJourneyParser instance = new DatedServiceJourneyParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
