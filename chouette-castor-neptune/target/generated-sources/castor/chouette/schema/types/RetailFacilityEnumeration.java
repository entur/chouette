/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.0.1</a>, using an XML
 * Schema.
 * $Id$
 */

package chouette.schema.types;

/**
 * Values for Retail Facility
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public enum RetailFacilityEnumeration implements java.io.Serializable {


      //------------------/
     //- Enum Constants -/
    //------------------/

    /**
     * Constant UNKNOWN
     */
    UNKNOWN("unknown"),
    /**
     * Constant FOOD
     */
    FOOD("food"),
    /**
     * Constant NEWSPAPERTOBACCO
     */
    NEWSPAPERTOBACCO("newspaperTobacco"),
    /**
     * Constant RECREATIONTRAVEL
     */
    RECREATIONTRAVEL("recreationTravel"),
    /**
     * Constant HYGIENEHEALTHBEAUTY
     */
    HYGIENEHEALTHBEAUTY("hygieneHealthBeauty"),
    /**
     * Constant FASHIONACCESSORIES
     */
    FASHIONACCESSORIES("fashionAccessories"),
    /**
     * Constant BANKFINANCEINSURANCE
     */
    BANKFINANCEINSURANCE("bankFinanceInsurance"),
    /**
     * Constant CASHMACHINE
     */
    CASHMACHINE("cashMachine"),
    /**
     * Constant CURRENCYEXCHANGE
     */
    CURRENCYEXCHANGE("currencyExchange"),
    /**
     * Constant TOURISMSERVICE
     */
    TOURISMSERVICE("tourismService"),
    /**
     * Constant PHOTOBOOTH
     */
    PHOTOBOOTH("photoBooth");

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field value.
     */
    private final java.lang.String value;


      //----------------/
     //- Constructors -/
    //----------------/

    private RetailFacilityEnumeration(final java.lang.String value) {
        this.value = value;
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method fromValue.
     * 
     * @param value
     * @return the constant for this value
     */
    public static chouette.schema.types.RetailFacilityEnumeration fromValue(
            final java.lang.String value) {
        for (RetailFacilityEnumeration c: RetailFacilityEnumeration.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException(value);
    }

    /**
     * 
     * 
     * @param value
     */
    public void setValue(
            final java.lang.String value) {
    }

    /**
     * Method toString.
     * 
     * @return the value of this constant
     */
    public java.lang.String toString(
    ) {
        return this.value;
    }

    /**
     * Method value.
     * 
     * @return the value of this constant
     */
    public java.lang.String value(
    ) {
        return this.value;
    }

}
