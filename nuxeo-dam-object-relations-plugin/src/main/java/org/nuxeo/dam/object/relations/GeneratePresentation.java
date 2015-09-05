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
 *     Thibaud Arguillere
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
import org.nuxeo.ecm.core.api.Blobs;
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
@Operation(id = GeneratePresentation.ID, category = Constants.CAT_CONVERSION, label = "GeneratePresentation", description = "Receives a list of assets to print. Possible values for style: 3x3, 2x2 or Landscape 6x3. Also, the generatePresentation_site context variable is filled with a Blob of the zipped-site.")
public class GeneratePresentation {

    public static final String ID = "GeneratePresentation";

    public static final String WKHTMLTOPDF_COMMAND = "wkhtmltopdf-default";

    public static final String JPEG_PICTURE_VIEW = "OriginalJpeg";

    public static final String MINISITE_BLOB_VAR_NAME = "generatePresentation_site";

    // All these should be put in a template, so no need to change the plug-in to add new layout
    // Globally, we should have templating system (using FreeMarker, typically).
    // (no time to build this before a demo)
    public static final String STYLE_2x2 = ".floating-box {float: left;width: 500px;height: 600px;margin: none; border: none;}\n"
            + ".imgThumb {max-width: 500px;max-height: 600px;}\n"
            + ".mainContainer {width: 1000px;}\n"
            + "body {text-align:-webkit-center;}\n";

    public static final String STYLE_3x3 = ".floating-box {float: left;width: 310px;height: 370px;margin: none; border: none;}\n"
            + ".imgThumb {max-width: 310px;max-height: 370px;}\n"
            + ".mainContainer {width: 1000px;}\n"
            + "body {text-align:-webkit-center;}\n";

    public static final String STYLE_LANDSCAPE_6x3 = ".floating-box {float: left;width: 250px;height: 300px;margin: none; border: none;}\n"
            + ".imgThumb {max-width: 250px;max-height: 300px;}\n"
            + ".mainContainer {width: 1500px;}\n"
            + "body {text-align:-webkit-center;}\n";

    public static final String IMAGE_DIV_TEMPLATE = "<div class='floating-box keeptogether'><img src='img/THE_IMAGE' class='imgThumb'></div>\n";

    protected DocumentModelList docs;

    protected String hardCodedStyle = "";
    
    protected File currentTempWorkingFolder = null;

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "fileName", required = false)
    String fileName = "";

    @Param(name = "title", required = false)
    String title = "";

    // @Param(name = "style", required = false, widget = Constants.W_OPTION, values = {"3x3", "2x2", "Landscape 6x3"})
    @Param(name = "style", required = false)
    String style = "3x3";

    @OperationMethod
    public Blob run(DocumentModelList input) throws IOException, CommandNotAvailable {

        docs = input;

        String orientation;

        if (StringUtils.isBlank(style)) {
            style = "3x3";
        }

        if (StringUtils.isBlank(fileName)) {
            fileName = "Presentation-" + style + ".pdf";
        } else if(!fileName.toLowerCase().endsWith(".pdf")) {
            fileName += ".pdf";
        }

        switch (style.toLowerCase()) {
        case "landscape 6x3":
            orientation = "Landscape";
            hardCodedStyle = STYLE_LANDSCAPE_6x3;
            break;

        case "3x3":
            orientation = "Portrait";
            hardCodedStyle = STYLE_3x3;
            break;

        default:
            orientation = "Portrait";
            hardCodedStyle = STYLE_2x2;
            break;
        }

        File indexFile = buildMiniSite();
        // buildMiniSite() also setup the MINISITE_BLOB_VAR_NAME Context Variable

        // Create a temp. File handled by Nuxeo
        Blob resultPdf = Blobs.createBlobWithExtension(".pdf");

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("sourceFilePath", indexFile.getAbsolutePath());
        params.addNamedParameter("targetFilePath", resultPdf.getFile().getAbsolutePath());
        params.addNamedParameter("orientation", orientation);

        // Run
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult result = cles.execCommand(WKHTMLTOPDF_COMMAND, params);

        if (result.getError() != null) {
            throw new ClientException("Failed to execute the command <" + WKHTMLTOPDF_COMMAND + ">", result.getError());
        }

        if (!result.isSuccessful()) {
            throw new ClientException("Failed to execute the command <" + WKHTMLTOPDF_COMMAND + ">. Final command [ "
                    + result.getCommandLine() + " ] returned with error " + result.getReturnCode());
        }
        resultPdf.setMimeType("application/pdf");
        resultPdf.setFilename(fileName);

        // Now we used Nuxeo temp file Handling we can do some cleanup so we don't fill the tmp folder
        cleanup();

        return resultPdf;
    }

    protected File buildMiniSite() throws IOException {

        File indexHtml = null;
        currentTempWorkingFolder = new File(Files.createTempDirectory(
                "wkhtmltopdf-minisite-" + java.util.UUID.randomUUID().toString()).toString());
        String htmlFolderName = fileName.replace(".pdf", "") + "-site";
        File mainFolder = new File(currentTempWorkingFolder, htmlFolderName);
        mainFolder.mkdir();
        // File mainFolder = new File(Files.createTempDirectory("wkhtmltopdf-" + java.util.UUID.randomUUID().toString())
        // .toString());
        File imgFolder = new File(mainFolder, "img");
        imgFolder.mkdir();

        String html = "<!DOCTYPE html><html>";
        html += "<head><style>\n";
        html += hardCodedStyle + "\n";
        html += "</style></head>";

        html += "<body>\n";
        html += "<div id='mainCont' class='mainContainer'>\n";
        if (StringUtils.isNotBlank(title)) {
            html += "<h2 style='text-align: center;'>" + title + "</h2>\n";
        }
        Blob image;
        File imageFile;
        String imageName;
        int count = 0;
        for (DocumentModel doc : docs) {
            if (doc.hasSchema("picture")) {
                MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);

                if (mvp != null) {
                    PictureView v = mvp.getView(JPEG_PICTURE_VIEW);
                    if (v != null) {
                        image = v.getBlob();
                        if (image != null) {
                            count += 1;
                            imageName = "img-" + count + ".jpg";
                            imageFile = new File(imgFolder, imageName);
                            image.transferTo(imageFile);

                            html += IMAGE_DIV_TEMPLATE.replace("THE_IMAGE", imageName);
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

        File miniSiteZip = new File(currentTempWorkingFolder, htmlFolderName + ".zip");
        ZipDirectory zd = new ZipDirectory(mainFolder.getAbsolutePath(), miniSiteZip.getAbsolutePath());
        zd.zip();
        // miniSiteZip is a valid .zip archive of the site
        Blob fileSiteZip = new FileBlob(miniSiteZip);

        // Now, transfer to a temp. blob handled by Nuxeo
        // (we'll delete everyting else later)
        Blob tempBlob = Blobs.createBlobWithExtension(".zip");
        fileSiteZip.transferTo(tempBlob.getFile());
        tempBlob.setMimeType("application/zip");
        tempBlob.setFilename(htmlFolderName + ".zip");

        ctx.put(MINISITE_BLOB_VAR_NAME, tempBlob);

        return indexHtml;
    }
    
    protected void cleanup() throws IOException {
        
        if(currentTempWorkingFolder != null && currentTempWorkingFolder.exists()) {
            org.apache.commons.io.FileUtils.deleteDirectory(currentTempWorkingFolder);
        }
        
        currentTempWorkingFolder = null;
    }

}
