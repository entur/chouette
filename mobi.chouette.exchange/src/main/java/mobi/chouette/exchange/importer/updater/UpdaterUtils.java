package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.NeptuneIdentifiedObject;

import java.util.Collection;

public class UpdaterUtils {

    public static Collection<String> getObjectIds(Collection<? extends NeptuneIdentifiedObject> list) {
        return list.stream().map(NeptuneIdentifiedObject::getObjectId).toList();
    }
}
