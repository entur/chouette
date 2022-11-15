package mobi.chouette.exchange.netexprofile.exporter.producer;

import org.testng.Assert;
import org.testng.annotations.Test;


public class NetexProducerUtilsTest {

    @Test
    public void testTranslateObjectId() {
        Assert.assertEquals(NetexProducerUtils.translateObjectId("XXX:YYY:ZZZ", "RRR"), "XXX:RRR:ZZZ");
    }


}
