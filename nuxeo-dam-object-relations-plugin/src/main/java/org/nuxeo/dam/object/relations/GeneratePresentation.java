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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.api.Framework;

/**
 * We always take the jpeg file (in PIctureViews), not the orginal file:content
 */
@Operation(id = GeneratePresentation.ID, category = Constants.CAT_CONVERSION, label = "GeneratePresentation", description = "Receives a list of assets to print")
public class GeneratePresentation {

    public static final String ID = "GeneratePresentation";
    
    public static final String WKHTMLTOPDF_COMMAND = "wkhtmltopdf-default";
    
    public static final String JPEG_PICTURE_VIEW = "OriginalJpeg";
    
    public static final String IMAGE_DIV_TEMPLATE = "<div class='floating-box keeptogether'><img src='img/THE_IMAGE' class='imgThumb'></div>\n";

    DocumentModelList docs;

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "fileName", required = false)
    String fileName = "";

    @Param(name = "title", required = false)
    String title = "";

    @OperationMethod
    public Blob run(DocumentModelList input) throws IOException, CommandNotAvailable {

        docs = input;

        if (StringUtils.isBlank(fileName)) {
            fileName = "Presentation-.pdf";
        }

        File indexFile = buildMiniSite();
        File destFile = File.createTempFile(java.util.UUID.randomUUID().toString(), ".pdf");
        destFile.deleteOnExit();
        Framework.trackFile(destFile, this);

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("sourceFilePath", indexFile.getAbsolutePath());
        params.addNamedParameter("targetFilePath", destFile.getAbsolutePath());

        // Run
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult result = cles.execCommand(WKHTMLTOPDF_COMMAND, params);
        
        if (result.getError() != null) {
            throw new ClientException("Failed to execute the command <"
                    + WKHTMLTOPDF_COMMAND + ">", result.getError());
        }

        if (!result.isSuccessful()) {
            throw new ClientException("Failed to execute the command <"
                    + WKHTMLTOPDF_COMMAND + ">. Final command [ "
                    + result.getCommandLine()
                    + " ] returned with error "
                    + result.getReturnCode());
        }
        
        FileBlob resultBlob = new FileBlob(destFile);
        resultBlob.setMimeType("application/pdf");
        resultBlob.setFilename(fileName);
        

        return resultBlob;
    }

    protected File buildMiniSite() throws IOException {

        File indexHtml = null;
        File mainFolder = new File( Files.createTempDirectory("wkhtmltopdf-" + java.util.UUID.randomUUID().toString()).toString() );
        String mainFolderPath = mainFolder.getAbsolutePath();
        File imgFolder = new File(mainFolder, "img");
        imgFolder.mkdir();
        
        String html = "<!DOCTYPE html><html>";
        html += "<head><style>\n";
        html += ".floating-box " + "{float: left;width: 300px;height: 350px;margin: 10px;border: 1px solid grey;}\n";
        html += ".imgThumb {max-width: 300px;max-height: 350px;}\n";
        html += ".mainContainer {width: 1000px;}\n";
        html += "@media print {#mainCont .keeptogether {page-break-inside:avoid;}}\n";
        html += "</style></head>";
        
        html += "<body>\n";
        html += "<div id='mainCont' class='mainContainer'>\n";
        if(StringUtils.isNotBlank(title)) {
            html += "<h2 style='text-align: center;'>" + title + "</h2>\n";
        }
        Blob image;
        File imageFile;
        String imageName;
        int count = 0;
        for(DocumentModel doc : docs) {
            if (doc.hasSchema("picture")) {
                MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
                
                if(mvp != null) {
                    PictureView v = mvp.getView(JPEG_PICTURE_VIEW);
                    if(v != null) {
                        image = v.getBlob();
                        if(image != null) {
                            count  += 1;
                            imageName = "img-" + count + ".jpg";
                            imageFile = new File(imgFolder, imageName);
                            image.transferTo(imageFile);
                            
                            html += IMAGE_DIV_TEMPLATE.replace("THE_IMAGE", imageName );
                        }
                    }
                }
            }
        }
        html += "</div>\n";
        html += "</body>\n";
        html += "</html>\n";
        
        indexHtml = new File(mainFolder, "index.html");
        org.apache.commons.io.FileUtils.writeStringToFile(indexHtml, html, false);
        

        return indexHtml;
    }

}
