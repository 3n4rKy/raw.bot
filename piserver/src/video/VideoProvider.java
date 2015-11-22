package video;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import audio.DownloadFile;

public class VideoProvider implements Runnable {
	public void startVideo() {
		final Logger logger = LogManager.getLogger(DownloadFile.class);
		try {
			logger.debug("may be video start");
			Process p = Runtime.getRuntime().exec("./startVideo.sh");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		startVideo();
		
	}
}