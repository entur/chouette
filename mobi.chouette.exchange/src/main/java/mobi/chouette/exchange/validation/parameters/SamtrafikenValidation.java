package mobi.chouette.exchange.validation.parameters;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SamtrafikenValidation {

    @XmlElement(name = "check_route_section", defaultValue="0")
    private int checkRouteSection = 0;

}
