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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.DocumentsCallback.ReturnStatus;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 */
@Operation(id=UpdatePicturesOp.ID, category=Constants.CAT_SERVICES, label="Demo: Update Pictures", description="WARNING: If you have a lot of Pictures, call this operaiton from an async. event.")
public class UpdatePicturesOp {

    public static final String ID = "Demo.UpdatePictures";
    
    private static final Log log = LogFactory.getLog(UpdatePicturesOp.class);
    
    protected static final int COMMIT_MODULO = 50;
    
    protected int countSaved;

    private UpdateDataWalkerCallback walkerCallback;
    
    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() {

        log.warn("Updating Picture documents...");

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        
        String nxql = "SELECT * FROM Picture WHERE (admin:is_copy IS NULL OR admin:is_copy = 0)";
        nxql += " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy=0 AND ecm:currentLifeCycleState != 'deleted'";
        
        countSaved = 0;
        
        DocumentsWalker dw = new DocumentsWalker(session, nxql, 1000);
        walkerCallback = new UpdateDataWalkerCallback();
        dw.runForEachDocument(walkerCallback);
        
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("...updating done.");
    }
    
    protected class UpdateDataWalkerCallback implements DocumentsCallback {

        long documentCount = 0;

        ReturnStatus lastReturnStatus;

        @Override
        public ReturnStatus callback(List<DocumentModel> inDocs) {

            throw new RuntimeException("Should not be here. We are walking doc by doc");
        }

        @Override
        public ReturnStatus callback(DocumentModel inDoc) {

            documentCount += 1;
            try {
                doUpdateDoc(inDoc);
            } catch (Exception e) {
                log.error("Error while updating a document", e);
                return ReturnStatus.STOP;
            }

            return ReturnStatus.CONTINUE;

        }

        @Override
        public void init() {
            // Unused here
        }

        @Override
        public void end(ReturnStatus inLastReturnStatus) {
            lastReturnStatus = inLastReturnStatus;
        }

        public long getDocumentCount() {
            return documentCount;
        }

    }
    
    protected void doUpdateDoc(DocumentModel inDoc) {

        AssetRelationsBuilder arb = new AssetRelationsBuilder(inDoc, session);
        inDoc = arb.run();
        
        if(arb.docWasModifiedAndSaved()) {
            countSaved += 1;
            if((countSaved % COMMIT_MODULO) == 0) {
                log.warn("Updated: " + countSaved);

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        
    }

}
