
package org.cidarlab.eugenelab.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Erik
 */
public class FileManager {
    
    private String rootDirectory;
    
    public FileManager(String rootDirectory, String user) {
        this.rootDirectory = rootDirectory;
    }
    
    private String loadFile(String fileName) {
        BufferedReader br = null;
        try {
            String currentFileExtension = getFileExtension(fileName, true);
            File file = new File(currentFileExtension);
            br = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String toReturn = "";
            String line = br.readLine();
            while (line != null) {
                toReturn = toReturn + "\n" + line;
                line = br.readLine();
            }
            br.close();
            return toReturn;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "the bads";
        }
    }
    
    private void saveFile(String fileName, String fileContent) throws IOException {
        String currentFileExtension = getFileExtension("/" + fileName, true);
        File file = new File(currentFileExtension);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        bw.write(fileContent);
        bw.close();
    }
    
    private void deleteFile(String fileName, String currentFolder) {
        String currentFileExtension = getFileExtension(currentFolder + "/" + fileName, true);
        File file = new File(currentFileExtension);
        file = file.getAbsoluteFile();
        file.delete();
    }
    
    private boolean createNewFolder(String folderExtension) {
        folderExtension = getFileExtension(folderExtension, false);
        File newFolder = new File(folderExtension);
        return newFolder.mkdir(); //returns true if successful
    }
    
    private String getFileExtension(String localExtension, boolean isFile) {
        //String extension = this.getServletContext().getRealPath("/") + "data/" + getCurrentUser() + "/" + localExtension;
        String extension = this.rootDirectory + localExtension;
        if (!isFile) {
            extension += "/";
        }
        return extension;
    }
}
