package mobi.chouette.model.statistics;

import java.time.LocalDate;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import mobi.chouette.model.util.DateAdapter;

@XmlRootElement(name = "period")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "from", "to" })
@Getter
public class Period implements Comparable<Period> {
	// Use sql date for reliable serialization of date only (Should have been java.time.LocalDate)


	@XmlJavaTypeAdapter(DateAdapter.class)
	private java.sql.Date from;
	@XmlJavaTypeAdapter(DateAdapter.class)
	private java.sql.Date to;

	public Period(Date from, Date to) {
		super();
		this.from = from == null ? null : new java.sql.Date(from.getTime());
		this.to = to == null ? null : new java.sql.Date(to.getTime());
	}

	public Period(LocalDate from, LocalDate to) {
		super();
		this.from = from == null ? null : java.sql.Date.valueOf(from);
		this.to = to == null ? null : java.sql.Date.valueOf(to);
	}

	public boolean isEmpty() {
		return from == null || to == null;
	}

	@Override
	public int compareTo(Period o) {
		if (o == null) {
			return -1;
		}

		int f = from.compareTo(o.from);
		if (f == 0) {
			f = to.compareTo(o.to);
		}

		return f;
	}

	@Override
	public String toString() {
		return "Period [from=" + from + ", to=" + to + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Period other = (Period) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

}