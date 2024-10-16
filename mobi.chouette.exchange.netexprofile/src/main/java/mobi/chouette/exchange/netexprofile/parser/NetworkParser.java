package mobi.chouette.exchange.netexprofile.parser;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.OrganisationRefStructure;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.Company;
import mobi.chouette.model.GroupOfLine;
import mobi.chouette.model.Line;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

public class NetworkParser extends NetexParser implements Parser, Constant {

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
		NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);
        org.rutebanken.netex.model.Network netexNetwork = (org.rutebanken.netex.model.Network) context.get(NETEX_LINE_DATA_CONTEXT);

        mobi.chouette.model.Network chouetteNetwork = ObjectFactory.getPTNetwork(referential, netexNetwork.getId());
        chouetteNetwork.setObjectVersion(NetexParserUtils.getVersion(netexNetwork));

        if (netexNetwork.getCreated() != null) {
            chouetteNetwork.setCreationTime(netexNetwork.getCreated());
        }
        if (netexNetwork.getChanged() != null) {
            chouetteNetwork.setVersionDate(netexNetwork.getChanged().toLocalDate());
        }

        chouetteNetwork.setName(netexNetwork.getName().getValue());

        OrganisationRefStructure authorityRefStruct = netexNetwork.getTransportOrganisationRef().getValue();
        Company company = ObjectFactory.getCompany(referential, authorityRefStruct.getRef());
        chouetteNetwork.setCompany(company);
        chouetteNetwork.setDescription(ConversionUtil.getValue(netexNetwork.getDescription()));
       
        if (netexNetwork.getPrivateCode() != null) {
            chouetteNetwork.setRegistrationNumber(netexNetwork.getPrivateCode().getValue());
        }
        if (netexNetwork.getMainLineRef() != null) {
            Line line = ObjectFactory.getLine(referential, netexNetwork.getMainLineRef().getRef());
            line.setNetwork(chouetteNetwork);
        }

        if (netexNetwork.getGroupsOfLines() != null) {
            List<GroupOfLines> groupsOfLines = netexNetwork.getGroupsOfLines().getGroupOfLines();

            for (GroupOfLines groupOfLines : groupsOfLines) {
                GroupOfLine groupOfLine = ObjectFactory.getGroupOfLine(referential, groupOfLines.getId());
                groupOfLine.setName(ConversionUtil.getValue(groupOfLines.getName()));

                if (groupOfLines.getMembers() != null) {
                    for (JAXBElement<? extends LineRefStructure> lineRefRelStruct : groupOfLines.getMembers().getLineRef()) {
                        String lineIdRef = lineRefRelStruct.getValue().getRef();
                        Line line = ObjectFactory.getLine(referential, lineIdRef);

                        if (line != null) {
                            groupOfLine.addLine(line);
                        }
                    }
                }
             
                netexReferential.getGroupOfLinesToNetwork().put(groupOfLine.getObjectId(),chouetteNetwork.getObjectId());
                groupOfLine.setFilled(true);
            }
        }

        chouetteNetwork.setFilled(true);
    }

 
    static {
        ParserFactory.register(NetworkParser.class.getName(), new ParserFactory() {
            private NetworkParser instance = new NetworkParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
