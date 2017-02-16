package kmeans;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

public class Displayer extends JFrame {

	private List<Cluster> points;

	private int[][] connectionMatrix;

	private List<Integer> path;

	private String titleString;

	public Displayer(List<Cluster> points, int[][] connectionMatrix, String title, List<Integer> path) {
		this.points = points;
		this.setTitle(title);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(1400, 700);
		this.setBackground(null);
		this.setVisible(true);
		this.points = points;
		this.connectionMatrix = connectionMatrix;
		this.path = path;
		this.titleString = title;
	}

	public void paint(Graphics g) {
		super.paint(g);
		List<Color> colors = new ArrayList<>();
		// draw clusters
		for (int i = 0; i < points.size(); i++) {
			Color color = new Color(i * 20 % 255, (i * 20 + 100) % 255, 255 - i * 20 % 255);
			g.setColor(color);
			for (int j = 0; j < points.get(i).getNumPoints(); j++) {
				LatLon ll = points.get(i).getPoints().get(j).getLatlon();
				g.fillOval((int) ll.getLat(), (int) ll.getLon(), 5, 5);
				g.drawString(String.valueOf(i), (int) ll.getLat(), (int) ll.getLon());
			}
			if (!titleString.equals("final")) {
			LatLon center = points.get(i).getCenter();
			int x = (int) center.getLat();
			int y = (int) center.getLon();
			int r = (int) Math.ceil(points.get(i).getRadius());
				g.drawOval(x - r, y - r, 2 * r, 2 * r);
			}
		}
		// draw connections
		if (!titleString.equals("final")) {
		for (int i = 0; i < connectionMatrix.length - 1; i++) {
			for (int j = i + 1; j < connectionMatrix.length; j++) {
				if (connectionMatrix[i][j] == 1) {
					LatLon c1 = points.get(i).getCenter();
					LatLon c2 = points.get(j).getCenter();
					g.drawLine((int) c1.getLat(), (int) c1.getLon(), (int) c2.getLat(), (int) c2.getLon());
				}
			}
			}
		}
		// draw path
		if (path != null) {
			for (int i = 0; i < path.size() - 1; i++) {
				LatLon c1 = points.get(path.get(i)).getCenter();
				LatLon c2 = points.get(path.get(i + 1)).getCenter();
				g.setColor(Color.RED);
				g.drawLine((int) c1.getLat(), (int) c1.getLon(), (int) c2.getLat(), (int) c2.getLon());
			}
		}
	}
}
