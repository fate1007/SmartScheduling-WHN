import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class p2j {
	public static void main(String args[]){
		try {
			File f = new File('points.txt');
			InputStreamReader read = new InputStreamReader(new FileInputStream(f));
			BufferedReader br = new BufferedReader(read);
			String line = null;
			while((line = bufferedReader.readLine()) != null){
            	
            }
            read.close();
		}
		catch(Exception e){}
	}
}