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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since TODO
 */
public class BuildAssetRelations {

    private static final Log log = LogFactory.getLog(BuildAssetRelations.class);

    // The space bewteen the name, the lowercase, there is a final dot: All this is normal (see below)
    public static final String[] COMPOSITION_VALUES = { " comp.", " cmp." };

    DocumentModel doc;

    public BuildAssetRelations(DocumentModel inDoc) {
        doc = inDoc;
    }

    public DocumentModel run() {

        if (!doc.getDocumentType().equals("Picture")) {
            return doc;
        }

        // Assumes the title equals the filename of the binary
        String title = (String) doc.getPropertyValue("dc:title");
        // Minimum 2 numbers, 4 license code, a dot and a file extension
        if (title.length() < 8) {
            return doc;
        }

        // We check if the file is a Composition. In the naming convention, a composition has a nme that ends with
        // " Comp" or "Cmp". Then comes the file extension. SO, we search for " Comp." or " cmp."
        String titleLC = title.toLowerCase();
        String yearStr, code;
        int year;
        boolean isComposition;
        for (String compValue : COMPOSITION_VALUES) {
            if (titleLC.indexOf(compValue) > 0) { // sure does not start with the value.
                // Check if starts with 2 digits
                yearStr = title.substring(0, 2);
                try {
                    year = Integer.parseInt(yearStr);
                    isComposition = true;
                } catch (NumberFormatException nfe) {
                    isComposition = false;
                }
                
                if(isComposition) {
                    
                } else {
                    // It is a "COmposition Art Resource"
                }

                break;
            }
        }

        return doc;
    }
}
