import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Date;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;

public class HttpServer {
    private String serverAddress;
    private int serverPort;
    private File rootDir;
    private TreeMap<String, String> mimeTypes;
    private ServerSocket serverSo;
    
    public HttpServer() {
        try {
            parseHttpConf();
            mimeTypes = new TreeMap<String, String>();
            parseMIME();
            serverSo = new ServerSocket(serverPort, 10, InetAddress.getByName(serverAddress));
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    private void parseHttpConf() throws IOException {
        File configFile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "http.conf");
        
        FileInputStream confStream = new FileInputStream(configFile);
        Scanner confIn = new Scanner(confStream);
        
        serverAddress = confIn.nextLine().substring("address".length()).trim();
        serverPort = Integer.parseInt(confIn.nextLine().substring("port".length()).trim());
        rootDir = new File(confIn.nextLine().substring("root_dir".length()).trim());
        
        confStream.close();
        confIn.close();
    }
    private void parseMIME() throws IOException {
        File mimeTypesFile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "mime.types");
        
        FileInputStream mimeStream = new FileInputStream(mimeTypesFile);
        Scanner mimeIn = new Scanner(mimeStream);
        
        String pattern = "(\\S+)(\\s+)(.*)";
        Pattern r = Pattern.compile(pattern);
        
        while ( mimeIn.hasNextLine() ) {
            String line = mimeIn.nextLine();
            Matcher m = r.matcher(line);
            if ( m.find() )
                mimeTypes.put(m.group(1), m.group(3));
            else
                continue;
        }
    }
    
    public void start() {
        try {
            while ( true ) {
                Socket incoming = serverSo.accept();
                processConnection(incoming);
                
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    private void processConnection(Socket incoming) {
        try (
            InputStream inStream = incoming.getInputStream();
            OutputStream outStream = incoming.getOutputStream();
            Scanner in = new Scanner(inStream);
            PrintWriter out = new PrintWriter(outStream, true)
            ) {
            String filename, fileExtension, httpVersion;
            
            if ( in.hasNextLine() ) {
                String line = in.nextLine();
                System.out.println(line);

                String[] startLineArr = line.split(" ");
                httpVersion = startLineArr[2];
                
                filename = startLineArr[1];
                if ( filename.equals("/") ) {
                    filename = "/index.html";
                }
                String[] fileTokens = filename.split("\\.");
                fileExtension = fileTokens[fileTokens.length - 1];
                
                if ( !startLineArr[0].equals("GET") ) {
                    String errorMsg = httpVersion.trim() + " 405 Method Not Allowed\n";
                    outStream.write(errorMsg.getBytes());
                    return;
                }
            } else {
                return;
            }
            
            File targetToUpload = new File(rootDir.toString() + filename);
            if ( !targetToUpload.exists() ) {
                String errorMsg = httpVersion.trim() + " 404 Not Found\n";
                outStream.write(errorMsg.getBytes());
                System.out.println("404 /some-strange-url.notfound");
                System.out.println("root dir is " + rootDir.toString());

                return;
            }
            out.println(httpVersion + " 200 OK");
            System.out.println("200 " + filename);
            out.println("Connetction: close");
            out.println("Content-Length: " + targetToUpload.length());
            out.println("Content-Type: " + selectMimeType(fileExtension));
            out.println("Date: " + getDate());
            out.println("");
            
            byte[] bytesToUpload = new byte[(int)targetToUpload.length()];
            FileInputStream fileInStream = new FileInputStream(targetToUpload);
            fileInStream.read(bytesToUpload);
            outStream.write(bytesToUpload);
        } catch ( IOException e ) {
            e.printStackTrace();
        } 
    }
    private String selectMimeType(String extension) {
        String requiredExtension = mimeTypes.get(extension);
        if ( requiredExtension == null ) {
            return "application/octet-stream";
        }
        return requiredExtension;
    }
    private String getDate() {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("E',' d MMM y hh:mm:ss 'GMT'");
        
        return ft.format(dNow).toString();
    }
    
    public static void main(String[] args) {
        System.out.println("zalupa");

        HttpServer server = new HttpServer();
        server.start();
    }
}