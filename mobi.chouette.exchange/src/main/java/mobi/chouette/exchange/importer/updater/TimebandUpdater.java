package mobi.chouette.exchange.importer.updater;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import mobi.chouette.common.Context;
import mobi.chouette.dao.TimebandDAO;
import mobi.chouette.model.Timeband;

import java.time.format.DateTimeFormatter;

@Stateless(name = TimebandUpdater.BEAN_NAME)
public class TimebandUpdater implements Updater<Timeband> {

	public static final String BEAN_NAME = "TimebandUpdater";
	public static final DateTimeFormatter DATE_TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	@EJB 
	private TimebandDAO timebandDAO;

	@Override
	public void update(Context context, Timeband oldValue, Timeband newValue) throws Exception {
		if (newValue.isSaved()) {
			return;
		}
		newValue.setSaved(true);

		if (newValue.getObjectId() != null
				&& !newValue.getObjectId().equals(oldValue.getObjectId())) {
			oldValue.setObjectId(newValue.getObjectId());
		}
		if (newValue.getObjectVersion() != null
				&& !newValue.getObjectVersion().equals(
						oldValue.getObjectVersion())) {
			oldValue.setObjectVersion(newValue.getObjectVersion());
		}
		if (newValue.getCreationTime() != null
				&& !newValue.getCreationTime().equals(
						oldValue.getCreationTime())) {
			oldValue.setCreationTime(newValue.getCreationTime());
		}
		if (newValue.getCreatorId() != null
				&& !newValue.getCreatorId().equals(oldValue.getCreatorId())) {
			oldValue.setCreatorId(newValue.getCreatorId());
		}
		if (newValue.getName() != null
				&& !newValue.getName().equals(oldValue.getName())) {
			oldValue.setName(newValue.getName());
		}
		if (newValue.getStartTime() != null
				&& !newValue.getStartTime().equals(oldValue.getStartTime())) {
			oldValue.setStartTime(newValue.getStartTime());
		}
		if (newValue.getEndTime() != null
				&& !newValue.getEndTime().equals(oldValue.getEndTime())) {
			oldValue.setEndTime(newValue.getEndTime());
		}
		if (oldValue.getName() == null || oldValue.getName().isEmpty()) {
			oldValue.setName(DATE_TIME_FORMATTER_HH_MM.format(oldValue.getStartTime())+" - "+ DATE_TIME_FORMATTER_HH_MM.format(oldValue.getEndTime()));
		}
		if (timebandDAO.findByObjectId(oldValue.getObjectId()) == null)
			timebandDAO.create(oldValue);
		else
			timebandDAO.update(oldValue);
	}
}
