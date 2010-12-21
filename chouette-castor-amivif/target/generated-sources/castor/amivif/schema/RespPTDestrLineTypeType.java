/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.0.1</a>, using an XML
 * Schema.
 * $Id$
 */

package amivif.schema;

/**
 * Message type pour la destruction d'une ligne
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public abstract class RespPTDestrLineTypeType extends chouette.schema.castor.SchemaObject 
implements java.io.Serializable
{


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _line.
     */
    private amivif.schema.Line _line;


      //----------------/
     //- Constructors -/
    //----------------/

    public RespPTDestrLineTypeType() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'line'.
     * 
     * @return the value of field 'Line'.
     */
    public amivif.schema.Line getLine(
    ) {
        return this._line;
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
     * Sets the value of field 'line'.
     * 
     * @param line the value of field 'line'.
     */
    public void setLine(
            final amivif.schema.Line line) {
        this._line = line;
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
