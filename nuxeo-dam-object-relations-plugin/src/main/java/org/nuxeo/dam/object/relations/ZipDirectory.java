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
 * 
 * This is a small adaptation after a copy paste of
 * http://www.avajava.com/tutorials/lessons/how-do-i-zip-a-directory-and-all-its-contents.html
 */
package org.nuxeo.dam.object.relations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @since 7.3
 */
public class ZipDirectory {

    // Path to the whole folder to zip
    protected File directoryToZip;

    // Path to the .zip file
    protected String destZipPath;

    protected byte[] bytes = new byte[4096];

    public ZipDirectory(String inDirectoryPath, String inDestZipPah) {

        directoryToZip = new File(inDirectoryPath);
        destZipPath = inDestZipPah;
    }

    public void zip() throws IOException {

        List<File> fileList = new ArrayList<File>();

        getAllFiles(directoryToZip, fileList);

        writeZipFile(directoryToZip, fileList);
    }

    protected void getAllFiles(File dir, List<File> fileList) {

        File[] files = dir.listFiles();
        for (File file : files) {
            fileList.add(file);
            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

    protected void writeZipFile(File directoryToZip, List<File> fileList) throws IOException {

        FileOutputStream fos = new FileOutputStream(destZipPath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (File file : fileList) {
            if (!file.isDirectory()) { // we only zip files, not directories
                addToZip(directoryToZip, file, zos);
            }
        }

        zos.close();
        fos.close();
    }

    protected void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        System.out.println("Writing '" + zipFilePath + "' to zip file");
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

}
