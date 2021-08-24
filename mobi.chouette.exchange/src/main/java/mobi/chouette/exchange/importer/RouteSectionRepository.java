package mobi.chouette.exchange.importer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.GeometryUtil;
import mobi.chouette.exchange.importer.geometry.RouteSectionGenerator;
import mobi.chouette.exchange.importer.geometry.osrm.OsrmRouteSectionId;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectIdTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate route sections between two points thanks to the external OSRM service.
 * LineStrings generated by OSRM for a given set of coordinates and transport mode are cached.
 * RouteSection objects that have identical (start point, stop point, lineString) are reused.
 */

@Log4j
public class RouteSectionRepository {

    private static final int DEFAULT_MAX_METERS_FROM_QUAY = 100;

    private final RouteSectionGenerator routeSectionGenerator;
    private final Map<OsrmRouteSectionId, LineString> lineStringCache;
    private final Map<String, RouteSection> routeSectionCache;
    private final Integer maxMetersFromQuay;


    public RouteSectionRepository(RouteSectionGenerator routeSectionGenerator) {
        this.routeSectionGenerator = routeSectionGenerator;
        this.routeSectionCache = new HashMap<>();
        this.lineStringCache = new HashMap<>();

        String maxAsString = System.getProperty("iev.route.section.generate.quay.distance.max.meters");
        if (maxAsString != null) {
            this.maxMetersFromQuay = Integer.valueOf(maxAsString);
            log.info("Using configured value for iev.route.section.generate.quay.distance.max.meters: " + maxMetersFromQuay);
        } else {
            this.maxMetersFromQuay = DEFAULT_MAX_METERS_FROM_QUAY;
            log.info("No value configured iev.route.section.generate.quay.distance.max.meters, using default: " + maxMetersFromQuay);
        }

    }


    /**
     * Generate a RouteSection between two points for a given transport mode.
     * The LineStrings between two points are calculated by the OSRM service.
     * LineStrings are cached so that the OSRM service is called only once for a given triple (start point, end point, transport mode)
     * LineStrings returned by OSRM are discarded if they are too far from the start point/ end point of the section
     *
     * @param jp
     * @param fromStopPoint
     * @param toStopPoint
     * @param transportMode
     * @return
     */
    public RouteSection getRouteSection(JourneyPattern jp, StopPoint fromStopPoint, StopPoint toStopPoint, TransportModeNameEnum transportMode) {
        {
            Coordinate from = getCoordinateFromStopPoint(fromStopPoint);
            Coordinate to = getCoordinateFromStopPoint(toStopPoint);
            LineString lineString = null;
            if (from != null && to != null) {
                lineString = lineStringCache.computeIfAbsent(new OsrmRouteSectionId(from, to, transportMode), routeSectionGenerator::getRouteSection);
                if (!isLineStringGoodMatchForQuays(lineString, from, to)) {
                    log.info("Ignoring generated LineString because it is to far from stop at start and/or end of section." +
                            "JP: " + jp.getObjectId() + ", From: " + fromStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() +
                            ", to: " + toStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() + ", transportMode: " + transportMode);
                    lineString = null;
                }
            }
            return createRouteSection(fromStopPoint, toStopPoint, lineString);
        }
    }

    /**
     * Create a RouteSection between two points.
     * RouteSections are stored in a map for reuse.
     * RouteSections are uniquely identified by their start point, end point and the hashcode of their LineString.
     * RouteSections may have an empty LineString if the LineString generated by OSRM is discarded.
     *
     * @param from
     * @param to
     * @param lineString
     * @return
     */
    private RouteSection createRouteSection(StopPoint from, StopPoint to, LineString lineString) {

        String fromScheduledStopPointId = from.getScheduledStopPoint().getObjectId();
        String toScheduledStopPointId = to.getScheduledStopPoint().getObjectId();
        String uniqueRouteSectionId = fromScheduledStopPointId.substring(fromScheduledStopPointId.lastIndexOf(':') + 1) + '_' + toScheduledStopPointId.substring(toScheduledStopPointId.lastIndexOf(':') + 1) + '_' + (lineString == null ? "NULL" : String.valueOf(lineString.hashCode()));

        return routeSectionCache.computeIfAbsent(uniqueRouteSectionId, s -> {
            RouteSection routeSection = new RouteSection();
            routeSection.setObjectId(from.objectIdPrefix() + ":" + ObjectIdTypes.ROUTE_SECTION_KEY + ":" + uniqueRouteSectionId);
            routeSection.setFromScheduledStopPoint(from.getScheduledStopPoint());
            routeSection.setToScheduledStopPoint(to.getScheduledStopPoint());
            routeSection.setInputGeometry(lineString);
            routeSection.setProcessedGeometry(lineString);
            routeSection.setNoProcessing(true);
            routeSection.setFilled(true);
            routeSection.setDetached(true);
            if (lineString != null) {
                routeSection.setDistance(BigDecimal.valueOf(GeometryUtil.convertFromAngleDegreesToMeters(lineString.getLength())));
            }
            return routeSection;
        });
    }


    protected boolean isLineStringGoodMatchForQuays(LineString lineString, Coordinate from, Coordinate to) {

        if (lineString != null && lineString.getCoordinates() != null && lineString.getCoordinates().length > 0) {

            Coordinate lineStart = lineString.getCoordinates()[0];
            Coordinate lineEnd = lineString.getCoordinates()[lineString.getCoordinates().length - 1];

            double distanceFromStart = GeometryUtil.calculateDistanceInMeters(from.x, from.y, lineStart.x, lineStart.y);
            double distanceFromEnd = GeometryUtil.calculateDistanceInMeters(to.x, to.y, lineEnd.x, lineEnd.y);

            if (distanceFromStart > maxMetersFromQuay || distanceFromEnd > maxMetersFromQuay) {
                return false;
            }
        }
        return true;
    }

    private Coordinate getCoordinateFromStopPoint(StopPoint stopPoint) {
        if (stopPoint == null || stopPoint.getScheduledStopPoint() == null || stopPoint.getScheduledStopPoint().getContainedInStopAreaRef() == null) {
            return null;
        }
        StopArea stopArea = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
        if (stopArea == null || stopArea.getLongitude() == null || stopArea.getLatitude() == null) {
            return null;
        }
        return new Coordinate(stopArea.getLongitude().doubleValue(), stopArea.getLatitude().doubleValue());
    }
}
