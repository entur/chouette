package mobi.chouette.exchange.netexprofile.exporter.producer;

import static mobi.chouette.exchange.netexprofile.Constant.NETEX_REFERENTIAL;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static org.rutebanken.netex.model.VehicleModeEnumeration.AIR;
import static org.rutebanken.netex.model.VehicleModeEnumeration.BUS;
import static org.rutebanken.netex.model.VehicleModeEnumeration.CABLEWAY;
import static org.rutebanken.netex.model.VehicleModeEnumeration.COACH;
import static org.rutebanken.netex.model.VehicleModeEnumeration.FERRY;
import static org.rutebanken.netex.model.VehicleModeEnumeration.FUNICULAR;
import static org.rutebanken.netex.model.VehicleModeEnumeration.METRO;
import static org.rutebanken.netex.model.VehicleModeEnumeration.OTHER;
import static org.rutebanken.netex.model.VehicleModeEnumeration.RAIL;
import static org.rutebanken.netex.model.VehicleModeEnumeration.TRAM;
import static org.rutebanken.netex.model.VehicleModeEnumeration.TROLLEY_BUS;
import static org.rutebanken.netex.model.VehicleModeEnumeration.WATER;

import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.netex.model.ZoneRefStructure;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;

public class StopPlaceProducer extends NetexProducer implements NetexEntityProducer<StopPlace, StopArea> {

	private static final String DEFAULT_COORDINATE_SYSTEM = "WGS84";

	@Override
	public StopPlace produce(Context context, StopArea stopArea) {
		NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);
		StopPlace stopPlace = netexFactory.createStopPlace();

		NetexProducerUtils.populateId(stopArea, stopPlace);
		stopPlace.setName(ConversionUtil.getMultiLingualString(stopArea.getName()));
		stopPlace.setDescription(ConversionUtil.getMultiLingualString(stopArea.getComment()));

		if (isSet(stopArea.getRegistrationNumber())) {
			PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
			privateCodeStruct.setValue(stopArea.getRegistrationNumber());
			stopPlace.setPrivateCode(privateCodeStruct);
		}

		if (stopArea.hasCoordinates()) {
			SimplePoint_VersionStructure pointStruct = netexFactory.createSimplePoint_VersionStructure();
			LocationStructure locationStruct = netexFactory.createLocationStructure().withSrsName(DEFAULT_COORDINATE_SYSTEM);

			if (stopArea.hasCoordinates()) {
				locationStruct.setLatitude(stopArea.getLatitude());
				locationStruct.setLongitude(stopArea.getLongitude());
			}

			pointStruct.setLocation(locationStruct);
			stopPlace.setCentroid(pointStruct);
		}

		if(stopArea.getTransportModeName() != null) {
			stopPlace.setTransportMode(mapTransportMode(stopArea.getTransportModeName()));
		}
		if(stopArea.getTransportSubMode() != null) {
			mapTransportSubMode(stopPlace, stopArea.getTransportSubMode());
		}

		if (isSet(stopArea.getParent())) {
			ZoneRefStructure zoneRefStruct = netexFactory.createZoneRefStructure();
			NetexProducerUtils.populateReference(stopArea.getParent(), zoneRefStruct, true);
			stopPlace.setParentZoneRef(zoneRefStruct);
		}

		if (stopArea.getAreaType().equals(ChouetteAreaEnum.CommercialStopPoint) && CollectionUtils.isNotEmpty(stopArea.getContainedStopAreas())) {
			Quays_RelStructure quayStruct = netexFactory.createQuays_RelStructure();

			for (StopArea containedStopArea : stopArea.getContainedStopAreas()) {
				Quay quay = netexFactory.createQuay();
				NetexProducerUtils.populateId(containedStopArea, quay);

				quay.setName(ConversionUtil.getMultiLingualString(containedStopArea.getName()));
				quay.setDescription(ConversionUtil.getMultiLingualString(containedStopArea.getComment()));

				if (isSet(containedStopArea.getRegistrationNumber())) {
					PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
					privateCodeStruct.setValue(containedStopArea.getRegistrationNumber());
					quay.setPrivateCode(privateCodeStruct);
				}

				if (containedStopArea.hasCoordinates()) {
					SimplePoint_VersionStructure pointStruct = netexFactory.createSimplePoint_VersionStructure();
					LocationStructure locationStruct = netexFactory.createLocationStructure().withSrsName(DEFAULT_COORDINATE_SYSTEM);

					if (containedStopArea.hasCoordinates()) {
						locationStruct.setLatitude(containedStopArea.getLatitude());
						locationStruct.setLongitude(containedStopArea.getLongitude());
					}

					pointStruct.setLocation(locationStruct);
					quay.setCentroid(pointStruct);
				}

				quayStruct.getQuayRefOrQuay().add(quay);
			}

			stopPlace.setQuays(quayStruct);
		}

	
		return stopPlace;
	}

	private static VehicleModeEnumeration mapTransportMode(TransportModeNameEnum transportModeNameEnum) {
		if (transportModeNameEnum == null) {
			return null;
		}
		switch (transportModeNameEnum) {
			case Air:
				return AIR;
			case Bus:
				return BUS;
			case Rail:
				return RAIL;
			case Tram:
				return TRAM;
			case Coach:
				return COACH;
			case Ferry:
				return FERRY;
			case Metro:
				return METRO;
			case Water:
				return WATER;
			case Cableway:
				return CABLEWAY;
			case Funicular:
				return FUNICULAR;
			case TrolleyBus:
				return TROLLEY_BUS;
			case Other:
				return OTHER;
		}
		return OTHER;
	}

	private void mapTransportSubMode(StopPlace stopPlace, TransportSubModeNameEnum transportSubMode) {
		TransportSubmodeStructure transportSubmodeStructure = ConversionUtil.toTransportSubmodeStructure(transportSubMode);
		stopPlace.setAirSubmode(transportSubmodeStructure.getAirSubmode());
		stopPlace.setBusSubmode(transportSubmodeStructure.getBusSubmode());
		stopPlace.setCoachSubmode(transportSubmodeStructure.getCoachSubmode());
		stopPlace.setFunicularSubmode(transportSubmodeStructure.getFunicularSubmode());
		stopPlace.setMetroSubmode(transportSubmodeStructure.getMetroSubmode());
		stopPlace.setRailSubmode(transportSubmodeStructure.getRailSubmode());
		stopPlace.setSnowAndIceSubmode(transportSubmodeStructure.getSnowAndIceSubmode());
		stopPlace.setTelecabinSubmode(transportSubmodeStructure.getTelecabinSubmode());
		stopPlace.setTramSubmode(transportSubmodeStructure.getTramSubmode());
		stopPlace.setWaterSubmode(transportSubmodeStructure.getWaterSubmode());
	}

}
