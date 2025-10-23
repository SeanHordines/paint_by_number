import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PaintTest extends JPanel implements MouseListener {

    private int[][] numberGrid;        // The "true" answer grid
    private int[][] userPaintGrid;     // What the user has painted
    private Color[] colorPalette;      // Color palette
    private boolean showNumbers = true;
    private int selectedColorNumber = 1;
    private boolean isComplete = false;

    private final int rows = 8;
    private final int cols = 8;

    public PaintTest() {
        // Hardcoded 8x8 pattern for testing
        numberGrid = new int[][] {
            {1,5,2,1,1,1,3,7},
            {1,2,7,4,7,3,1,6},
            {8,4,3,3,4,4,7,6},
            {3,8,7,4,8,8,8,6},
            {3,8,6,7,4,6,6,6},
            {3,3,8,5,3,3,8,8},
            {3,3,3,5,3,3,3,3},
            {3,3,3,3,3,3,3,3}
        };

        // Player’s blank canvas
        userPaintGrid = new int[rows][cols];

        // Distinct color palette (index 0 unused)
        colorPalette = new Color[] {
            Color.WHITE,             // 0 unused
            new Color(43, 63, 149),
            new Color(130, 140, 69), 
            new Color(44, 51, 59), 
            new Color(114, 136, 150),
            new Color(21, 30, 73),  
            new Color(167, 182, 161),
            new Color(89, 120, 174),  
            new Color(56, 80, 118) 
        };

        addMouseListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cellSize = Math.min(getWidth() / cols, (getHeight() - 80) / rows); // leave space for palette
        int xOffset = (getWidth() - (cellSize * cols)) / 2;
        int yOffset = (getHeight() - 80 - (cellSize * rows)) / 2;

        Font font = new Font("SansSerif", Font.BOLD, Math.max(14, cellSize / 3));
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        // Draw the painting grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;
                int correctNum = numberGrid[r][c];
                int paintedNum = userPaintGrid[r][c];

                // Determine what to fill
                if (paintedNum == 0) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(colorPalette[paintedNum]);
                }
                g2d.fillRect(x, y, cellSize, cellSize);

                // Border
                g2d.setColor(Color.GRAY);
                g2d.drawRect(x, y, cellSize, cellSize);

                // Always overlay painting number 
                if (showNumbers) {
                    String label = String.valueOf(correctNum);
                    int textWidth = fm.stringWidth(label);
                    int textHeight = fm.getAscent();
                    int tx = x + (cellSize - textWidth) / 2;
                    int ty = y + (cellSize + textHeight) / 2 - 4;
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(label, tx + 1, ty + 1);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(label, tx, ty);
                }
            }
        }

        // Place color palette at bottom
        int paletteY = getHeight() - 70;
        int paletteSize = 50;
        int paletteSpacing = 10;
        int totalWidth = (paletteSize + paletteSpacing) * 8;
        int startX = (getWidth() - totalWidth) / 2;

        for (int i = 1; i <= 8; i++) {
            int x = startX + (i - 1) * (paletteSize + paletteSpacing);
            g2d.setColor(colorPalette[i]);
            g2d.fillRect(x, paletteY, paletteSize, paletteSize);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(x, paletteY, paletteSize, paletteSize);

            // Highlight selected color
            if (i == selectedColorNumber) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(x - 2, paletteY - 2, paletteSize + 4, paletteSize + 4);
                g2d.setStroke(new BasicStroke(1));
            }
        }

        // Completion message
        if (isComplete) {
            g2d.setColor(new Color(0, 128, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String msg = "Painting Complete!";
            int msgWidth = g2d.getFontMetrics().stringWidth(msg);
            g2d.drawString(msg, (getWidth() - msgWidth) / 2, getHeight() / 2);
        }
    }

    // Click handling
    @Override
    public void mouseClicked(MouseEvent e) {
        int cellSize = Math.min(getWidth() / cols, (getHeight() - 80) / rows);
        int xOffset = (getWidth() - (cellSize * cols)) / 2;
        int yOffset = (getHeight() - 80 - (cellSize * rows)) / 2;

        // Detect clicks in grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;
                if (e.getX() >= x && e.getX() < x + cellSize && e.getY() >= y && e.getY() < y + cellSize) {
                    userPaintGrid[r][c] = selectedColorNumber;
                    checkForCompletion();
                    repaint();
                    return;
                }
            }
        }

        // Detect clicks in palette
        int paletteY = getHeight() - 70;
        int paletteSize = 50;
        int paletteSpacing = 10;
        int totalWidth = (paletteSize + paletteSpacing) * 8;
        int startX = (getWidth() - totalWidth) / 2;
        for (int i = 1; i <= 8; i++) {
            int x = startX + (i - 1) * (paletteSize + paletteSpacing);
            if (e.getX() >= x && e.getX() < x + paletteSize &&
                e.getY() >= paletteY && e.getY() < paletteY + paletteSize) {
                selectedColorNumber = i;
                repaint();
                return;
            }
        }
    }

    // Check if painting is complete
    private void checkForCompletion() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (userPaintGrid[r][c] != numberGrid[r][c]) {
                    isComplete = false;
                    return;
                }
            }
        }
        isComplete = true;
    }

    // Unused interface methods
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // Main test runner
    public static void main(String[] args) {
        JFrame frame = new JFrame("Paint by Number (Interactive)");
        PaintTest panel = new PaintTest();

        JButton toggleNumbers = new JButton("Toggle Numbers");
        toggleNumbers.addActionListener(e -> {
            panel.showNumbers = !panel.showNumbers;
            panel.repaint();
        });

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(toggleNumbers, BorderLayout.SOUTH);
        frame.setSize(700, 750);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
