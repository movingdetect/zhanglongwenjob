import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.control.*;
import java.io.*;

/**
 * 以下是用来测试 MotionDetectionEffect 的示例程序。
 */
public class TestMotionDetection extends Frame implements ControllerListener {

	Processor p;
	DataSink fileW = null;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;

	public TestMotionDetection() {
		super("Test Motion Detection");
	}

	/**
	 * 给定一个数据源以创建一个processor，并用这个processor播放视频文件
	 * 在此processor的配置状态中，将MotionDetectionEffect及TimeStampEffect插入视频
	 * 
	 */
	public boolean open(MediaLocator ds) {

		try {
			p = Manager.createProcessor(ds);
		} catch (Exception e) {
			System.err.println("Failed to create a processor from the given datasource: " + e);
			return false;
		}

		p.addControllerListener(this);

		// 将Processor置为配置状态
		p.configure();
		if (!waitForState(p.Configured)) {
			System.err.println("Failed to configure the processor.");
			return false;
		}

		// 以下即可将此Processor用作视频播放器

		p.setContentDescriptor(null);
		// 获取视频轨道控制器
		TrackControl tc[] = p.getTrackControls();

		if (tc == null) {
			System.err.println("Failed to obtain track controls from the processor.");
			return false;
		}

		// 搜索遍历视频轨道控制器
		TrackControl videoTrack = null;

		for (int i = 0; i < tc.length; i++) {
			if (tc[i].getFormat() instanceof VideoFormat) {
				videoTrack = tc[i];
				break;
			}
		}

		if (videoTrack == null) {
			System.err.println("The input media does not contain a video track.");
			return false;
		}

		//初始化并设置解码器，使其作用于视频数据流
		try {
			Codec codec[] = { new MotionDetectionEffect(), new TimeStampEffect() };
			videoTrack.setCodecChain(codec);
		} catch (UnsupportedPlugInException e) {
			System.err.println("The processor does not support effects.");
		}

		// Realize the processor.

		p.prefetch();
		if (!waitForState(p.Prefetched)) {
			System.err.println("Failed to realize the processor.");
			return false;
		}
		// 显示 visual & control 组件

		// 获取播放器，获通过processor构建播放器

		setLayout(new BorderLayout());

		Component cc;

		Component vc;
		if ((vc = p.getVisualComponent()) != null) {
			add("Center", vc);
		}

		if ((cc = p.getControlPanelComponent()) != null) {
			add("South", cc);
		}

		// 启动并正式运行 processor.
		p.start();

		setVisible(true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				p.close();
				System.exit(0);
			}
		});

		p.start();

		return true;
	}

	public void addNotify() {
		super.addNotify();
		pack();
	}

	/**
	 * 在processor进入指定状态前，进程阻塞
	 * 如操作失败，则返回false
	 */
	boolean waitForState(int state) {
		synchronized (waitSync) {
			try {
				while (p.getState() != state && stateTransitionOK)
					waitSync.wait();
			} catch (Exception e) {
			}
		}
		return stateTransitionOK;
	}

	/**
	 * Controller监听
	 */
	public void controllerUpdate(ControllerEvent evt) {

		System.out.println(this.getClass().getName() + evt);
		if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent) {
			synchronized (waitSync) {
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		} else if (evt instanceof ResourceUnavailableEvent) {
			synchronized (waitSync) {
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} else if (evt instanceof EndOfMediaEvent) {
			p.close();
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		File aviFile = new File("E:/v5.mpg");
		// 指定视频文件(原打算做成调用本机摄像头直接捕捉图像，时间仓促未能实现，换之以读取已有视频文件)
		String url = "file:" + aviFile.getAbsolutePath();
		System.out.println("url:" + url);
		MediaLocator ml;
		// 创建MediaLocator对象
		if ((ml = new MediaLocator(url)) == null) {
			System.err.println("Cannot build media locator from: " + url);
			System.exit(0);
		}

		TestMotionDetection fa = new TestMotionDetection();
		// 打开MediaLocator对象继而进行操作
		if (!fa.open(ml))
			System.exit(0);
	}

	static void prUsage() {
		System.err.println("Usage: java TestMotionDetection <url>");
	}

}
