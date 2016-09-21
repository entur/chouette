package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.ChouetteAreaEnum;
import no.rutebanken.netex.model.BoardingPosition;
import no.rutebanken.netex.model.Quay;
import no.rutebanken.netex.model.StopPlace;
import org.apache.velocity.runtime.directive.Stop;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.*;

public class StopPlaceMapperTest {

    private StopPlaceMapper stopPlaceMapper = new StopPlaceMapper();

    @Test
    public void stopPlaceWithThreeBoardingPositions() {
        StopArea stopPlace = createStopPlace("Moensletta");

        StopArea firstBoardingPosition = createBoardingPosition(stopPlace.getName());
        StopArea secondBoardingPosition = createBoardingPosition(stopPlace.getName());
        StopArea thirdBoardingPosition = createBoardingPosition(stopPlace.getName());
        stopPlace.setContainedStopAreas(Arrays.asList(firstBoardingPosition, secondBoardingPosition, thirdBoardingPosition));

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), stopPlace.getName());

        assertNotNull(netexStopPlace.getQuays(), "Quays shall not be null.");
        assertEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 3);
        Quay firstQuay = (Quay) netexStopPlace.getQuays().getQuayRefOrQuay().get(0);
        assertEquals(firstQuay.getName().getValue(), stopPlace.getName());
    }

    @Test
    public void boardingPositionWithParentStopPlace() {
        StopArea boardingPosition = createBoardingPosition("boarding position 1");

        StopArea parentStopArea = createStopPlace("parent stop place");

        boardingPosition.setParent(parentStopArea);

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);
        assertEquals(netexStopPlace.getName().getValue(), parentStopArea.getName());

        assertNotNull(netexStopPlace.getQuays());
        assertNotEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 0);
        assertEquals(((Quay) netexStopPlace.getQuays().getQuayRefOrQuay().get(0)).getName().getValue(), boardingPosition.getName());

    }

    /**
     * A single boarding position must be mapped to quay with a parent stop place.
     */
    @Test
    public void boardingPositionWithoutParentStopPlace() {
        StopArea boardingPosition = createBoardingPosition("boarding position");

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);

        assertEquals(netexStopPlace.getName().getValue(), boardingPosition.getName());

        assertNotNull(netexStopPlace.getQuays());
        assertEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 1);
        assertEquals(((Quay) netexStopPlace.getQuays().getQuayRefOrQuay().get(0)).getName().getValue(), boardingPosition.getName());

    }


    @Test
    public void stopPlaceWithoutBoardingPositions() {
        StopArea stopPlace = createStopPlace("Klavestadhaugen");

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), stopPlace.getName());
    }

    @Test
    public void boardingPositionWithParentCommercialStopPoint() {
        StopArea commercialStopPoint = createStopArea("Otto Blehrs vei", ChouetteAreaEnum.CommercialStopPoint);
        StopArea boardingPosition = createBoardingPosition("Otto Blehrs vei");

        boardingPosition.setParent(commercialStopPoint);

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), commercialStopPoint.getName());
        assertNotNull(netexStopPlace.getQuays());
        assertNotNull(netexStopPlace.getQuays().getQuayRefOrQuay());
        assertEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 1);
    }


    @Test
    public void stopPlaceWithId() {
        StopArea stopPlace = createStopPlace("Hestehaugveien");
        stopPlace.setObjectId("id");
        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertEquals(netexStopPlace.getId(), String.valueOf(stopPlace.getObjectId()));
    }

    @Test
    public void boardingPositionWithoutStopPlace() {
        StopArea boardingPosition = createBoardingPosition("Borgen");
        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), boardingPosition.getName());
    }

    private StopArea createStopPlace(String name) {
        return createStopArea(name, ChouetteAreaEnum.StopPlace);
    }

    private StopArea createStopArea(String name, ChouetteAreaEnum chouetteAreaEnum) {
        StopArea stopPlace = new StopArea();
        stopPlace.setName(name);
        stopPlace.setAreaType(chouetteAreaEnum);
        return stopPlace;
    }


    private StopArea createBoardingPosition(String name) {
        StopArea boardingPosition = new StopArea();
        boardingPosition.setName(name);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
        return boardingPosition;
    }

}