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
 * �������������� MotionDetectionEffect ��ʾ������
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
	 * ����һ������Դ�Դ���һ��processor���������processor������Ƶ�ļ�
	 * �ڴ�processor������״̬�У���MotionDetectionEffect��TimeStampEffect������Ƶ
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

		// ��Processor��Ϊ����״̬
		p.configure();
		if (!waitForState(p.Configured)) {
			System.err.println("Failed to configure the processor.");
			return false;
		}

		// ���¼��ɽ���Processor������Ƶ������

		p.setContentDescriptor(null);
		// ��ȡ��Ƶ���������
		TrackControl tc[] = p.getTrackControls();

		if (tc == null) {
			System.err.println("Failed to obtain track controls from the processor.");
			return false;
		}

		// ����������Ƶ���������
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

		//��ʼ�������ý�������ʹ����������Ƶ������
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
		// ��ʾ visual & control ���

		// ��ȡ����������ͨ��processor����������

		setLayout(new BorderLayout());

		Component cc;

		Component vc;
		if ((vc = p.getVisualComponent()) != null) {
			add("Center", vc);
		}

		if ((cc = p.getControlPanelComponent()) != null) {
			add("South", cc);
		}

		// ��������ʽ���� processor.
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
	 * ��processor����ָ��״̬ǰ����������
	 * �����ʧ�ܣ��򷵻�false
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
	 * Controller����
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
		// ָ����Ƶ�ļ�(ԭ�������ɵ��ñ�������ͷֱ�Ӳ�׽ͼ��ʱ��ִ�δ��ʵ�֣���֮�Զ�ȡ������Ƶ�ļ�)
		String url = "file:" + aviFile.getAbsolutePath();
		System.out.println("url:" + url);
		MediaLocator ml;
		// ����MediaLocator����
		if ((ml = new MediaLocator(url)) == null) {
			System.err.println("Cannot build media locator from: " + url);
			System.exit(0);
		}

		TestMotionDetection fa = new TestMotionDetection();
		// ��MediaLocator����̶����в���
		if (!fa.open(ml))
			System.exit(0);
	}

	static void prUsage() {
		System.err.println("Usage: java TestMotionDetection <url>");
	}

}
