/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.dam.object.relations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Making some hard coded assumptions here. For example:
 * <ul>
 * <li>We have an "IPcontractRoot" whose title is "02. Licenses" (this is defined in the Studio Project)</li>
 * <li>We have only one domain and only one IPcontractRoot folder</li>
 * <li>...</li>
 * </ul>
 * 
 * @since 7.3
 */
public class AssetRelationsBuilder {

    private static final Log log = LogFactory.getLog(AssetRelationsBuilder.class);

    // The space between the name, the lowercase, there is a final dot: All this is normal.
    // We check if the file is a Composition. In the naming convention, a composition has a name that ends with
    // " Comp" or "Cmp". Then comes the file extension. So, we search for " Comp." or " cmp."
    public static final String[] COMPOSITION_VALUES = { " comp.", " cmp." };

    public static enum ASSET_TYPE {
        COMPOSITION, COMPOSITION_RESOURCE, OTHER
    };

    // As defined in the Structure Template of the Studio project
    public static final String IPCONTRACTROOT_TITLE = "02. Licenses";

    // As defined in the Structure Template of the Studio project
    public static final String ARTFILENUMBERCONTAINER_TITLE = "02. Art File Numbers";

    // As defined in the Structure Template of the Studio project
    public static final String STYLEUMBERCONTAINER_TITLE = "01. Style Numbers";

    // As defined in the "asset_nature" vocabulary
    public static final String VOC_COMPOSITION_RESOURCE = "Comp Resource";

    // As defined in the "asset_nature" vocabulary
    public static final String VOC_COMPOSITION = "Comp";

    // As defined in the "asset_nature" vocabulary
    public static final String VOC_LICENSED_ART_RESOURCE = "Licensed Art Resource";

    // As defined in the "LicenseStatus" vocabulary
    public static final String VOC_LICENSED = "Licensed";

    // As defined in the "LicenseStatus" vocabulary
    public static final String VOC_NONLICENSED = "Non-Licensed";

    public static final String VOC_DEPARTMENT = "Department";

    protected static String ipContractRootPath = null;

    protected static String artFileNumbeContainerPath = null;

    protected static String styleNumbeContainerPath = null;

    public static final String USUAL_NXQL_LAST_FILTER = " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy=0 AND ecm:currentLifeCycleState != 'deleted'";

    DocumentModel doc;

    String title = null;

    // The title minus the file extension and the Composition Suffix
    String titleNoExtension = null;

    int licenseYear = 0;

    String licenseCode = "";

    String seqNumberStr = "";

    String seqNumberSuffix = "";

    String name = "";

    String department;

    String extension;

    ASSET_TYPE assetType = ASSET_TYPE.OTHER;

    CoreSession session;

    boolean docModifiedAndSaved = false;

    DirectoryService directoryService;

    List<String> departmentValues = null;

    public AssetRelationsBuilder(DocumentModel inDoc, CoreSession inSession) {
        doc = inDoc;
        session = inSession;
        loadDocTitle();

        directoryService = Framework.getService(DirectoryService.class);

        org.nuxeo.ecm.directory.Session directorySession = directoryService.open(VOC_DEPARTMENT);
        departmentValues = directorySession.getProjection(new HashMap<String, Serializable>(), "id");
        directorySession.close();

    }

    // Not 100% reliable: If an Admin changes the vocabulary, we don't know that
    // God enough for a demo.
    protected void checkDepartmentValueInDirectory(String inValue) {
        inValue = inValue == null ? null : inValue.replace("/",  "-");
        if (!departmentValues.contains(inValue)) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("id", inValue);
            entry.put("label", inValue);
            entry.put("parent", "");
            entry.put("obsolete", 0);
            entry.put("ordering", 10000);

            org.nuxeo.ecm.directory.Session directorySession = directoryService.open(VOC_DEPARTMENT);
            directorySession.createEntry(entry);
            directorySession.close();

            departmentValues.add(inValue);
        }
    }

    protected String getIpContractRootPath() {

        if (ipContractRootPath == null) {
            String nxql = "SELECT * FROM IPcontractRoot WHERE dc:title = '" + IPCONTRACTROOT_TITLE + "'";
            nxql += USUAL_NXQL_LAST_FILTER;

            DocumentModelList docs = session.query(nxql);
            // We give up if we don't find it
            if (docs.size() == 0) {
                throw new RuntimeException("Cannot find a IPcontractRoot with dc:title of " + IPCONTRACTROOT_TITLE);
            }
            ipContractRootPath = docs.get(0).getPathAsString();
        }

        return ipContractRootPath;
    }

    protected String getArtFileNumberContainerPath() {

        if (artFileNumbeContainerPath == null) {
            String nxql = "SELECT * FROM ArtFileNumberContainer WHERE dc:title = '" + ARTFILENUMBERCONTAINER_TITLE
                    + "'";
            nxql += USUAL_NXQL_LAST_FILTER;

            DocumentModelList docs = session.query(nxql);
            // We give up if we don't find it
            if (docs.size() == 0) {
                throw new RuntimeException("Cannot find a ArtFileNumberContainer with dc:title of "
                        + ARTFILENUMBERCONTAINER_TITLE);
            }
            artFileNumbeContainerPath = docs.get(0).getPathAsString();
        }

        return artFileNumbeContainerPath;
    }

    protected String getStyleNumberContainerPath() {

        return getStyleNumberContainerPath(session);
    }
    
    public static String getStyleNumberContainerPath(CoreSession inSession) {

        if (styleNumbeContainerPath == null) {
            String nxql = "SELECT * FROM StyleNumberContainer WHERE dc:title = '" + STYLEUMBERCONTAINER_TITLE + "'";
            nxql += USUAL_NXQL_LAST_FILTER;

            DocumentModelList docs = inSession.query(nxql);
            // We give up if we don't find it
            if (docs.size() == 0) {
                throw new RuntimeException("Cannot find a StyleNumberContainer with dc:title of " + STYLEUMBERCONTAINER_TITLE);
            }
            styleNumbeContainerPath = docs.get(0).getPathAsString();
        }
        
        return styleNumbeContainerPath;
    }

    /*
     * Just a way to centralize code, the day we read the title in another place than dc:title. 1st implementation:
     * Assumes the title equals the filename of the binary
     */
    protected void loadDocTitle() {

        if (doc == null) {
            title = null;
        } else {
            title = (String) doc.getPropertyValue("dc:title");
        }
    }

    public DocumentModel run() {

        if (!doc.getType().equals("Picture")) {
            return doc;
        }

        // Minimum 2 numbers, 4 license code, a dot and a file extension
        if (title.length() < 8) {
            return doc;
        }

        extension = "";
        int pos = title.lastIndexOf('.');
        if (pos > 0) {
            extension = title.substring(pos + 1);
            titleNoExtension = title.substring(0, pos);
        }

        // Check if starts with 2 digits
        int year = 0;
        try {
            year = Integer.parseInt(titleNoExtension.substring(0, 2));
        } catch (Exception e) {
            // Ignore, we just want to check if it starts with 2 numbers
        }

        // Starts with 2 digits and ends with the correct suffix => it's a composition
        boolean foundComp = false;
        String titleLC = title.toLowerCase();
        for (String compValue : COMPOSITION_VALUES) {
            pos = titleLC.indexOf(compValue);
            if (pos > 0) { // sure does not start with the value.
                foundComp = true;
                
                titleNoExtension = titleNoExtension.substring(0, pos);
                if (year > 0) {
                    doc = handleLicensedCompositionAsset();

                } else {
                    doc = handleNonLicensedCompositionResource();
                }

                break;
            }
        }
        
        // Not a composition but still maybe an interesting final resource, to link to a License
        if(!foundComp) {
            if(year > 0) {
                doc = handleLicensedNonCompositionAsset();
            } else {
                doc = HandleNonLicensedAsset();
            }
        }

        return doc;
    }

    /*
     * At this step, we are sure the title is ok, starts with 2 digits, etc. IMPORTANT: We don't handle a start with "0"
     */
    // 15BTMN002 King Of Bats cmp.psd
    // 15BTMN002B King Of Bats cmp.psd
    protected DocumentModel handleLicensedCompositionAsset() {
        
        return autoLinkLicensedAsset(ASSET_TYPE.COMPOSITION, VOC_COMPOSITION);
    }

    // example: GR125 basic crew with side tie COMP.psd
    // or JR228 Gym Tote Comp.psd
    // GR is the syle, 228 (or 125) the sequence number, then we have the name
    protected DocumentModel handleNonLicensedCompositionResource() {

        String tmp;
        int pos;

        initValues();

        // -------------------- Extract Info --------------------
        assetType = ASSET_TYPE.COMPOSITION_RESOURCE;

        tmp = titleNoExtension;
        pos = tmp.toLowerCase().lastIndexOf(" comp");
        if (pos > 0) {
            titleNoExtension = tmp.substring(0, pos);
        }

        tmp = titleNoExtension;
        pos = tmp.indexOf(" ");
        if (pos > 0) {
            name = tmp.substring(pos + 1);
            tmp = tmp.substring(0, pos);

            String seqNum = "";
            // First 1-n letters are the department
            char[] chars = new char[tmp.length()];
            tmp.getChars(0, tmp.length(), chars, 0);
            int i = 0;
            for (char c : chars) {
                if (Character.isDigit(c)) {
                    seqNum = tmp.substring(i);
                    break;
                } else {
                    department += Character.toString(c);
                }
                i += 1;
            }

            SeqNumberExtractor sne = new SeqNumberExtractor(seqNum);
            seqNumberStr = sne.numberAsStr;
            seqNumberSuffix = sne.suffix;
        }

        // -------------------- Check --------------------
        // If we don't have enough information, we just do nothing
        if (StringUtils.isBlank(department) || StringUtils.isBlank(seqNumberStr) || StringUtils.isBlank(name)) {
            return doc;
        }

        // -------------------- Link to the StyleNumber --------------------
        checkDepartmentValueInDirectory(department);
        DocumentModel styleDoc;
        // A StyleNumber document has the "linking" and the "style_number" schemas (among others)
        String nxql = "SELECT * FROM StyleNumber WHERE style_number:department = '" + department + "'";
        nxql += " AND style_number:number = '" + seqNumberStr + "'";
        nxql += " AND style_number:short_name = '" + name + "'";
        nxql += USUAL_NXQL_LAST_FILTER;
        DocumentModelList docs = session.query(nxql);
        if (docs.size() == 0) {
            styleDoc = session.createDocumentModel(getStyleNumberContainerPath(), department + seqNumberStr + name,
                    "StyleNumber");

            styleDoc.setPropertyValue("style_number:department", department);
            styleDoc.setPropertyValue("style_number:number", seqNumberStr);
            styleDoc.setPropertyValue("style_number:short_name", name);

            styleDoc = session.createDocument(styleDoc);
            styleDoc = session.saveDocument(styleDoc);

        } else {
            styleDoc = docs.get(0);
        }
        doc.setPropertyValue("linking:style_number_id", styleDoc.getId());

        // -------------------- Last Update(s) --------------------
        doc.setPropertyValue("asset:nature", VOC_COMPOSITION_RESOURCE);
        doc.setPropertyValue("asset:variation_letter", seqNumberSuffix);
        // doc.setPropertyValue("asset:licensing", VOC_LICENSED);

        // -------------------- Ok, we're done --------------------
        doc = session.saveDocument(doc);
        docModifiedAndSaved = true;
        return doc;
    }
    
    protected DocumentModel handleLicensedNonCompositionAsset() {

        return autoLinkLicensedAsset(ASSET_TYPE.OTHER, VOC_LICENSED_ART_RESOURCE);
        
    }
    
    protected DocumentModel HandleNonLicensedAsset() {

        initValues();
        
        return doc;
    }
    
    protected DocumentModel autoLinkLicensedAsset(ASSET_TYPE inType, String inNature) {
        String tmp;
        int pos;

        initValues();

        // -------------------- Extract Info --------------------
        assetType = inType;
        
        licenseYear = Integer.parseInt(titleNoExtension.substring(0, 2));
        licenseCode = title.substring(2, 6);

        // License sequence number is as long the character is a digit
        tmp = titleNoExtension.substring(6);
        // Find the space after the seq. number
        pos = tmp.indexOf(" ");
        if (pos > 0) {

            name = tmp.substring(pos + 1);
            String seqNum = tmp.substring(0, pos);

            SeqNumberExtractor sne = new SeqNumberExtractor(seqNum);
            seqNumberStr = sne.numberAsStr;
            seqNumberSuffix = sne.suffix;
        }

        // -------------------- Check --------------------
        // If we don't have enough information, we just do nothing
        if (StringUtils.isBlank(licenseCode) || StringUtils.isBlank(seqNumberStr) || StringUtils.isBlank(name)) {
            return doc;
        }

        // -------------------- Link to the License --------------------
        // (IPcontract document type in Studio project)
        DocumentModel licenseDoc;
        String licenseDocId;
        String nxql = "SELECT * FROM IPcontract WHERE license:year = " + licenseYear;
        nxql += " AND license:product_line_code = '" + licenseCode + "'";
        nxql += USUAL_NXQL_LAST_FILTER;

        DocumentModelList docs = session.query(nxql);
        if (docs.size() == 0) {
            licenseDoc = session.createDocumentModel(getIpContractRootPath(), "" + licenseYear + licenseCode + " "
                    + licenseCode, "IPcontract");

            licenseDoc.setPropertyValue("license:year", licenseYear);
            licenseDoc.setPropertyValue("license:product_line_code", licenseCode);
            // We don't have the product_line when extracting from a file
            licenseDoc.setPropertyValue("license:product_line", licenseCode);
            licenseDoc.setPropertyValue("license:has_default_product_line", true);

            licenseDoc = session.createDocument(licenseDoc);
            licenseDoc = session.saveDocument(licenseDoc);
        } else {
            licenseDoc = docs.get(0);
        }
        licenseDocId = licenseDoc.getId();
        doc.setPropertyValue("linking:license_id", licenseDocId);

        // -------------------- Link to the ArtFileNumber --------------------
        DocumentModel afnDoc;
        // An ArtFileNumber document has the "linking" and the "ArtFileNumber" schemas (among others)
        nxql = "SELECT * FROM ArtFileNumber WHERE linking:license_id = '" + licenseDocId + "'";
        nxql += " AND art_file_number:number = '" + seqNumberStr + "'";
        nxql += USUAL_NXQL_LAST_FILTER;
        docs = session.query(nxql);
        if (docs.size() == 0) {
            afnDoc = session.createDocumentModel(getArtFileNumberContainerPath(), seqNumberStr + name, "ArtFileNumber");

            afnDoc.setPropertyValue("linking:license_id", licenseDocId);
            afnDoc.setPropertyValue("art_file_number:number", seqNumberStr);
            afnDoc.setPropertyValue("art_file_number:short_name", name);

            afnDoc = session.createDocument(afnDoc);
            afnDoc = session.saveDocument(afnDoc);

        } else {
            afnDoc = docs.get(0);
        }
        doc.setPropertyValue("linking:art_file_number_id", afnDoc.getId());

        // -------------------- Last Update(s) --------------------
        doc.setPropertyValue("asset:nature", inNature);
        doc.setPropertyValue("asset:variation_letter", seqNumberSuffix);
        doc.setPropertyValue("asset:licensing", VOC_LICENSED);

        // -------------------- Ok, we're done --------------------
        doc = session.saveDocument(doc);
        docModifiedAndSaved = true;
        
        return doc;
    }
    
    protected void initValues() {

        licenseYear = 0;
        licenseCode = "";
        seqNumberStr = "";
        seqNumberSuffix = "";
        name = "";
        department = "";
        
        assetType = ASSET_TYPE.OTHER;
    }

    private class SeqNumberExtractor {
        private String numberAsStr = "";

        private String suffix = "";

        private SeqNumberExtractor(String inSeqNum) {

            if (StringUtils.isBlank(inSeqNum)) {
                return;
            }

            char[] chars = new char[inSeqNum.length()];
            inSeqNum.getChars(0, inSeqNum.length(), chars, 0);
            boolean isSuffix = false;
            for (char c : chars) {
                if (isSuffix) {
                    suffix += Character.toString(c);
                } else {
                    if (Character.isDigit(c)) {
                        numberAsStr += Character.toString(c);
                    } else {
                        isSuffix = true;
                        suffix += Character.toString(c);
                    }
                }
            }
        }

    }

    public int getLicenseYear() {
        return licenseYear;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public String getSeqNumberStr() {
        return seqNumberStr;
    }

    public String getSeqNumberSuffix() {
        return seqNumberSuffix;
    }

    public String getName() {
        return name;
    }

    public ASSET_TYPE getAssetType() {
        return assetType;
    }

    public boolean docWasModifiedAndSaved() {
        return docModifiedAndSaved;
    }

    public String getDepartment() {
        return department;
    }

    public String toJsonString() {

        String str = "";

        str = "{";
        str += "\"licenseYear\":" + licenseYear + ",";
        str += "\"licenseCode\":\"" + licenseCode + "\",";
        str += "\"seqNumberStr\":\"" + seqNumberStr + "\",";
        str += "\"seqNumberSuffix\":\"" + seqNumberSuffix + "\",";
        str += "\"assetType\":\"" + assetType.toString() + "\"";
        str += "}";

        return str;
    }
}
