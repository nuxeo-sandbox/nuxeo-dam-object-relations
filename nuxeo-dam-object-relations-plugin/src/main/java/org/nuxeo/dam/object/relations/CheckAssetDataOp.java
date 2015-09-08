/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 */
@Operation(id = CheckAssetDataOp.ID, category = Constants.CAT_DOCUMENT, label = "Demo: Check Asset Data", description = "Create vocabulary entry if needed, etc.")
public class CheckAssetDataOp {

    public static final String ID = "CheckAssetDataOp";

    protected static final String[] FIELDS = { "asset:body_type", "asset:body_color", "asset:print_location" };

    protected static final String[] VOCS = { "BodyType", "BodyColor", "PrintLocation" };

    protected static final int FIELDS_MAX = FIELDS.length - 1;
        
    protected static DirectoryService directoryService = null;
    
    List<String> vocValues;

    @Context
    protected CoreSession session;

    @Param(name = "save", required = false, values = { " false" })
    boolean save = false;

    @OperationMethod
    public DocumentModel run(DocumentModel inDoc) {

        String value;
        
        if(directoryService == null) {
            directoryService = Framework.getService(DirectoryService.class);
        }

        // Handle the vocabularies
        for(int i = 0; i < FIELDS_MAX; ++i) {
            value = (String) inDoc.getPropertyValue(FIELDS[i]);
            if(StringUtils.isNotBlank(value)) {
                value = value.replace("/","-");
                org.nuxeo.ecm.directory.Session directorySession = directoryService.open(VOCS[i]);                
                if (!directorySession.hasEntry(value)) {
                    Map<String, Object> entry = new HashMap<String, Object>();
                    entry.put("id", value);
                    entry.put("label", value);
                    entry.put("parent", "");
                    entry.put("obsolete", 0);
                    entry.put("ordering", 0);

                    directorySession.createEntry(entry);
                }
                directorySession.close();
            }
        }

        return inDoc;
    }

}
