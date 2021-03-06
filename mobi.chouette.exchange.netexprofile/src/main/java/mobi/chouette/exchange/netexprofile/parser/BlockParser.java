package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.Block;
import org.rutebanken.netex.model.BlocksInFrame_RelStructure;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

import javax.xml.bind.JAXBElement;

@Log4j
public class BlockParser extends NetexParser implements Parser {

    @Override
    public void parse(Context context) {
        Referential referential = (Referential) context.get(REFERENTIAL);
        BlocksInFrame_RelStructure blocks = (BlocksInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
        for (DataManagedObjectStructure genericNetexBlock : blocks.getBlockOrCompoundBlockOrTrainBlock()) {
            if (genericNetexBlock instanceof Block) {
                parseBlock(referential, (Block) genericNetexBlock);
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Ignoring non-Block element with id: " + genericNetexBlock.getId());
                }
            }
        }
    }

    private void parseBlock(Referential referential, Block netexBlock) {
        String blockId = netexBlock.getId();
        if(log.isDebugEnabled()) {
            log.debug("Parsing Block with id: " + blockId);

        }
        mobi.chouette.model.Block chouetteBlock = ObjectFactory.getBlock(referential, netexBlock.getId());

        // private code
        chouetteBlock.setPrivateCode(netexBlock.getPrivateCode().getValue());

        // day types
        DayTypeRefs_RelStructure dayTypes = netexBlock.getDayTypes();
        if (dayTypes != null) {
            for (JAXBElement<? extends DayTypeRefStructure> dayType : dayTypes.getDayTypeRef()) {
                String timetableId = dayType.getValue().getRef();
                Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
                timetable.addBlock(chouetteBlock);
            }
        }

        // vehicle journey
        for (JAXBElement<?> jaxbJourneyRef : netexBlock.getJourneys().getJourneyRefOrJourneyDesignatorOrServiceDesignator()) {
            Object reference = jaxbJourneyRef.getValue();
            if (reference instanceof VehicleJourneyRefStructure) {
                VehicleJourneyRefStructure vehicleJourneyRefStructure = (VehicleJourneyRefStructure) reference;
                mobi.chouette.model.VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, vehicleJourneyRefStructure.getRef());
                chouetteBlock.addVehicleJourney(vehicleJourney);
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Ignoring non-VehicleJourneyRef element with id: " + reference);
                }

            }
        }
    }


    static {
        ParserFactory.register(BlockParser.class.getName(), new ParserFactory() {
            private BlockParser instance = new BlockParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
