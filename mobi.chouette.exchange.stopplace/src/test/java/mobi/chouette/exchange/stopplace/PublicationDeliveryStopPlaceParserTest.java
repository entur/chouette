package mobi.chouette.exchange.stopplace;

import com.google.common.collect.Sets;

import mobi.chouette.model.StopArea;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PublicationDeliveryStopPlaceParserTest {

    @Test
    public void testParseStopPlacesFromPublicationDelivery() throws Exception {
        Set<String> expectedActiveIds = Sets.newHashSet("NSR:StopPlace:51566", "NSR:StopPlace:11001");
        Set<String> expectedRemovedIds = Sets.newHashSet("NSR:StopPlace:10089");
        PublicationDeliveryStopPlaceParser parser = new PublicationDeliveryStopPlaceParser(new FileInputStream("src/test/resources/netex/PublicationDeliveryWithStopPlaces.xml"));

        Collection<StopArea> activeStopAreas = parser.getUpdateContext().getActiveStopAreas();
        Assert.assertEquals(activeStopAreas.size(), expectedActiveIds.size());
        Assert.assertTrue(activeStopAreas.stream().allMatch(sa -> expectedActiveIds.remove(sa.getObjectId())));

        Collection<String> inactiveStopAreas = parser.getUpdateContext().getInactiveStopAreaIds();
        Assert.assertEquals(inactiveStopAreas.size(), expectedRemovedIds.size());
        Assert.assertTrue(inactiveStopAreas.stream().allMatch(id -> expectedRemovedIds.remove(id)));


    }
}
