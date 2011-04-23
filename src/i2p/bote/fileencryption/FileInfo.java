package i2p.bote.fileencryption;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import net.i2p.data.Base64;

/**
 * Shows information on encrypted I2P-Bote files in a directory and all subdirectories.<br/>
 * Syntax: <code>FileInfo &lt;directory&gt;</code><br/>
 * If the directory is omitted, the current directory is used.
 */
public class FileInfo {
    
    public FileInfo(String[] args) throws IOException {
        String startDir;
        if (args.length > 0)
            startDir = args[0];
        else
            startDir = ".";
        
        System.out.println("--------------------- Filename --------------------- Ver - N -- r  p -- Salt --");
        printInfo(new File(startDir));
    }
    
    private void printInfo(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            System.err.println("Error: File.listFiles() returned null for <" + dir.getCanonicalPath() + ">.");
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
            System.out.println(dir.getCanonicalPath());
        }
        
        for (File file: files)
            if (file.isDirectory())
                printInfo(file);
            else {
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
                            continue;
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
    }
    
    public static void main(String[] args) throws IOException {
        new FileInfo(args);
    }
}