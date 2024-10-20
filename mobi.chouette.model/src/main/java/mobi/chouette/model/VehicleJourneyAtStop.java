package mobi.chouette.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import java.time.LocalTime;

/**
 * Chouette VehicleJourneyAtStop : passing time on stops
 * <p/>
 * Neptune mapping : VehicleJourneyAtStop <br/>
 * Gtfs mapping : StopTime <br/>
 */

@Entity
@Table(name = "vehicle_journey_at_stops", uniqueConstraints = @UniqueConstraint(columnNames = {
		"vehicle_journey_id", "stop_point_id" }, name = "index_vehicle_journey_at_stops_on_stop_point_id"))
@NoArgsConstructor
@ToString(callSuper=true, exclude = { "vehicleJourney" })
public class VehicleJourneyAtStop extends NeptuneIdentifiedObject implements JourneyAtStop{

	private static final long serialVersionUID = 194243517715939830L;

	@Getter
	@Setter
	@GenericGenerator(name = "vehicle_journey_at_stops_id_seq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", 
		parameters = {
			@Parameter(name = "sequence_name", value = "vehicle_journey_at_stops_id_seq"),
			@Parameter(name = "increment_size", value = "1000") })
	@GeneratedValue(generator = "vehicle_journey_at_stops_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;
	
	/**
	 * connecting Service Id
	 * 
	 * @param connectingServiceId
	 *            New value
	 * @return The actual value
	 */
	@Deprecated
	@Getter
	@Setter
	@Transient
	// @Column(name = "connecting_service_id")
	private String connectingServiceId;

	/**
	 * not saved, boarding alighting possibility
	 * 
	 * @param boardingAlightingPossibility
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
//	@Enumerated(EnumType.STRING)
	@Transient
//	@Column(name = "boarding_alighting_possibility")
	private BoardingAlightingPossibilityEnum boardingAlightingPossibility;

	/**
	 * arrival time
	 * 
	 * @param arrivalTime
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "arrival_time")
	private LocalTime arrivalTime;

	/**
	 * departure time
	 * 
	 * @param departureTime
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "departure_time")
	private LocalTime departureTime;

	
	/**
	 * departure day offset
	 * 
	 * @param departureDayOffset
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "departure_day_offset")
	private int departureDayOffset;
	
	
	/**
	 * arrival day offset
	 * 
	 * @param arrivalDayOffset
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "arrival_day_offset")
	private int arrivalDayOffset;
	
	/*
	 * waiting time
	 * 
	 * @param waitingTime
	 *            New value
	 * @return The actual value
	 */
	//@Deprecated
	//@Getter
	//@Setter
	//@Transient
	// @Column(name = "waiting_time")
	//private Time waitingTime;

	/*
	 * elapse duration <br/>
	 * for vehicle journey with time slots<br/>
	 * definition should change in next release
	 * 
	 * @param elapseDuration
	 *            New value
	 * @return The actual value
	 */
	//@Deprecated
	//@Getter
	//@Setter
	// @Column(name = "elapse_duration")
	//@Transient
	//private Time elapseDuration;

	/*
	 * headway frequnecy <br/>
	 * for vehicle journey with time slots<br/>
	 * field should move to vehicleJourney in next release
	 * 
	 * @param headwayFrequency
	 *            New value
	 * @return The actual value
	 */
	//@Deprecated
	//@Getter
	//@Setter
	//@Column(name = "headway_frequency")
	//@Transient
	//private Time headwayFrequency;

	/**
	 * vehicle journey reference <br/>
	 * 
	 * @return The actual value
	 */
	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_journey_id")
	private VehicleJourney vehicleJourney;

	/**
	 * set vehicle journey reference
	 * 
	 * @param vehicleJourney
	 */
	public void setVehicleJourney(VehicleJourney vehicleJourney) {
		if (this.vehicleJourney != null) {
			this.vehicleJourney.getVehicleJourneyAtStops().remove(this);
		}
		this.vehicleJourney = vehicleJourney;
		if (vehicleJourney != null) {
			vehicleJourney.getVehicleJourneyAtStops().add(this);
		}
	}

	/**
	 * stop point reference <br/>
	 * 
	 * @param stopPoint
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "stop_point_id")
	private StopPoint stopPoint;
	
	/**
	 * footnotes refs
	 * 
	 * @param footnotes
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToMany( cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "footnotes_vehicle_journey_at_stops", joinColumns = { @JoinColumn(name = "vehicle_journey_at_stop_id") }, inverseJoinColumns = { @JoinColumn(name = "footnote_id") })
	private List<Footnote> footnotes = new ArrayList<>(0);



}
