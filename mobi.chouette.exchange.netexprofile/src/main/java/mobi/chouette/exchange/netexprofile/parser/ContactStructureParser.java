package mobi.chouette.exchange.netexprofile.parser;

import mobi.chouette.model.ContactStructure;

/**
 * Parse NeTEx ContactStructure to local ContactStructure objects.
 */
public class ContactStructureParser {

	public ContactStructure parse(org.rutebanken.netex.model.ContactStructure netexObject) {
		if (netexObject == null) {
			return null;
		}
		ContactStructure chouetteObject = new ContactStructure();
		if (netexObject.getContactPerson() != null) {
			chouetteObject.setContactPerson(mobi.chouette.exchange.netexprofile.ConversionUtil.getValue(netexObject.getContactPerson()));
		}
		chouetteObject.setUrl(netexObject.getUrl());
		chouetteObject.setPhone(netexObject.getPhone());
		chouetteObject.setFax(netexObject.getFax());
		chouetteObject.setEmail(netexObject.getEmail());
		if (netexObject.getFurtherDetails() != null) {
			chouetteObject.setFurtherDetails(mobi.chouette.exchange.netexprofile.ConversionUtil.getValue(netexObject.getFurtherDetails()));
		}
		return chouetteObject;
	}
}
