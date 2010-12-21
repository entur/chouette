/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.0.1</a>, using an XML
 * Schema.
 * $Id$
 */

package chouette.schema;

/**
 * La liste de tous les itin�raires de la ligne.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public class ChouetteRoute extends chouette.schema.RouteTypeType 
implements java.io.Serializable
{


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _routeExtension.
     */
    private chouette.schema.RouteExtension _routeExtension;


      //----------------/
     //- Constructors -/
    //----------------/

    public ChouetteRoute() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'routeExtension'.
     * 
     * @return the value of field 'RouteExtension'.
     */
    public chouette.schema.RouteExtension getRouteExtension(
    ) {
        return this._routeExtension;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, handler);
    }

    /**
     * Sets the value of field 'routeExtension'.
     * 
     * @param routeExtension the value of field 'routeExtension'.
     */
    public void setRouteExtension(
            final chouette.schema.RouteExtension routeExtension) {
        this._routeExtension = routeExtension;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled chouette.schema.ChouetteRoute
     */
    public static chouette.schema.ChouetteRoute unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (chouette.schema.ChouetteRoute) org.exolab.castor.xml.Unmarshaller.unmarshal(chouette.schema.ChouetteRoute.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}
