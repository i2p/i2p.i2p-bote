package i2p.bote.fileencryption;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import net.i2p.data.Base64;

/**
 * Shows information on an encrypted I2P-Bote file, or all files in a
 * directory and all subdirectories.<br/>
 * Syntax: <code>FileInfo &lt;file or directory&gt;</code><br/>
 * If the file/directory is omitted, the current directory is used.
 */
public class FileInfo {
    
    public FileInfo(String[] args) throws IOException {
        String fileArg;
        if (args.length > 0)
            fileArg = args[0];
        else
            fileArg = ".";
        
        File file = new File(fileArg);
        if (!file.exists()) {
            System.err.println("File or directory not found: " + file);
            System.exit(1);
        }
        
        System.out.println("--------------------- Filename --------------------- Ver - N -- r  p -- Salt --");
        printInfo(file);
    }
    
    /** Prints information for a file or a directory */
    private void printInfo(File fileOrDir) throws IOException {
        if (!fileOrDir.isDirectory()) {
            printFileInfo(fileOrDir);
            return;
        }
        
        File[] files = fileOrDir.listFiles();
        if (files == null) {
            System.err.println("Error: File.listFiles() returned null for <" + fileOrDir.getCanonicalPath() + ">.");
            return;
        }
        // put files first, directories last
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return Boolean.valueOf(file1.isDirectory()).compareTo(file2.isDirectory());
            }
        });
        if (files.length > 0) {
            System.out.println();
            System.out.println(fileOrDir.getCanonicalPath());
        }
        
        for (File file: files)
            printInfo(file);
    }
    
    /** Prints information for a file */
    private void printFileInfo(File file) throws IOException {
        String filename = file.getName();
        System.out.print("  ");
        System.out.print(filename.length()>50 ? filename.substring(0, 50) : filename);
        for (int i=filename.length(); i<50; i++)
            System.out.print(" "); 
        System.out.print("  ");

        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(new FileInputStream(file));
            if (!"derivparams".equals(file.getName())) {
                byte[] sofBuffer = new byte[4];
                int bytesRead = inputStream.read(sofBuffer);
                if (bytesRead<4 || !Arrays.equals(sofBuffer, FileEncryptionConstants.START_OF_FILE)) {
                    System.out.println("not encrypted");
                    return;
                }
                System.out.print((inputStream.read()&0xFF) + "  ");
            }
            else
                System.out.print("   ");
            
            System.out.print(inputStream.readInt() + "  ");
            System.out.print(inputStream.readInt() + "  ");
            System.out.print(inputStream.readInt() + "  ");
            byte[] saltBuffer = new byte[6];
            inputStream.read(saltBuffer);
            System.out.print(Base64.encode(saltBuffer));
            System.out.println();
        }
        catch (Exception e) {
            System.out.println();
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
    
    public static void main(String[] args) throws IOException {
        new FileInfo(args);
    }
}