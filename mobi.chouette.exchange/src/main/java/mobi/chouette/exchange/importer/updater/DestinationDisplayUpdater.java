package mobi.chouette.exchange.importer.updater;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.dao.DestinationDisplayDAO;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

@Stateless(name = DestinationDisplayUpdater.BEAN_NAME)
@Log4j
public class DestinationDisplayUpdater implements Updater<DestinationDisplay> {

	public static final String BEAN_NAME = "DestinationDisplayUpdater";

	@EJB
	private DestinationDisplayDAO destinationDisplayDAO;

	@Override
	public void update(Context context, DestinationDisplay oldValue, DestinationDisplay newValue) {

		Referential cache = (Referential) context.get(CACHE);

		if (log.isTraceEnabled()) {
			log.trace("Updating " + oldValue + " with " + newValue);

		}
		if (newValue.getName() != null
				&& !newValue.getName().equals(oldValue.getName())) {
			oldValue.setName(newValue.getName());
		}

		if (newValue.getFrontText() != null
				&& !newValue.getFrontText().equals(oldValue.getFrontText())) {
			oldValue.setFrontText(newValue.getFrontText());
		}

		if (newValue.getSideText() != null
				&& !newValue.getSideText().equals(oldValue.getSideText())) {
			oldValue.setSideText(newValue.getSideText());
		}

		// Handle vias
		if(!newValue.getVias().isEmpty()) {
			oldValue.getVias().clear();
			for(DestinationDisplay via : newValue.getVias()) {
				String objectId = via.getObjectId();
				DestinationDisplay destinationDisplay = cache.getDestinationDisplays().get(objectId);
				if (destinationDisplay == null) {
					destinationDisplay = destinationDisplayDAO.findByObjectId(objectId);
					if (destinationDisplay != null) {
						cache.getDestinationDisplays().put(objectId, destinationDisplay);
					}
				}
				if (destinationDisplay == null) {
					destinationDisplay = ObjectFactory.getDestinationDisplay(cache, objectId);
					destinationDisplay.setName(via.getName());
					destinationDisplay.setSideText(via.getSideText());
					destinationDisplay.setFrontText(via.getFrontText());
				}
				oldValue.getVias().add(destinationDisplay);
			}
		}



		
	}

}
