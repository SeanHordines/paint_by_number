import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public class PaintTest extends JPanel implements MouseListener, MouseMotionListener {

    private int numRows = 16, numCols = 16;
    private int cellSize = 40;
    private Color selectedColor = Color.BLACK;
    private Color[][] targetColors;
    private Color[][] currentColors;
    private BufferedImage baseImage;
    private java.util.List<Color> palette;

    public PaintTest() {
        addMouseListener(this);
        addMouseMotionListener(this);
        init();
    }

    private void init() {
        targetColors = new Color[numRows][numCols];
        currentColors = new Color[numRows][numCols];

        try {
            baseImage = ImageIO.read(new File("starry_night.jpg"));
            baseImage = resizeImage(baseImage, numCols, numRows);
            palette = extractPalette(baseImage, 16); // get up to 10 main colors
        } catch (Exception e) {
            System.out.println("! - Could not load image, using random colors instead.");
            palette = generateRandomPalette(16);
        }

        Random rand = new Random();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numCols; column++) {
                if (baseImage != null) {
                    int rgb = baseImage.getRGB(column, row);
                    targetColors[row][column] = new Color(rgb);
                } else {
                    targetColors[row][column] = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
                }
                currentColors[row][column] = Color.WHITE;
            }
        }
    }

    private java.util.List<Color> generateRandomPalette(int n) {
        Random rand = new Random();
        java.util.List<Color> list = new ArrayList<>();
        for (int i = 0; i < n; i++)
            list.add(new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));
        return list;
    }

    // Extract dominant colors from the image
    private java.util.List<Color> extractPalette(BufferedImage img, int maxColors) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                freq.put(rgb, freq.getOrDefault(rgb, 0) + 1);
            }
        }
        java.util.List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(freq.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        java.util.List<Color> colors = new ArrayList<>();
        for (int i = 0; i < Math.min(maxColors, sorted.size()); i++) {
            colors.add(new Color(sorted.get(i).getKey()));
        }
        return colors;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        Image tmp = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //Graphics2D g2d = (Graphics2D) g;

        //  Faint background 
        /*
        if (baseImage != null) {
            int imgWidth = numCols * cellSize;
            int imgHeight = numRows * cellSize;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g2d.drawImage(baseImage, 0, 0, imgWidth, imgHeight, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        */
        // Painted cells
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numCols; column++) {
                g.setColor(currentColors[row][column]);
                g.fillRect(column * cellSize, row * cellSize, cellSize, cellSize);
                g.setColor(Color.GRAY);
                g.drawRect(column * cellSize, row * cellSize, cellSize, cellSize);
            }
        }
    }

    private void paintCellAt(MouseEvent e) {
        int col = e.getX() / cellSize;
        int row = e.getY() / cellSize;
        if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
            currentColors[row][col] = selectedColor;
            repaint();
        }
    }

    @Override public void mousePressed(MouseEvent e) { paintCellAt(e); }
    @Override public void mouseDragged(MouseEvent e) { paintCellAt(e); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    public boolean isCompleted() {
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numCols; column++) {
                if (!currentColors[row][column].equals(targetColors[row][column])) return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Paint Test (Palette Limited)");
        PaintTest panel = new PaintTest();

        // Palette panel
        JPanel palettePanel = new JPanel();
        palettePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        for (Color color : panel.palette) {
            JButton colorButton = new JButton();
            colorButton.setBackground(color);
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            colorButton.addActionListener(e -> panel.selectedColor = color);
            palettePanel.add(colorButton);
        }

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            for (int row = 0; row < panel.numRows; row++)
                for (int column = 0; column < panel.numCols; column++)
                    panel.currentColors[row][column] = Color.WHITE;
            panel.repaint();
        });

        JButton checkButton = new JButton("Check Completion");
        checkButton.addActionListener(e -> {
            boolean done = panel.isCompleted();
            JOptionPane.showMessageDialog(frame, done ? "Complete!" : "Not yet complete!");
        });

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(palettePanel, BorderLayout.CENTER);

        JPanel rightButtons = new JPanel();
        rightButtons.add(resetButton);
        rightButtons.add(checkButton);
        controls.add(rightButtons, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.SOUTH);

        frame.setSize(650, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}